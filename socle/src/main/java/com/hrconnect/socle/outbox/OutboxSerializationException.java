package com.hrconnect.socle.outbox;

/**
 * Exception levée lors d'une erreur de sérialisation dans l'Outbox.
 */
public class OutboxSerializationException extends RuntimeException {

    public OutboxSerializationException(String message) {
        super(message);
    }

    public OutboxSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
