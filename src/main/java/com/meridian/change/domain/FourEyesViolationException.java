package com.meridian.change.domain;

public class FourEyesViolationException extends RuntimeException {

    public FourEyesViolationException(String changeId, String user) {
        super("%s authored change %s and cannot approve it".formatted(user, changeId));
    }
}
