package ru.jamsys.core.flat.util.validate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Wsdl {

    public static void validate(InputStream soap, InputStream wsdl, Function<String, InputStream> importSchemeResolver) throws Exception {
        try (
                InputStream inSoap = Objects.requireNonNull(soap, "SOAP data is null");
                InputStream inWsdl = Objects.requireNonNull(wsdl, "WSDL schema is null")
        ) {
            validate(
                    new String(inSoap.readAllBytes()),
                    new String(inWsdl.readAllBytes()),
                    importSchemeResolver
            );
        }
    }

    public static void validate(
            String soap,
            String wsdl,
            Function<String, InputStream> importResolver
    ) throws Exception {
        try {
            if (soap == null || soap.isEmpty()) {
                throw new IllegalArgumentException("SOAP is empty");
            }
            if (wsdl == null || wsdl.isEmpty()) {
                throw new IllegalArgumentException("WSDL is empty");
            }
            // 1. Собираем бизнес‑схемы из WSDL (<xsd:schema> внутри <wsdl:types>)
            final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;
            final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
            final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            // 1.1 Парсим WSDL
            Document wsdlDoc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(wsdl)));
            NodeList typesList = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "types");
            if (typesList.getLength() == 0) {
                throw new IllegalArgumentException("WSDL не содержит <wsdl:types>");
            }

            // 1.2 Собираем все <xsd:schema>
            List<Source> schemaSources = new ArrayList<>();
            for (int t = 0; t < typesList.getLength(); t++) {
                Element typesEl = (Element) typesList.item(t);
                NodeList schemas = typesEl.getElementsByTagNameNS(XSD_NS, "schema");
                for (int i = 0; i < schemas.getLength(); i++) {
                    schemaSources.add(new DOMSource(schemas.item(i)));
                }
            }
            if (schemaSources.isEmpty()) {
                throw new IllegalArgumentException("Во WSDL не найдено ни одной <xsd:schema>");
            }

            // 2. Настраиваем SchemaFactory с резолвом для <import> / <include>
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setResourceResolver((type, namespaceURI, publicId, systemId, baseURI) -> {
                InputStream in = Objects.requireNonNull(
                        importResolver.apply(systemId),
                        () -> "Не найден XSD для systemId=" + systemId
                );
                SimpleLSInput input = new SimpleLSInput(publicId, systemId, in);
                input.setBaseURI(baseURI);
                return input;
            });

            // 3. Компилируем единый Schema
            Schema schema = sf.newSchema(schemaSources.toArray(new Source[0]));

            // 4. Создаём Validator и собираем ошибки
            Validator validator = schema.newValidator();

            // 5. Парсим SOAP‑сообщение
            Document soapDoc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(soap)));

            // 6. Извлекаем payload из <soapenv:Body>
            NodeList bodies = soapDoc.getElementsByTagNameNS(SOAP_ENV_NS, "Body");
            if (bodies.getLength() == 0) {
                throw new IllegalArgumentException("В SOAP‑сообщении нет <soapenv:Body>");
            }
            Element bodyEl = (Element) bodies.item(0);

            Element payload = null;
            for (Node n = bodyEl.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    payload = (Element) n;
                    break;
                }
            }
            if (payload == null) {
                throw new IllegalArgumentException("В <soapenv:Body> нет элемента для валидации");
            }

            // 7. Валидируем только payload
            validator.validate(new DOMSource(payload));
        } catch (Exception e) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("soap", soap)
                    .append("wsdl", wsdl)
                    ,
                    e
            );
        }
    }

    // Вспомогательный: сериализация DOM-узла в строку
    private static String nodeToString(Node node) {
        try {
            javax.xml.transform.Transformer t =
                    javax.xml.transform.TransformerFactory.newInstance().newTransformer();
            java.io.StringWriter sw = new java.io.StringWriter();
            t.transform(new DOMSource(node), new javax.xml.transform.stream.StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            return "<error serializing node>";
        }
    }

}
