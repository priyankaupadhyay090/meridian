package com.meridian.library.adapter.web;

import com.meridian.library.application.ContractLibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/console/contract-files")
public class StaffContractController {

    private final ContractLibraryService library;

    public StaffContractController(ContractLibraryService library) {
        this.library = library;
    }

    @GetMapping
    List<ContractFileView> all(Principal user) {
        return library.all(user.getName()).stream().map(ContractFileView::of).toList();
    }

    @GetMapping("/{id}/file")
    ResponseEntity<byte[]> download(@PathVariable String id, Principal user) {
        return FileResponses.attachment(library.anyFile(id), library.contentForStaff(id, user.getName()));
    }
}
