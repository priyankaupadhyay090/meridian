package com.meridian.library.application;

public record NewContractFile(String fileName, String format, String contentType, byte[] content,
                              String carrierName) {
}
