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

    /**
     * Читает InputStream-ы и делегирует валидацию по строкам.
     */
    public static void validate(InputStream soap, InputStream wsdl,
                                Function<String, InputStream> importResolver) throws Exception {
        try (
                InputStream inSoap = Objects.requireNonNull(soap, "SOAP data is null");
                InputStream inWsdl = Objects.requireNonNull(wsdl, "WSDL schema is null")
        ) {
            String soapXml = new String(inSoap.readAllBytes());
            String wsdlXml = new String(inWsdl.readAllBytes());
            validate(soapXml, wsdlXml, importResolver);
        }
    }

    /**
     * Основной метод валидации.
     * 1) Собирает все <xsd:schema> из <wsdl:types>
     * 2) Строит единый javax.xml.validation.Schema
     * 3) Парсит SOAP, определяет его namespace (SOAP 1.1 или SOAP 1.2)
     * 4) Валидирует каждый элемент внутри <Envelope>/<Body>
     * 5) Проверяет наличие <Fault> и mustUnderstand‑заголовков
     */
    public static void validate(String soap,
                                String wsdl,
                                Function<String, InputStream> importResolver) throws Exception {
        Objects.requireNonNull(soap, "soapXml is null");
        Objects.requireNonNull(wsdl, "wsdlXml is null");
        Objects.requireNonNull(importResolver, "importResolver is null");

        try {
            final String XSD_NS  = XMLConstants.W3C_XML_SCHEMA_NS_URI;
            final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";

            // Парсер DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            // 1) Загружаем WSDL и собираем все <xsd:schema>
            Document wsdlDoc = dbf.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(wsdl)));
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

            // 2) Строим SchemaFactory и настраиваем resolver для <xsd:import>
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setResourceResolver((type, namespaceURI, publicId, systemId, baseURI) -> {
                InputStream in = Objects.requireNonNull(
                        importResolver.apply(systemId),
                        () -> "resolver returned null for " + systemId
                );
                SimpleLSInput input = new SimpleLSInput(publicId, systemId, in);
                input.setBaseURI(baseURI);
                return input;
            });

            // Собираем общий Schema и инициализируем Validator
            Schema schema = sf.newSchema(schemaSources.toArray(new Source[0]));
            Validator validator = schema.newValidator();

            // 3) Парсим SOAP-документ
            Document soapDoc = dbf.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(soap)));

            // Определяем namespace на корневом элементе (<Envelope>)
            Element envelope = soapDoc.getDocumentElement();
            String soapEnvNs = envelope.getNamespaceURI();
            if (!"http://schemas.xmlsoap.org/soap/envelope/".equals(soapEnvNs) &&
                    !"http://www.w3.org/2003/05/soap-envelope".equals(soapEnvNs)) {
                throw new IllegalArgumentException(
                        "Неизвестное SOAP-пространство имён: " + soapEnvNs
                );
            }

            // 4) Валидируем содержимое Body
            NodeList bodyList = soapDoc.getElementsByTagNameNS(soapEnvNs, "Body");
            if (bodyList.getLength() == 0) {
                throw new IllegalArgumentException("В SOAP‑сообщении нет <Envelope>/<Body>");
            }
            Element bodyEl = (Element) bodyList.item(0);

            for (Node child = bodyEl.getFirstChild();  child != null;  child = child.getNextSibling()) {
                if (child instanceof Element) {
                    validator.validate(new DOMSource(child));
                }
            }

            // 5a) Проверка на SOAP Fault
            NodeList faults = bodyEl.getElementsByTagNameNS(soapEnvNs, "Fault");
            if (faults.getLength() > 0) {
                Node fault = faults.item(0);
                throw new ForwardException(
                        new HashMapBuilder<String, Object>()
                                .append("soapFault", nodeToString(fault)),
                        null
                );
            }

            // 5b) Проверка mustUnderstand‑заголовков
            NodeList headerList = soapDoc.getElementsByTagNameNS(soapEnvNs, "Header");
            if (headerList.getLength() > 0) {
                Element headerEl = (Element) headerList.item(0);
                for (Node n = headerEl.getFirstChild();  n != null;  n = n.getNextSibling()) {
                    if (n instanceof Element) {
                        Element he = (Element) n;
                        String mu = he.getAttributeNS(soapEnvNs, "mustUnderstand");
                        if ("1".equals(mu) || "true".equals(mu)) {
                            throw new ForwardException(
                                    new HashMapBuilder<String, Object>()
                                            .append("unhandledHeader", he.getLocalName())
                                            .append("mustUnderstand", mu),
                                    null
                            );
                        }
                    }
                }
            }

        } catch (Exception e) {
            // В случае любой ошибки — оборачиваем в ForwardException с исходными XML для отладки
            throw new ForwardException(
                    new HashMapBuilder<String, Object>()
                            .append("soap", soap)
                            .append("wsdl", wsdl),
                    e
            );
        }
    }

    /**
     * Сериализует DOM-узел в строку для логирования/исключений
     */
    private static String nodeToString(Node node) {
        if (node == null) return "";
        try {
            var tf = javax.xml.transform.TransformerFactory.newInstance();
            var t  = tf.newTransformer();
            var sw = new java.io.StringWriter();
            t.transform(new DOMSource(node), new javax.xml.transform.stream.StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            return "<error serializing node>";
        }
    }

}
