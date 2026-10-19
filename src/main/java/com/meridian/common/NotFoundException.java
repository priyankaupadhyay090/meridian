package com.meridian.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String kind, String id) {
        super("%s %s was not found".formatted(kind, id));
    }
}
