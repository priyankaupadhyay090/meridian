package com.meridian.library.adapter.web;

import com.meridian.contract.adapter.document.ContractDocumentReader;
import com.meridian.contract.adapter.document.ExtractedDocument;
import com.meridian.library.application.NewContractFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
class LibraryUploads {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "PDF", "application/pdf",
            "Word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "OpenDocument", "application/vnd.oasis.opendocument.text",
            "Text", "text/plain;charset=UTF-8");

    private final ContractDocumentReader reader;

    LibraryUploads(ContractDocumentReader reader) {
        this.reader = reader;
    }

    NewContractFile from(MultipartFile file, String carrierName) throws IOException {
        return from(file.getOriginalFilename(), file.getBytes(), carrierName);
    }

    NewContractFile from(String fileName, byte[] content, String carrierName) {
        ExtractedDocument document = reader.read(fileName, content);
        String carrier = carrierName == null || carrierName.isBlank() ? null : carrierName.strip();
        return new NewContractFile(fileName, document.format(), CONTENT_TYPES.get(document.format()), content,
                carrier == null || carrier.length() > 120 ? null : carrier);
    }

    String text(String fileName, byte[] content) {
        return reader.read(fileName, content).text();
    }
}
