package com.meridian.contract.adapter.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ContractDocumentReader {

    public static final int MAX_CHARACTERS = 100_000;

    private static final int MAX_PAGES = 100;
    private static final int MAX_XML_BYTES = 10 * 1024 * 1024;
    private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String ODF_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";

    public ExtractedDocument read(String fileName, byte[] bytes) {
        if (bytes.length == 0) {
            throw new DocumentReadException("The file is empty.");
        }
        String name = fileName == null ? "document" : fileName;
        if (startsWith(bytes, "%PDF".getBytes(StandardCharsets.US_ASCII))) {
            return result(name, "PDF", pdf(bytes));
        }
        if (startsWith(bytes, new byte[]{'P', 'K', 3, 4})) {
            Map<String, byte[]> parts = zipParts(bytes);
            if (parts.containsKey("word/document.xml")) {
                return result(name, "Word", word(parts.get("word/document.xml"), parts.get("word/numbering.xml")));
            }
            if (parts.containsKey("content.xml")) {
                return result(name, "OpenDocument", openDocument(parts.get("content.xml")));
            }
            throw new DocumentReadException("This file type is not supported. Upload a PDF, Word (.docx), OpenDocument (.odt) or text file.");
        }
        if (startsWith(bytes, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0})) {
            throw new DocumentReadException("Old Word files (.doc) cannot be read. Save the contract as .docx or PDF and upload it again.");
        }
        return result(name, "Text", plainText(bytes));
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }

    private static ExtractedDocument result(String fileName, String format, String raw) {
        String text = raw.replace("\u00A0", " ")
                .replaceAll("(\\p{Ll})-[ \\t]*\\R[ \\t]*(\\p{Ll})", "$1$2")
                .replaceAll("[ \\t]+\\R", "\n")
                .replaceAll("\\R{3,}", "\n\n")
                .strip();
        if (text.isEmpty()) {
            throw new DocumentReadException(format.equals("PDF")
                    ? "No text found in this PDF. It looks like a scan or a photo; scanned contracts cannot be read yet."
                    : "No text found in this file.");
        }
        if (text.length() > MAX_CHARACTERS) {
            throw new DocumentReadException("The contract has %,d characters; the check reads at most %,d. Upload only the clauses that matter."
                    .formatted(text.length(), MAX_CHARACTERS));
        }
        return new ExtractedDocument(fileName, format, text, text.length());
    }

    private static String pdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new DocumentReadException("The PDF has more than %d pages.".formatted(MAX_PAGES));
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new DocumentReadException("The PDF could not be read. It may be damaged or protected by a password.", e);
        }
    }

    private static String plainText(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                throw new DocumentReadException("This file type is not supported. Upload a PDF, Word (.docx), OpenDocument (.odt) or text file.");
            }
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    private static Map<String, byte[]> zipParts(byte[] bytes) {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equals("word/document.xml") || entryName.equals("word/numbering.xml")
                        || entryName.equals("content.xml")) {
                    parts.put(entryName, readLimited(zip));
                }
            }
        } catch (IOException e) {
            throw new DocumentReadException("The file could not be opened. It may be damaged.", e);
        }
        return parts;
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_XML_BYTES) {
                throw new DocumentReadException("The document is too large to read.");
            }
        }
        return out.toByteArray();
    }

    private static Document xml(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new DocumentReadException("The document could not be read. It may be damaged.", e);
        }
    }

    private static String word(byte[] documentXml, byte[] numberingXml) {
        WordNumbering numbering = WordNumbering.from(numberingXml == null ? null : xml(numberingXml));
        NodeList paragraphs = xml(documentXml).getElementsByTagNameNS(WORD, "p");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = (Element) paragraphs.item(i);
            String content = wordParagraphText(paragraph);
            String number = numbering.next(paragraph);
            if (content.isBlank() && number.isEmpty()) {
                continue;
            }
            text.append(number.isEmpty() ? content : number + " " + content).append('\n');
        }
        return text.toString();
    }

    private static String wordParagraphText(Element paragraph) {
        StringBuilder text = new StringBuilder();
        appendWordText(paragraph, text);
        return text.toString().strip();
    }

    private static void appendWordText(Node node, StringBuilder text) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE || !WORD.equals(child.getNamespaceURI())) {
                continue;
            }
            switch (child.getLocalName()) {
                case "t" -> text.append(child.getTextContent());
                case "tab" -> text.append(' ');
                case "br", "cr" -> text.append(' ');
                case "p" -> {
                }
                default -> appendWordText(child, text);
            }
        }
    }

    private static String openDocument(byte[] contentXml) {
        Document document = xml(contentXml);
        StringBuilder text = new StringBuilder();
        collectOdfParagraphs(document.getDocumentElement(), text);
        return text.toString();
    }

    private static void collectOdfParagraphs(Node node, StringBuilder text) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (ODF_TEXT.equals(child.getNamespaceURI())
                    && (child.getLocalName().equals("p") || child.getLocalName().equals("h"))) {
                text.append(child.getTextContent().strip()).append('\n');
            } else {
                collectOdfParagraphs(child, text);
            }
        }
    }
}
