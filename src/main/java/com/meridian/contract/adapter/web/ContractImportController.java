package com.meridian.contract.adapter.web;

import com.meridian.contract.application.ContractImportService;
import com.meridian.contract.domain.ContractImport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/console")
public class ContractImportController {

    private final ContractImportService imports;

    public ContractImportController(ContractImportService imports) {
        this.imports = imports;
    }

    @PostMapping("/contracts/imports")
    @ResponseStatus(HttpStatus.CREATED)
    ContractImport importContract(@Valid @RequestBody ContractImportRequest request, Principal user) {
        return imports.importContract(user.getName(), request.carrierName(), request.text(),
                request.senderTypeOrDefault(), request.termsBasisOrDefault());
    }

    @GetMapping("/contracts/imports")
    List<ContractImport> all() {
        return imports.all();
    }

    @GetMapping("/contracts/imports/{id}")
    ContractImport one(@PathVariable String id) {
        return imports.find(id);
    }
}
