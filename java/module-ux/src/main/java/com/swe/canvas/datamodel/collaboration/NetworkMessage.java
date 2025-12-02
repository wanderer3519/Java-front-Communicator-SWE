/*
 * -----------------------------------------------------------------------------
 * File: NetworkMessage.java
 * Owner: B S S Krishna
 * Roll Number: 112201013
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.collaboration;

import com.swe.canvas.datamodel.serialization.JsonUtils;
import java.util.Base64;

/**
 * A wrapper for data sent over the network.
 *
 * <p>This class encapsulates the type of message (e.g., NORMAL, UNDO),
 * the serialized binary data of an action, and an optional string payload
 * (used primarily for RESTORE operations).</p>
 */
public class NetworkMessage {

    /** The type of the message. */
    private final MessageType messageType;

    /** The serialized action data (can be null if payload is used). */
    private final byte[] serializedAction;

    /** Optional payload for operations like RESTORE (JSON String). */
    private final String payload;

    /**
     * Constructor for standard actions without a string payload.
     *
     * @param type   The type of the message.
     * @param action The serialized action bytes.
     */
    public NetworkMessage(final MessageType type, final byte[] action) {
        this(type, action, null);
    }

    /**
     * Constructor for messages with an optional string payload (e.g., RESTORE).
     *
     * @param type    The type of the message.
     * @param action  The serialized action bytes (can be null).
     * @param content The string payload (can be null).
     */
    public NetworkMessage(final MessageType type, final byte[] action, final String content) {
        this.messageType = type;
        if (action != null) {
            this.serializedAction = action.clone();
        } else {
            this.serializedAction = null;
        }
        this.payload = content;
    }

    /**
     * Gets the message type.
     *
     * @return The MessageType enum.
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Gets the serialized action data.
     *
     * @return A copy of the byte array, or null.
     */
    public byte[] getSerializedAction() {
        if (serializedAction != null) {
            return serializedAction.clone();
        }
        return null;
    }

    /**
     * Gets the string payload.
     *
     * @return The payload string, or null.
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Serializes this NetworkMessage into a JSON string.
     *
     * @return A JSON representation of this message.
     */
    public String serialize() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(JsonUtils.jsonEscape("type")).append(":")
            .append(JsonUtils.jsonEscape(messageType.toString()));

        // Encode byte array as Base64 string if present
        if (serializedAction != null) {
            final String actionBase64 = Base64.getEncoder().encodeToString(serializedAction);
            sb.append(",").append(JsonUtils.jsonEscape("action")).append(":")
                .append(JsonUtils.jsonEscape(actionBase64));
        }

        // Append Payload string if present
        if (payload != null) {
            sb.append(",").append(JsonUtils.jsonEscape("payload")).append(":")
                .append(JsonUtils.jsonEscape(payload));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Deserializes a JSON string back into a NetworkMessage.
     *
     * @param json The JSON string to deserialize.
     * @return The NetworkMessage object, or null if deserialization fails.
     */
    public static NetworkMessage deserialize(final String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            // 1. Extract Type
            final String typeStr = JsonUtils.extractString(json, "type");
            if (typeStr == null) {
                return null;
            }
            final MessageType type = MessageType.valueOf(typeStr);

            // 2. Extract Action (Base64)
            final String actionBase64 = JsonUtils.extractString(json, "action");
            final byte[] actionBytes;
            if (actionBase64 != null) {
                actionBytes = Base64.getDecoder().decode(actionBase64);
            } else {
                actionBytes = null;
            }

            // 3. Extract Payload
            final String payloadStr = JsonUtils.extractString(json, "payload");

            return new NetworkMessage(type, actionBytes, payloadStr);

        } catch (final Exception e) {
            System.err.println("NetworkMessage deserialization failed: " + e.getMessage());
            return null;
        }
    }
}