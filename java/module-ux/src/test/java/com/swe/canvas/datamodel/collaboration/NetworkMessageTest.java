package com.swe.canvas.datamodel.collaboration;

/*
 * -----------------------------------------------------------------------------
 * File: NetworkMessageTest.java
 * Owner: B S S Krishna
 * Roll Number: 112201013
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

import com.swe.controller.serialize.DataSerializer;

/**
 * Tests for NetworkMessage serialization and deserialization.
 */
class NetworkMessageTest {

    @Test
    void testConstructorAndGetters_WithAction() {
        String testMessage = "test-data";
        byte[] data = null;
        try {
            data = DataSerializer.serialize(testMessage);    
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);

        assertEquals(MessageType.NORMAL, msg.getMessageType());
        assertArrayEquals(data, msg.getSerializedAction());
        assertNull(msg.getPayload());
    }

    @Test
    void testConstructorAndGetters_WithPayload_NullAction() {
        // This tests the "else" branch in constructor (action == null)
        // and "return null" branch in getSerializedAction()
        String payload = "{json: true}";
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, payload);

        assertEquals(MessageType.RESTORE, msg.getMessageType());
        assertNull(msg.getSerializedAction());
        assertEquals(payload, msg.getPayload());
    }

    @Test
    void testGetters_DefensiveCopy() {
        byte[] original = {1, 2, 3};
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, original);
        
        // Modify original array
        original[0] = 99;
        
        // Message should not change (constructor defensive copy)
        assertNotEquals(99, msg.getSerializedAction()[0]);
        assertEquals(1, msg.getSerializedAction()[0]);
        
        // Modify getter result
        byte[] retrieved = msg.getSerializedAction();
        retrieved[0] = 88;
        
        // Message should not change (getter defensive copy)
        assertEquals(1, msg.getSerializedAction()[0]);
    }

    @Test
    void testSerialize_NormalMessage() {
        String abc = "abc";

        byte[] data = null;
        try {
            data = DataSerializer.serialize(abc);
        } catch (Exception e) {
            e.printStackTrace();
        }
         
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);
        
        String json = msg.serialize();
        
        assertTrue(json.contains("\"type\":\"NORMAL\""));
        assertTrue(json.contains("\"action\":"));
        // "abc" in Base64 is "YWJj"
        assertTrue(json.contains("YWJj"));
    }

    @Test
    void testSerialize_RestoreMessage() {
        String payload = "{\"key\":\"value\"}";
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, payload);
        
        String json = msg.serialize();
        
        assertTrue(json.contains("\"type\":\"RESTORE\""));
        assertTrue(json.contains("\"payload\":\"{\\\"key\\\":\\\"value\\\"}\""));
    }

    @Test
    void testRoundTrip_Normal() {
        byte[] data = {10, 20, 30};
        NetworkMessage original = new NetworkMessage(MessageType.UNDO, data);
        
        String json = original.serialize();
        NetworkMessage restored = NetworkMessage.deserialize(json);
        
        assertNotNull(restored);
        assertEquals(MessageType.UNDO, restored.getMessageType());
        assertArrayEquals(data, restored.getSerializedAction());
        assertNull(restored.getPayload());
    }

    @Test
    void testRoundTrip_Payload() {
        String content = "some-content";
        NetworkMessage original = new NetworkMessage(MessageType.RESTORE, null, content);
        
        String json = original.serialize();
        NetworkMessage restored = NetworkMessage.deserialize(json);
        
        assertNotNull(restored);
        assertEquals(MessageType.RESTORE, restored.getMessageType());
        assertNull(restored.getSerializedAction());
        assertEquals(content, restored.getPayload());
    }

    @Test
    void testDeserialize_NullOrEmpty() {
        assertNull(NetworkMessage.deserialize(null));
        assertNull(NetworkMessage.deserialize(""));
    }

    @Test
    void testDeserialize_MissingType_ReturnsNull() {
        String json = "{\"action\":\"YWJj\"}";
        assertNull(NetworkMessage.deserialize(json));
    }

    @Test
    void testDeserialize_InvalidBase64_ReturnsNull() {
        String json = "{\"type\":\"NORMAL\", \"action\":\"!!!NotBase64!!!\"}";
        assertNull(NetworkMessage.deserialize(json));
    }
    
    @Test
    void testDeserialize_UnknownTypeException() {
        String json = "{\"type\":\"INVALID_ENUM_VAL\"}";
        assertNull(NetworkMessage.deserialize(json));
    }
}