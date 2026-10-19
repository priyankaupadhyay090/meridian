package com.meridian.library.adapter.persistence;

import com.meridian.library.application.ContractFileRepository;
import com.meridian.library.domain.ContractFile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryContractFileRepository implements ContractFileRepository {

    private final Map<String, ContractFile> files = new ConcurrentHashMap<>();

    @Override
    public void save(ContractFile file) {
        files.put(file.id(), file);
    }

    @Override
    public Optional<ContractFile> findById(String id) {
        return Optional.ofNullable(files.get(id));
    }


    @Override
    public List<ContractFile> findAll() {
        return List.copyOf(files.values());
    }

    @Override
    public void delete(String id) {
        files.remove(id);
    }
}
