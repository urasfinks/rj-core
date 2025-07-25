package ru.jamsys.core.flat.util.validate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilFileResource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
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

    /**
     * Вариант для InputStream-ов.
     */
    public static void validate(InputStream soapStream,
                                InputStream wsdlStream,
                                Function<String, InputStream> importResolver) throws Exception {
        try (
                InputStream soapIn = Objects.requireNonNull(soapStream, "SOAP data is null");
                InputStream wsdlIn = Objects.requireNonNull(wsdlStream, "WSDL schema is null")
        ) {
            String soapXml = new String(soapIn.readAllBytes());
            String wsdlXml = new String(wsdlIn.readAllBytes());
            validate(soapXml, wsdlXml, importResolver);
        }
    }

    /**
     * Основной метод валидации.
     */
    public static void validate(String soapXml,
                                String wsdlXml,
                                Function<String, InputStream> importResolver) throws Exception {
        Objects.requireNonNull(soapXml, "soapXml is null");
        Objects.requireNonNull(wsdlXml, "wsdlXml is null");
        Objects.requireNonNull(importResolver, "importResolver is null");

        try {
            final String XSD_NS  = XMLConstants.W3C_XML_SCHEMA_NS_URI;
            final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";

            // 1) Настроить DOM‑парсер и защитить от XXE
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            // 2) Собираем все <xsd:schema> из <wsdl:types>
            Document wsdlDoc = dbf.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(wsdlXml)));
            NodeList typesList = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "types");
            if (typesList.getLength() == 0) {
                throw new IllegalArgumentException("WSDL не содержит <wsdl:types>");
            }

            List<Source> schemaSources = new ArrayList<>();
            for (int i = 0;  i < typesList.getLength();  i++) {
                Element typesEl = (Element) typesList.item(i);
                NodeList schemas = typesEl.getElementsByTagNameNS(XSD_NS, "schema");
                for (int j = 0;  j < schemas.getLength();  j++) {
                    schemaSources.add(new DOMSource(schemas.item(j)));
                }
            }
            if (schemaSources.isEmpty()) {
                throw new IllegalArgumentException("Во WSDL не найдено ни одной <xsd:schema>");
            }

            // 3) Подключаем официальные XSD для SOAP Envelope (1.1 и 1.2)
            InputStream soap11Xsd = UtilFileResource.get(
                    "schema/soap/soap_1.1.xsd",
                    UtilFileResource.Direction.RESOURCE_CORE
            );
            InputStream soap12Xsd = UtilFileResource.get(
                    "schema/soap/soap_1.1.xsd",
                    UtilFileResource.Direction.RESOURCE_CORE
            );
            if (soap11Xsd == null || soap12Xsd == null) {
                throw new IllegalStateException("Не найдены XSD для SOAP Envelope в /schemas");
            }
            schemaSources.add(new StreamSource(soap11Xsd));
            schemaSources.add(new StreamSource(soap12Xsd));

            // 4) Строим SchemaFactory и настраиваем resolver для <xsd:import>
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setResourceResolver((type, namespaceURI, publicId, systemId, baseURI) -> {
                InputStream in = Objects.requireNonNull(
                        importResolver.apply(systemId),
                        () -> "importResolver returned null for " + systemId
                );
                SimpleLSInput input = new SimpleLSInput(publicId, systemId, in);
                input.setBaseURI(baseURI);
                return input;
            });

            // 5) Собираем единый Schema и инициализируем Validator
            Schema schema = sf.newSchema(schemaSources.toArray(new Source[0]));
            Validator validator = schema.newValidator();

            // 6) Парсим SOAP-документ
            Document soapDoc = dbf.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(soapXml)));

            // 7) Проверяем корневой <Envelope>
            Element envelope = soapDoc.getDocumentElement();
            if (!"Envelope".equals(envelope.getLocalName())) {
                throw new IllegalArgumentException("Корневой элемент не Envelope: " + envelope.getLocalName());
            }
            String soapEnvNs = envelope.getNamespaceURI();
            if (!"http://schemas.xmlsoap.org/soap/envelope/".equals(soapEnvNs) &&
                    !"http://www.w3.org/2003/05/soap-envelope".equals(soapEnvNs)) {
                throw new IllegalArgumentException("Неизвестное SOAP‑пространство имён: " + soapEnvNs);
            }

            // 8) Извлекаем <Header> и <Body> без риска переопределения namespace
            Element headerEl = null;
            Element bodyEl = null;
            NodeList envChildren = envelope.getChildNodes();
            for (int i = 0; i < envChildren.getLength(); i++) {
                Node n = envChildren.item(i);
                if (!(n instanceof Element)) continue;
                Element el = (Element) n;
                switch (el.getLocalName()) {
                    case "Header":
                        headerEl = el;
                        break;
                    case "Body":
                        bodyEl = el;
                        break;
                }
            }
            if (bodyEl == null) {
                throw new IllegalArgumentException("В SOAP‑сообщении отсутствует элемент Body");
            }

            // 9) Валидируем и конверт, и payload:
            validator.validate(new DOMSource(envelope));

            // 10) Проверяем внутри Body: сначала XSD‑валидация payload (можно по‑отдельности)
            for (Node child = bodyEl.getFirstChild();  child != null;  child = child.getNextSibling()) {
                if (child instanceof Element) {
                    validator.validate(new DOMSource(child));
                }
            }

            // 11) Проверяем наличие Fault
            NodeList faults = bodyEl.getElementsByTagNameNS(soapEnvNs, "Fault");
            if (faults.getLength() > 0) {
                Node fault = faults.item(0);
                throw new ForwardException(
                        new HashMapBuilder<String, Object>()
                                .append("soapFault", nodeToString(fault)),
                        null
                );
            }

            // 12) mustUnderstand + actor/role
            if (headerEl != null) {
                NodeList headers = headerEl.getChildNodes();
                for (int i = 0; i < headers.getLength(); i++) {
                    Node n = headers.item(i);
                    if (!(n instanceof Element)) continue;
                    Element he = (Element) n;
                    String mu = he.getAttributeNS(soapEnvNs, "mustUnderstand");
                    if ("1".equals(mu) || "true".equals(mu)) {
                        // проверяем actor (1.1) или role (1.2)
                        String actor = he.getAttributeNS(soapEnvNs, "actor");
                        String role = he.getAttributeNS(soapEnvNs, "role");
                        throw new ForwardException(
                                new HashMapBuilder<String, Object>()
                                        .append("unhandledHeader", he.getLocalName())
                                        .append("mustUnderstand", mu)
                                        .append("actor", actor.isEmpty() ? role : actor),
                                null
                        );
                    }
                }
            }

        } catch (Exception e) {
            // Оборачиваем в ForwardException для логирования исходных XML
            throw new ForwardException(
                    new HashMapBuilder<String, Object>()
                            .append("soap", soapXml)
                            .append("wsdl", wsdlXml),
                    e
            );
        }
    }

    /**
     * Сериализует DOM-узел в строку для логов/исключений.
     */
    private static String nodeToString(Node node) {
        if (node == null) return "";
        try {
            var tf = javax.xml.transform.TransformerFactory.newInstance();
            var t  = tf.newTransformer();
            var sw = new java.io.StringWriter();
            t.transform(new DOMSource(node), new javax.xml.transform.stream.StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            return "<error serializing node>";
        }
    }

}
