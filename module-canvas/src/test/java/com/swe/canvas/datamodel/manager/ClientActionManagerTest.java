/*
 * -----------------------------------------------------------------------------
 * File: ClientActionManagerTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.MessageType;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.serialization.DefaultActionSerializer;
import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.canvas.datamodel.shape.FreehandShape;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientActionManagerTest {

    private ClientActionManager client;
    private NetworkService mockNetwork;
    private CanvasState realCanvasState;
    private final String userId = "client-user";
    private DefaultActionSerializer serializer;

    // --- Stub for triggering exceptions ---
    static class FaultyCanvasState extends CanvasState {
        @Override
        public void setAllStates(Map<ShapeId, ShapeState> newStates) {
            throw new RuntimeException("Simulated Failure");
        }
    }

    private Shape createShape(String id) {
        return new FreehandShape(new ShapeId(id), new ArrayList<>(Arrays.asList(new Point(0, 0))), 1.0, Color.BLACK, "u", "u");
    }

    @BeforeEach
    void setUp() {
        mockNetwork = mock(NetworkService.class);
        realCanvasState = new CanvasState(); // Real object
        client = new ClientActionManager(userId, realCanvasState, mockNetwork);
        serializer = new DefaultActionSerializer();
    }

    @Test
    void testRequests_Create() {
        client.requestCreate(createShape("s1"));
        verify(mockNetwork).sendMessageToHost(any(NetworkMessage.class));
    }

    @Test
    void testRequests_Modify() {
        ShapeId id = new ShapeId("s1");
        realCanvasState.applyState(id, new ShapeState(createShape("s1"), false, 100L));
        client.requestModify(realCanvasState.getShapeState(id), createShape("s1"));
        verify(mockNetwork).sendMessageToHost(any());
    }

    @Test
    void testRequests_Delete() {
        ShapeId id = new ShapeId("s1");
        realCanvasState.applyState(id, new ShapeState(createShape("s1"), false, 100L));
        client.requestDelete(realCanvasState.getShapeState(id));
        verify(mockNetwork).sendMessageToHost(any());
    }

    @Test
    void testRequests_UndoRedo() {
        client.requestUndo();
        client.requestRedo();
        verify(mockNetwork, never()).sendMessageToHost(any());

        Shape shape = createShape("s1");
        shape.setLastUpdatedBy(userId);
        Action action = client.getActionFactory().createCreateAction(shape, userId);
        client.getUndoRedoManager().push(action);

        client.requestUndo();
        verify(mockNetwork, times(1)).sendMessageToHost(argThat(m -> m.getMessageType() == MessageType.UNDO));
        
        client.getUndoRedoManager().applyHostUndo();
        
        client.requestRedo();
        verify(mockNetwork, times(1)).sendMessageToHost(argThat(m -> m.getMessageType() == MessageType.REDO));
    }
    
    @Test
    void testSendException() {
        doThrow(new RuntimeException("Net fail")).when(mockNetwork).sendMessageToHost(any());
        assertDoesNotThrow(() -> client.requestCreate(createShape("s1")));
    }
    
    @Test
    void testCreationExceptions() {
        client.requestCreate(null);
        verify(mockNetwork, never()).sendMessageToHost(any());
        
        client.requestModify(null, null);
        verify(mockNetwork, never()).sendMessageToHost(any());
        
        client.requestDelete(null);
        verify(mockNetwork, never()).sendMessageToHost(any());
    }

    @Test
    void testIncoming_Restore() {
        ShapeId id = new ShapeId("s1");
        ShapeState st = new ShapeState(createShape("s1"), false, 100L);
        String json = ShapeSerializer.serializeShapesMap(Map.of(id, st));
        
        client.processIncomingMessage(new NetworkMessage(MessageType.RESTORE, null, json));
        assertNotNull(realCanvasState.getShapeState(id));
    }
    
    @Test
    void testIncoming_Restore_Exception() {
        CanvasState faulty = new FaultyCanvasState();
        ClientActionManager faultyClient = new ClientActionManager(userId, faulty, mockNetwork);
        String json = "{\"s1\": null}"; 
        faultyClient.processIncomingMessage(new NetworkMessage(MessageType.RESTORE, null, json));
        assertNull(realCanvasState.getShapeState(new ShapeId("s1")));
    }
    
    @Test
    void testIncoming_Restore_NullPayload() {
        client.processIncomingMessage(new NetworkMessage(MessageType.RESTORE, null, null));
        assertTrue(realCanvasState.getAllStates().isEmpty());
    }

    @Test
    void testIncoming_Normal_OwnAction() {
        Shape shape = createShape("s1");
        shape.setLastUpdatedBy(userId);
        Action action = client.getActionFactory().createCreateAction(shape, userId);
        byte[] data = serializer.serialize(action).getData();

        client.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, data));
        
        assertNotNull(realCanvasState.getShapeState(shape.getShapeId()));
        assertTrue(client.getUndoRedoManager().canUndo());
    }

    @Test
    void testIncoming_Normal_OtherAction() {
        Shape shape = createShape("s1");
        String otherUser = "other";
        shape.setLastUpdatedBy(otherUser);
        Action action = client.getActionFactory().createCreateAction(shape, otherUser);
        byte[] data = serializer.serialize(action).getData();

        client.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, data));

        assertNotNull(realCanvasState.getShapeState(shape.getShapeId()));
        assertFalse(client.getUndoRedoManager().canUndo());
    }
    
    @Test
    void testIncoming_UndoRedo_Own() {
        Shape shape = createShape("s1");
        shape.setLastUpdatedBy(userId);
        Action action = client.getActionFactory().createCreateAction(shape, userId);
        client.getUndoRedoManager().push(action);
        
        Action inverse = client.getActionFactory().createInverseAction(action, userId);
        byte[] data = serializer.serialize(inverse).getData();
        
        client.processIncomingMessage(new NetworkMessage(MessageType.UNDO, data));
        assertTrue(client.getUndoRedoManager().canRedo());
        
        data = serializer.serialize(action).getData();
        client.processIncomingMessage(new NetworkMessage(MessageType.REDO, data));
        assertFalse(client.getUndoRedoManager().canRedo());
    }
    
    @Test
    void testIncoming_BadData() {
        client.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, new byte[]{0, 1}));
        assertTrue(realCanvasState.getAllStates().isEmpty());
    }
    
    @Test
    void testIncoming_NullAction() {
        client.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, "null".getBytes()));
        assertTrue(realCanvasState.getAllStates().isEmpty());
    }

    @Test
    void testLocalSaveRestore() {
        assertDoesNotThrow(() -> client.restoreMap("{}")); 
        assertNotNull(client.saveMap());
    }
    
    @Test
    void testGettersAndCallback() {
        assertNotNull(client.getActionFactory());
        assertEquals(realCanvasState, client.getCanvasState());
        assertNotNull(client.getUndoRedoManager());
        
        AtomicBoolean flag = new AtomicBoolean(false);
        client.setOnUpdate(() -> flag.set(true));
        client.processIncomingMessage(new NetworkMessage(MessageType.RESTORE, null, "{}"));
        assertTrue(flag.get());
    }
}