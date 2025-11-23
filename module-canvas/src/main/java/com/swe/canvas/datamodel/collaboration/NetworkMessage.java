package com.swe.canvas.datamodel.collaboration;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.swe.canvas.datamodel.serialization.JsonUtils; // Helper reuse

/**
 * A wrapper for data sent over the network.
 */
public class NetworkMessage {

    private final MessageType messageType;
    private final byte[] serializedAction;
    // NEW: Optional payload for RESTORE (JSON String)
    private final String payload;

    // Constructor for standard actions
    public NetworkMessage(MessageType messageType, byte[] serializedAction) {
        this(messageType, serializedAction, null);
    }

    // Constructor for RESTORE or custom payloads
    public NetworkMessage(MessageType messageType, byte[] serializedAction, String payload) {
        this.messageType = messageType;
        this.serializedAction = serializedAction;
        this.payload = payload;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public byte[] getSerializedAction() {
        return serializedAction;
    }

    public String getPayload() {
        return payload;
    }

    /**
     * Serializes this NetworkMessage into a JSON string.
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(messageType.toString()).append("\"");

        // Encode byte array as Base64 string if present
        if (serializedAction != null) {
            String actionBase64 = Base64.getEncoder().encodeToString(serializedAction);
            sb.append(",\"action\":\"").append(actionBase64).append("\"");
        }

        // Append Payload string if present (JSON Escaped)
        if (payload != null) {
            // Reuse JsonUtils.jsonEscape to safely wrap the payload string
            sb.append(",\"payload\":").append(JsonUtils.jsonEscape(payload));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Deserializes a JSON string back into a NetworkMessage.
     */
    public static NetworkMessage deserialize(String json) {
        if (json == null || json.isEmpty()) return null;

        try {
            // We can't use simple regex for payload if the payload itself contains JSON with quotes.
            // We use JsonUtils for robust extraction.

            // 1. Extract Type
            String typeStr = JsonUtils.extractString(json, "type");
            MessageType type = MessageType.valueOf(typeStr);

            // 2. Extract Action (Base64)
            String actionBase64 = JsonUtils.extractString(json, "action");
            byte[] actionBytes = null;
            if (actionBase64 != null) {
                actionBytes = Base64.getDecoder().decode(actionBase64);
            }

            // 3. Extract Payload
            // Note: extractString handles escaped quotes inside the value string
            String payloadStr = JsonUtils.extractString(json, "payload");

            return new NetworkMessage(type, actionBytes, payloadStr);

        } catch (Exception e) {
            System.err.println("NetworkMessage deserialization failed: " + e.getMessage());
            return null;
        }
    }
}