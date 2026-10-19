package com.meridian.library.adapter.storage;

import com.meridian.library.application.ContractFileStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContractFileStore implements ContractFileStore {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] content, String contentType) {
        objects.put(key, content.clone());
    }

    @Override
    public byte[] get(String key) {
        byte[] content = objects.get(key);
        if (content == null) {
            throw new IllegalStateException("no stored object " + key);
        }
        return content.clone();
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }
}
