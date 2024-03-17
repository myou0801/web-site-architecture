package com.myou.backend.simulator.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public record XmlContent(String xml) implements RequestContent{

    private static final Logger logger = LoggerFactory.getLogger(XmlContent.class);

    private static final XmlMapper mapper = XmlMapper.builder().build();

    @Override
    public boolean matches(String key, String expectedValue) {
        try {
//            JsonNode rootNode = mapper.readTree(xml.getBytes());
//            JsonNode targetNode = rootNode.at(key);
//            return !targetNode.isMissingNode() && targetNode.asText().equals(expectedValue);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            String result = (String) xpath.evaluate(key, document, XPathConstants.STRING);
            return expectedValue.equals(result);
        } catch (Exception e) {
            logger.error("XMLデータの解析に失敗", e);
            return false;
        }
    }
}
