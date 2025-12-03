/*
 * Custom serializer for shape requests to handle proper JSON encoding/decoding.
 */

package com.swe.canvas.datamodel.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for serializing and deserializing shape request payloads.
 * Provides consistent JSON encoding/decoding for CLIENT_NODE and other shape
 * request data.
 */
public class ShapeRequestSerializer {

    /** The Jackson ObjectMapper for JSON serialization. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Serializes an object to a JSON string.
     *
     * @param obj the object to serialize
     * @return the serialized JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public static String serializeToString(final Object obj)
            throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * Serializes an object to a byte array (UTF-8 encoded JSON).
     *
     * @param obj the object to serialize
     * @return the serialized byte array
     * @throws JsonProcessingException if serialization fails
     */
    public static byte[] serializeToBytes(final Object obj)
            throws JsonProcessingException {
        final String json = serializeToString(obj);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Deserializes a JSON string to an object.
     *
     * @param json     the JSON string to deserialize
     * @param datatype the target class type
     * @param <T>      the type parameter
     * @return the deserialized object
     * @throws JsonProcessingException if deserialization fails
     */
    public static <T> T deserializeFromString(final String json, final Class<T> datatype)
            throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, datatype);
    }

    /**
     * Deserializes a byte array to an object.
     *
     * @param data     the byte array to deserialize
     * @param datatype the target class type
     * @param <T>      the type parameter
     * @return the deserialized object
     * @throws JsonProcessingException if deserialization fails
     */
    public static <T> T deserializeFromBytes(final byte[] data, final Class<T> datatype)
            throws JsonProcessingException {
        final String json = new String(data, StandardCharsets.UTF_8);
        return deserializeFromString(json, datatype);
    }

    /**
     * Attempts to un-escape a JSON string literal (e.g.,
     * "{\\"key\\":\\"value\\"}").
     * If the string is not a JSON literal or un-escaping fails, returns the
     * original string.
     *
     * @param potentialJsonLiteral the string that may be a JSON literal
     * @return the un-escaped JSON string, or the original if not a literal
     */
    public static String unescapeJsonString(final String potentialJsonLiteral) {
        if (potentialJsonLiteral == null || potentialJsonLiteral.isEmpty()) {
            return potentialJsonLiteral;
        }

        // Check if it looks like a JSON string literal
        if (potentialJsonLiteral.startsWith("\"") && potentialJsonLiteral.endsWith("\"")) {
            try {
                return OBJECT_MAPPER.readValue(potentialJsonLiteral, String.class);
            } catch (final JsonProcessingException e) {
                // If un-escaping fails, return as-is
                return potentialJsonLiteral;
            }
        }

        return potentialJsonLiteral;
    }
}
