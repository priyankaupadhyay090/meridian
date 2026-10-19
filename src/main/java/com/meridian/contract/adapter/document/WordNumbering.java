package com.meridian.contract.adapter.document;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

class WordNumbering {

    private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final int LEVELS = 9;

    private final Map<String, String> abstractByNum;
    private final Map<String, String> formatByAbstractLevel;
    private final Map<String, int[]> counters = new HashMap<>();

    private WordNumbering(Map<String, String> abstractByNum, Map<String, String> formatByAbstractLevel) {
        this.abstractByNum = abstractByNum;
        this.formatByAbstractLevel = formatByAbstractLevel;
    }

    static WordNumbering from(Document numbering) {
        Map<String, String> abstractByNum = new HashMap<>();
        Map<String, String> formats = new HashMap<>();
        if (numbering != null) {
            NodeList nums = numbering.getElementsByTagNameNS(WORD, "num");
            for (int i = 0; i < nums.getLength(); i++) {
                Element num = (Element) nums.item(i);
                Element abstractId = child(num, "abstractNumId");
                if (abstractId != null) {
                    abstractByNum.put(value(num, "numId"), value(abstractId, "val"));
                }
            }
            NodeList abstracts = numbering.getElementsByTagNameNS(WORD, "abstractNum");
            for (int i = 0; i < abstracts.getLength(); i++) {
                Element abstractNum = (Element) abstracts.item(i);
                NodeList levels = abstractNum.getElementsByTagNameNS(WORD, "lvl");
                for (int j = 0; j < levels.getLength(); j++) {
                    Element level = (Element) levels.item(j);
                    Element format = child(level, "numFmt");
                    formats.put(value(abstractNum, "abstractNumId") + "/" + value(level, "ilvl"),
                            format == null ? "decimal" : value(format, "val"));
                }
            }
        }
        return new WordNumbering(abstractByNum, formats);
    }

    String next(Element paragraph) {
        Element properties = child(paragraph, "pPr");
        Element numbered = properties == null ? null : child(properties, "numPr");
        if (numbered == null) {
            return "";
        }
        Element numIdElement = child(numbered, "numId");
        Element levelElement = child(numbered, "ilvl");
        String numId = numIdElement == null ? null : value(numIdElement, "val");
        if (numId == null || numId.equals("0")) {
            return "";
        }
        int level = levelElement == null ? 0 : Math.min(LEVELS - 1, Integer.parseInt(value(levelElement, "val")));
        String format = formatByAbstractLevel.getOrDefault(abstractByNum.get(numId) + "/" + level, "decimal");
        if (!format.equals("decimal")) {
            return "";
        }
        int[] count = counters.computeIfAbsent(abstractByNum.getOrDefault(numId, numId), key -> new int[LEVELS]);
        count[level]++;
        for (int deeper = level + 1; deeper < LEVELS; deeper++) {
            count[deeper] = 0;
        }
        StringJoiner number = new StringJoiner(".");
        for (int i = 0; i <= level; i++) {
            number.add(String.valueOf(Math.max(1, count[i])));
        }
        return level == 0 ? number + "." : number.toString();
    }

    private static Element child(Element parent, String localName) {
        NodeList children = parent.getElementsByTagNameNS(WORD, localName);
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getParentNode() == parent) {
                return (Element) children.item(i);
            }
        }
        return null;
    }

    private static String value(Element element, String attribute) {
        return element.getAttributeNS(WORD, attribute);
    }
}
