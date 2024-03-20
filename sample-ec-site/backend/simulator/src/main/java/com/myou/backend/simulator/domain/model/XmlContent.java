package com.myou.backend.simulator.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public record XmlContent(String xml, Document document) implements RequestContent {

    private static final Logger logger = LoggerFactory.getLogger(XmlContent.class);

    public XmlContent(String xml) {
        this(xml, parseXml(xml));
    }

    private static Document parseXml(String xml) {
        Assert.hasText(xml, () -> "xml content is null or blank");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided string is not valid XML.", e);
        }
    }

    @Override
    public String toString() {
        return "XmlContent{" +
                "xml='" + xml + '\'' +
                '}';
    }

    @Override
    public boolean matches(String key, String expectedValue) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String result = (String) xpath.evaluate(key, document, XPathConstants.STRING);
            return expectedValue.equals(result);
        } catch (Exception e) {
            logger.error("XMLデータの解析に失敗", e);
            return false;
        }
    }
}
