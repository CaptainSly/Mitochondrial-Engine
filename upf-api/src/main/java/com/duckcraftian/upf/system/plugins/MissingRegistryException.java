package com.duckcraftian.upf.system.plugins;

public class MissingRegistryException extends RuntimeException {
    public MissingRegistryException() {
        super();
    }

    public MissingRegistryException(String message) {
        super(message);
    }

    public MissingRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingRegistryException(Throwable cause) {
        super(cause);
    }

    protected MissingRegistryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}