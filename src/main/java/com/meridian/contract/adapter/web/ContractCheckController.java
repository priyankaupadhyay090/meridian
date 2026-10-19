package com.meridian.contract.adapter.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.contract.application.ContractImportService;
import com.meridian.contract.domain.ContractCheck;
import com.meridian.contract.domain.ContractExample;
import com.meridian.common.SeedResources;
import com.meridian.contract.adapter.document.ContractDocumentReader;
import com.meridian.contract.adapter.document.ExtractedDocument;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/api/public/contract-checks")
public class ContractCheckController {

    private final ContractImportService contracts;
    private final ContractDocumentReader documents;
    private final List<ContractExample> examples;

    public ContractCheckController(ContractImportService contracts, ContractDocumentReader documents,
                                   ObjectMapper mapper) {
        this.contracts = contracts;
        this.documents = documents;
        this.examples = List.copyOf(SeedResources.read(mapper, "seed/contract-examples.json",
                new TypeReference<List<ContractExample>>() {
                }));
    }

    @PostMapping
    ContractCheck check(@Valid @RequestBody ContractCheckRequest request) {
        String carrier = request.carrierName() == null || request.carrierName().isBlank() ? "unknown carrier"
                : request.carrierName();
        return contracts.check(carrier, request.text(), request.senderType(), request.termsBasis());
    }

    @PostMapping(path = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ExtractedDocument readDocument(@RequestParam("file") MultipartFile file) throws IOException {
        return documents.read(file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping("/examples")
    List<ContractExample> examples() {
        return examples;
    }
}
