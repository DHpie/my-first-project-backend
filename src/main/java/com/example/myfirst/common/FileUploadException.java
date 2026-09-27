package com.example.myfirst.common;

import lombok.Getter;

@Getter
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }
}
