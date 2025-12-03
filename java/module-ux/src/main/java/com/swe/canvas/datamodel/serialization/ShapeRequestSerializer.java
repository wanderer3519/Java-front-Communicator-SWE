/*
 * Custom serializer for shape requests to handle proper JSON encoding/decoding.
 */

package com.swe.canvas.datamodel.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe.controller.ClientNode;
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
        // Special handling for ClientNode to use custom format
        if (obj instanceof ClientNode) {
            return serializeClientNode((ClientNode) obj);
        }
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * Serializes a ClientNode to JSON with specific field names: "ipaddr" and
     * "port".
     *
     * @param clientNode the ClientNode to serialize
     * @return the serialized JSON string
     */
    private static String serializeClientNode(final ClientNode clientNode) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"ipaddr\" : \"").append(clientNode.hostName()).append("\",\n");
        sb.append("    \"port\" : ").append(clientNode.port()).append("\n");
        sb.append("}");
        return sb.toString();
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
        // Special handling for ClientNode to parse custom format
        if (datatype == ClientNode.class) {
            return (T) deserializeClientNode(json);
        }
        return OBJECT_MAPPER.readValue(json, datatype);
    }

    /**
     * Deserializes a JSON string with "ipaddr" and "port" fields to a ClientNode.
     *
     * @param json the JSON string with "ipaddr" and "port" fields
     * @return the deserialized ClientNode
     * @throws JsonProcessingException if deserialization fails
     */
    private static ClientNode deserializeClientNode(final String json)
            throws JsonProcessingException {
        // Use ObjectMapper to parse the JSON, then map "ipaddr" to "hostName"
        final java.util.Map<String, Object> map = OBJECT_MAPPER.readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                });
        final String hostName = (String) map.get("ipaddr");
        final int port = ((Number) map.get("port")).intValue();
        return new ClientNode(hostName, port);
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
