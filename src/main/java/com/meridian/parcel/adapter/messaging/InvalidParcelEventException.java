package com.meridian.parcel.adapter.messaging;

public class InvalidParcelEventException extends RuntimeException {

    public InvalidParcelEventException(String message) {
        super(message);
    }

    public InvalidParcelEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
