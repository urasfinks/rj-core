package ru.jamsys.core.flat.util.validate;

import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class SimpleLSInput implements LSInput {
    private String publicId;
    private String systemId;
    private String baseURI;
    private Reader characterStream;
    private InputStream byteStream;

    SimpleLSInput(String publicId, String systemId, InputStream byteStream) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.byteStream = byteStream;
    }

    @Override
    public Reader getCharacterStream() {
        return characterStream;
    }

    @Override
    public void setCharacterStream(Reader reader) {
        this.characterStream = reader;
    }

    @Override
    public InputStream getByteStream() {
        return byteStream;
    }

    @Override
    public void setByteStream(InputStream stream) {
        this.byteStream = stream;
    }

    @Override
    public String getStringData() {
        return null;
    }

    @Override
    public void setStringData(String stringData) {
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    @Override
    public String getBaseURI() {
        return baseURI;
    }

    @Override
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    @Override
    public String getEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public void setEncoding(String encoding) {
    }

    @Override
    public boolean getCertifiedText() {
        return false;
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {
    }

}
