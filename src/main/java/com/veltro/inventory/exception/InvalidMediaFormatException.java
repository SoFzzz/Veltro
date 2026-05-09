package com.veltro.inventory.exception;

public class InvalidMediaFormatException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public InvalidMediaFormatException(String messageKey, Object... messageArgs) {
        super(messageKey);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}

