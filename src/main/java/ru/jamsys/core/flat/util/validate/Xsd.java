package ru.jamsys.core.flat.util.validate;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import javax.xml.XMLConstants;
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

public class Xsd {

    public static void validate(InputStream xml, InputStream xsd, Function<String, InputStream> importSchemeResolver) throws Exception {
        try (
                InputStream inXml = Objects.requireNonNull(xml, "xml data is null");
                InputStream inXsd = Objects.requireNonNull(xsd, "xsd schema is null")
        ) {
            validate(
                    new String(inXml.readAllBytes()),
                    new String(inXsd.readAllBytes()),
                    importSchemeResolver
            );
        }
    }

    public static void validate(String xml, String xsd, Function<String, InputStream> importSchemeResolver) {
        if (xml == null || xml.isEmpty()) {
            throw new IllegalArgumentException("XML is empty");
        }
        if (xsd == null || xsd.isEmpty()) {
            throw new IllegalArgumentException("XSD is empty");
        }
        List<SAXParseException> errors = new ArrayList<>();
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setResourceResolver((_, _, publicId, systemId, baseURI) -> {
                // 1) забираем InputStream (не try-with-resources!)
                InputStream in = Objects.requireNonNull(
                        importSchemeResolver.apply(systemId),
                        () -> "resolver returned null for " + systemId
                );
                SimpleLSInput input = new SimpleLSInput(publicId, systemId, in);
                input.setBaseURI(baseURI);
                return input;
            });
            Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsd)));
            if (schema == null) {
                throw new IllegalArgumentException("XSD Schema is null");
            }
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));

            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    errors.add(exception);
                }

                @Override
                public void error(SAXParseException exception) {
                    errors.add(exception);
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    errors.add(exception);
                }

            });
        } catch (Exception e) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("xml", xml)
                    .append("xsd", xsd)
                    .append("errors", errors),
                    e
            );
        }
    }

}
