package com.parkyc.poelens.analysis.parser;

import com.parkyc.poelens.analysis.exception.AnalysisException;
import com.parkyc.poelens.analysis.model.PobBuild;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.InflaterInputStream;
import java.io.ByteArrayInputStream;

@Component
public class PobParser {

    public PobBuild parse(String input) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml(input))));
            Element build = firstElement(document, "Build");
            if (build == null) {
                throw invalid();
            }
            return new PobBuild(
                    optionalAttribute(build, "gameVersion", "targetVersion"),
                    parseLevel(build.getAttribute("level")),
                    defaultValue(build.getAttribute("className"), "Unknown class"),
                    gemNames(document),
                    itemNames(document));
        } catch (AnalysisException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private Element firstElement(Document document, String tagName) {
        NodeList elements = document.getElementsByTagName(tagName);
        return elements.getLength() == 0 ? null : (Element) elements.item(0);
    }

    private String xml(String input) throws Exception {
        String trimmed = input.trim();
        if (trimmed.startsWith("<")) {
            return trimmed;
        }
        byte[] compressed;
        try {
            compressed = Base64.getUrlDecoder().decode(trimmed);
        } catch (IllegalArgumentException exception) {
            compressed = Base64.getDecoder().decode(trimmed);
        }
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            return new String(inflater.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> gemNames(Document document) {
        List<String> names = new ArrayList<>();
        NodeList gems = document.getElementsByTagName("Gem");
        for (int index = 0; index < gems.getLength(); index++) {
            Element gem = (Element) gems.item(index);
            String name = gem.getAttribute("nameSpec");
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private List<String> itemNames(Document document) {
        List<String> names = new ArrayList<>();
        NodeList items = document.getElementsByTagName("Item");
        for (int index = 0; index < items.getLength(); index++) {
            String[] lines = items.item(index).getTextContent().replace("\\n", "\n").split("\\R");
            for (int line = 1; line < lines.length; line++) {
                if (!lines[line].isBlank()) {
                    names.add(lines[line].trim());
                    break;
                }
            }
        }
        return names;
    }

    private String optionalAttribute(Element element, String... names) {
        for (String name : names) {
            if (!element.getAttribute(name).isBlank()) {
                return element.getAttribute(name);
            }
        }
        return null;
    }

    private int parseLevel(String level) {
        try {
            return Integer.parseInt(level);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String defaultValue(String value, String fallback) {
        return value.isBlank() ? fallback : value;
    }

    private AnalysisException invalid() {
        return new AnalysisException("INVALID_POB_INPUT", "Provide a raw Path of Building XML export.");
    }
}
