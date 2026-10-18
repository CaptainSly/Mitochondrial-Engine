package com.duckcraftian.upf.system.plugins;

public class MissingDependencyException extends RuntimeException{

    public MissingDependencyException() {
        super();
    }

    public MissingDependencyException(String message) {
        super(message);
    }

    public MissingDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingDependencyException(Throwable cause) {
        super(cause);
    }

    protected MissingDependencyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}