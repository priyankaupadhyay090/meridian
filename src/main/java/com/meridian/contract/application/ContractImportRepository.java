package com.meridian.contract.application;

import com.meridian.contract.domain.ContractImport;

import java.util.List;
import java.util.Optional;

public interface ContractImportRepository {

    void save(ContractImport contractImport);

    Optional<ContractImport> findById(String id);

    List<ContractImport> findAll();
}
