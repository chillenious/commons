package com.chillenious.common.db.sync;

/**
 * Thrown by {@link DataRefreshListenerMarker#blockUntilNextMarker(com.chillenious.common.util.Duration)}
 * when the blocking times out.
 */
public final class OverDueException extends RuntimeException {

    public OverDueException() {
    }

    public OverDueException(String message) {
        super(message);
    }

    public OverDueException(String message, Throwable cause) {
        super(message, cause);
    }

    public OverDueException(Throwable cause) {
        super(cause);
    }
}
