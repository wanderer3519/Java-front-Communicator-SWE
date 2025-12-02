/*
 * -----------------------------------------------------------------------------
 * File: SerializationException.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.serialization;

/**
 * Custom runtime exception for serialization or deserialization failures.
 *
 * <p>This exception is thrown by serializers or deserializers
 * when an underlying error occurs (e.g., IOException, ClassNotFoundException).</p>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe, like all Exceptions.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class SerializationException extends RuntimeException {

    /**
     * Used for Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SerializationException with a message.
     *
     * @param message The detail message.
     */
    public SerializationException(final String message) {
        super(message);
    }

    /**
     * Constructs a new SerializationException with a message and cause.
     *
     * @param message The detail message.
     * @param cause   The underlying cause of the failure.
     */
    public SerializationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}