package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.action.ActionType;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.MessageType;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.serialization.NetActionSerializer;
import com.swe.canvas.datamodel.shape.LineShape;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.swe.controller.serialize.DataSerializer;

class HostActionManagerTest {

    private HostActionManager hostManager;
    private CanvasState canvasState;
    private NetworkService networkService;
    private final String hostId = "HOST_USER";

    private Shape createLineShape(final ShapeId id, final String userId) {
        return new LineShape(
                id,
                new ArrayList<>(Arrays.asList(new Point(0, 0), new Point(10, 10))),
                1.0,
                Color.BLACK,
                userId,
                userId);
    }

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
        networkService = mock(NetworkService.class);
        hostManager = new HostActionManager(hostId, canvasState, networkService);
    }

    @Test
    void testGetters() {
        assertNotNull(hostManager.getActionFactory());
        assertEquals(canvasState, hostManager.getCanvasState());
        assertNotNull(hostManager.getUndoRedoManager());
    }

    @Test
    void testSetOnUpdate() {
        AtomicBoolean called = new AtomicBoolean(false);
        hostManager.setOnUpdate(() -> called.set(true));
        hostManager.restoreMap("{}");
        assertTrue(called.get());
    }

    @Test
    void testRequestCreate() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);
        assertNotNull(canvasState.getShapeState(shape.getShapeId()));
        verify(networkService).broadcastMessage(any());
    }

    @Test
    void testRequestModify() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, hostId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        hostManager.requestModify(canvasState.getShapeState(id), createLineShape(id, hostId));
        verify(networkService).broadcastMessage(any());
    }

    @Test
    void testRequestDelete() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, hostId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        hostManager.requestDelete(canvasState.getShapeState(id));
        verify(networkService).broadcastMessage(any());
    }

    @Test
    void testRequestUndoRedo() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);

        hostManager.requestUndo();
        assertTrue(canvasState.getShapeState(shape.getShapeId()).isDeleted());

        hostManager.requestRedo();
        assertFalse(canvasState.getShapeState(shape.getShapeId()).isDeleted());
    }

    @Test
    void testProcessIncomingMessage_Normal() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), "CLIENT"), "CLIENT");
        String json = NetActionSerializer.serializeAction(action);

        byte[] data = null;
        try {
            data = DataSerializer.serialize(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);

        hostManager.processIncomingMessage(msg);
        assertNotNull(canvasState.getShapeState(new ShapeId("s1")));
        verify(networkService).broadcastMessage(msg);
    }

    @Test
    void testProcessIncomingMessage_Conflict() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, "U1");

        // Host state has timestamp 200
        canvasState.applyState(id, new ShapeState(s1, false, 200L));

        // Incoming action assumes timestamp 100 (stale)
        ShapeState staleState = new ShapeState(s1, false, 100L);
        Action conflictAction = new ActionFactory().createModifyAction(
                new CanvasState(), id, createLineShape(id, "U2"), "U2");
        
    }

    @Test
    void testProcessIncomingMessage_Restore() {
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{}");
        hostManager.processIncomingMessage(msg);
        // Should return early, no broadcast
        verify(networkService, never()).broadcastMessage(any());
    }

    @Test
    void testRestoreMap() {
        hostManager.restoreMap("{}");
        verify(networkService).broadcastMessage(argThat(msg -> msg.getMessageType() == MessageType.RESTORE));
    }

    @Test
    void testExceptionHandling() {
        // Malformed JSON
        String bad = "bad";
        
        byte[] data = null;
        try {
            data = DataSerializer.serialize(bad);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);
        assertDoesNotThrow(() -> hostManager.processIncomingMessage(msg));
    }
}