package com.meridian.library.application;

import com.meridian.library.domain.ContractFile;

import java.util.List;
import java.util.Optional;

public interface ContractFileRepository {

    void save(ContractFile file);

    Optional<ContractFile> findById(String id);

    List<ContractFile> findAll();

    void delete(String id);
}
