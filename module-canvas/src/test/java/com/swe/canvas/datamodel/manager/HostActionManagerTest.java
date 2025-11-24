/*
 * -----------------------------------------------------------------------------
 * File: HostActionManagerTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionType;
import com.swe.canvas.datamodel.action.ModifyShapeAction;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.MessageType;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.serialization.DefaultActionSerializer;
import com.swe.canvas.datamodel.shape.FreehandShape;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HostActionManagerTest {

    private HostActionManager host;
    private NetworkService mockNetwork;
    private CanvasState realCanvasState;
    private final String userId = "host-user";
    private DefaultActionSerializer serializer;

    // --- Stub for triggering exceptions in Restore ---
    static class FaultyCanvasState extends CanvasState {
        @Override
        public void setAllStates(Map<ShapeId, ShapeState> newStates) {
            throw new RuntimeException("Simulated Failure");
        }
    }

    // --- Stub Action for testing Undo Exception handling ---
    static class UnknownAction extends Action {
        private static final long serialVersionUID = 1L;
        public UnknownAction(String actionId) {
            super(actionId, "u", 1L, ActionType.UNKNOWN, new ShapeId("s"), null, 
                  new ShapeState(new FreehandShape(new ShapeId("s"), new ArrayList<>(Arrays.asList(new Point(0,0))), 1, Color.BLACK, "u", "u"), false, 1L));
        }
    }

    private Shape createShape(String id) {
        return new FreehandShape(new ShapeId(id), new ArrayList<>(Arrays.asList(new Point(0, 0))), 1.0, Color.BLACK, "u", "u");
    }

    @BeforeEach
    void setUp() {
        mockNetwork = mock(NetworkService.class);
        realCanvasState = new CanvasState();
        host = new HostActionManager(userId, realCanvasState, mockNetwork);
        serializer = new DefaultActionSerializer();
    }

    @Test
    void testRequestCreate_Success() {
        Shape shape = createShape("s1");
        host.requestCreate(shape);
        assertNotNull(realCanvasState.getShapeState(shape.getShapeId()));
        verify(mockNetwork).broadcastMessage(any(NetworkMessage.class));
    }

    @Test
    void testRequestCreate_Exception() {
        host.requestCreate(null);
        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testRequestModify_Success() {
        ShapeId id = new ShapeId("s1");
        Shape shape = createShape("s1");
        realCanvasState.applyState(id, new ShapeState(shape, false, 100L));
        
        ShapeState prevState = realCanvasState.getShapeState(id);
        Shape modified = createShape("s1");
        modified.setColor(Color.RED);

        host.requestModify(prevState, modified);
        verify(mockNetwork).broadcastMessage(any());
    }

    @Test
    void testRequestModify_Exception() {
        ShapeId id = new ShapeId("s1");
        ShapeState deletedState = new ShapeState(createShape("s1"), true, 100L);
        realCanvasState.applyState(id, deletedState);
        
        host.requestModify(deletedState, createShape("s1"));
        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testRequestDelete_Success() {
        ShapeId id = new ShapeId("s1");
        realCanvasState.applyState(id, new ShapeState(createShape("s1"), false, 100L));
        host.requestDelete(realCanvasState.getShapeState(id));
        verify(mockNetwork).broadcastMessage(any());
    }

    @Test
    void testRequestDelete_Exception() {
        ShapeState deletedState = new ShapeState(createShape("s1"), true, 100L);
        host.requestDelete(deletedState);
        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testRequestUndoRedo_Empty() {
        host.requestUndo();
        host.requestRedo();
        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testRequestUndo_Exception() {
        Action badAction = new UnknownAction("bad-act");
        host.getUndoRedoManager().push(badAction);
        
        assertDoesNotThrow(() -> host.requestUndo());
        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testProcessIncoming_SelfCreate() {
        Shape shape = createShape("s1");
        shape.setLastUpdatedBy(userId); 
        Action action = host.getActionFactory().createCreateAction(shape, userId);
        byte[] data = serializer.serialize(action).getData();
        
        host.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, data));
        
        assertNotNull(realCanvasState.getShapeState(shape.getShapeId()));
        assertTrue(host.getUndoRedoManager().canUndo());
        verify(mockNetwork).broadcastMessage(any());
    }

    @Test
    void testProcessIncoming_ClientCreate() {
        Shape shape = createShape("s1");
        String clientUser = "client-1";
        shape.setLastUpdatedBy(clientUser);
        Action action = host.getActionFactory().createCreateAction(shape, clientUser);
        byte[] data = serializer.serialize(action).getData();

        host.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, data));

        assertNotNull(realCanvasState.getShapeState(shape.getShapeId()));
        assertFalse(host.getUndoRedoManager().canUndo());
        verify(mockNetwork).broadcastMessage(any());
    }

    @Test
    void testProcessIncoming_Conflict_Reject() {
        ShapeId id = new ShapeId("s1");
        Shape shape = createShape("s1");
        realCanvasState.applyState(id, new ShapeState(shape, false, 200L));

        ShapeState stalePrev = new ShapeState(shape, false, 100L);
        ShapeState next = new ShapeState(shape, false, 300L);
        Action conflictAction = new ModifyShapeAction("a1", "c1", 300L, id, stalePrev, next);
        
        byte[] data = serializer.serialize(conflictAction).getData();
        host.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, data));

        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testProcessIncoming_HostUndo() {
        Shape shape = createShape("s1");
        host.requestCreate(shape); 
        
        Action undoAction = host.getUndoRedoManager().getActionToUndo();
        Action inverse = host.getActionFactory().createInverseAction(undoAction, userId);
        byte[] data = serializer.serialize(inverse).getData();
        
        host.processIncomingMessage(new NetworkMessage(MessageType.UNDO, data));
        
        assertTrue(host.getUndoRedoManager().canRedo());
    }

    @Test
    void testProcessIncoming_HostRedo() {
        Shape shape = createShape("s1");
        host.requestCreate(shape);
        host.getUndoRedoManager().applyHostUndo(); 
        
        Action redoAction = host.getUndoRedoManager().getActionToRedo();
        byte[] data = serializer.serialize(redoAction).getData();
        
        host.processIncomingMessage(new NetworkMessage(MessageType.REDO, data));
        
        assertFalse(host.getUndoRedoManager().canRedo());
        assertTrue(host.getUndoRedoManager().canUndo());
    }
    
    @Test
    void testProcessIncoming_BadData_Exception() {
        host.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, new byte[]{1, 2, 3}));
        verify(mockNetwork, never()).broadcastMessage(any());
    }
    
    @Test
    void testProcessIncoming_NullAction() {
        host.processIncomingMessage(new NetworkMessage(MessageType.NORMAL, "null".getBytes()));
        verify(mockNetwork, never()).broadcastMessage(any());
    }
    
    @Test
    void testProcessIncoming_Restore_Ignored() {
        host.processIncomingMessage(new NetworkMessage(MessageType.RESTORE, null, "{}"));
        verify(mockNetwork, never()).broadcastMessage(any());
    }

    @Test
    void testSaveRestore_Success() {
        ShapeId id = new ShapeId("s1");
        realCanvasState.applyState(id, new ShapeState(createShape("s1"), false, 100L));
        
        String json = host.saveMap();
        assertNotNull(json);
        
        realCanvasState.clear();
        host.restoreMap(json);
        
        assertNotNull(realCanvasState.getShapeState(id));
        verify(mockNetwork).broadcastMessage(argThat(m -> m.getMessageType() == MessageType.RESTORE));
    }

    @Test
    void testRestore_Exception() {
        String maliciousJson = "{ \"some-id\": { } }";
        host.restoreMap(maliciousJson);
        verify(mockNetwork, never()).broadcastMessage(any());
    }
    
    @Test
    void testRestore_LogicException() {
        CanvasState faulty = new FaultyCanvasState();
        HostActionManager faultyHost = new HostActionManager(userId, faulty, mockNetwork);
        
        faultyHost.restoreMap("{}");
        verify(mockNetwork, never()).broadcastMessage(any());
    }
    
    @Test
    void testCallback() {
        AtomicBoolean flag = new AtomicBoolean(false);
        host.setOnUpdate(() -> flag.set(true));
        host.requestCreate(createShape("s1"));
        assertTrue(flag.get());
    }
}