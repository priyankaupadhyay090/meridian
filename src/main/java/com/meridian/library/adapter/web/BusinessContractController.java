package com.meridian.library.adapter.web;

import com.meridian.library.application.ContractLibraryService;
import com.meridian.library.domain.Business;
import com.meridian.library.domain.ContractFile;
import com.meridian.security.BusinessAccounts;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessContractController {

    private final ContractLibraryService library;
    private final LibraryUploads uploads;
    private final BusinessAccounts accounts;

    public BusinessContractController(ContractLibraryService library, LibraryUploads uploads,
                                      BusinessAccounts accounts) {
        this.library = library;
        this.uploads = uploads;
        this.accounts = accounts;
    }

    @GetMapping("/me")
    Map<String, String> me(Principal user) {
        Business business = accounts.businessOf(user.getName());
        return Map.of("user", user.getName(), "businessId", business.id(), "businessName", business.name());
    }

    @GetMapping("/contracts")
    List<ContractFileView> contracts(Principal user) {
        return library.ofBusiness(accounts.businessOf(user.getName()).id()).stream().map(ContractFileView::of).toList();
    }

    @PostMapping(path = "/contracts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ContractFileView upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "carrierName", required = false) String carrierName,
                            Principal user) throws IOException {
        return ContractFileView.of(library.upload(accounts.businessOf(user.getName()), user.getName(),
                uploads.from(file, carrierName)));
    }

    @GetMapping("/contracts/{id}/file")
    ResponseEntity<byte[]> download(@PathVariable String id, Principal user) {
        String business = accounts.businessOf(user.getName()).id();
        return FileResponses.attachment(library.ownFile(business, id), library.ownContent(business, id));
    }

    @GetMapping("/contracts/{id}/text")
    Map<String, String> text(@PathVariable String id, Principal user) {
        String business = accounts.businessOf(user.getName()).id();
        ContractFile file = library.ownFile(business, id);
        return Map.of("fileName", file.fileName(), "carrierName", file.carrierName() == null ? "" : file.carrierName(),
                "text", uploads.text(file.fileName(), library.ownContent(business, id)));
    }

    @DeleteMapping("/contracts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id, Principal user) {
        library.delete(accounts.businessOf(user.getName()).id(), id, user.getName());
    }
}
