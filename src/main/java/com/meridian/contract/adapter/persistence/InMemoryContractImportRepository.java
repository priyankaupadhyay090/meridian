package com.meridian.contract.adapter.persistence;

import com.meridian.contract.application.ContractImportRepository;
import com.meridian.contract.domain.ContractImport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryContractImportRepository implements ContractImportRepository {

    private final Map<String, ContractImport> imports = new ConcurrentHashMap<>();

    @Override
    public void save(ContractImport contractImport) {
        imports.put(contractImport.id(), contractImport);
    }

    @Override
    public Optional<ContractImport> findById(String id) {
        return Optional.ofNullable(imports.get(id));
    }

    @Override
    public List<ContractImport> findAll() {
        return List.copyOf(imports.values());
    }
}
