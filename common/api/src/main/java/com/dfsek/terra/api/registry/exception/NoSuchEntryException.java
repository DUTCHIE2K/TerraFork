package com.dfsek.terra.api.registry.exception;

import java.io.Serial;


public class NoSuchEntryException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -9132485164486756407L;

    public NoSuchEntryException(String message) {
        super(message);
    }

}
