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
import org.mockito.Mockito;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.swe.controller.serialize.DataSerializer;

class ClientActionManagerTest {

    private ClientActionManager clientManager;
    private CanvasState canvasState;
    private NetworkService networkService;
    private final String userId = "CLIENT_USER";

    private Shape createLineShape(ShapeId id, String user) {
        return new LineShape(
                id,
                new ArrayList<>(Arrays.asList(new Point(0, 0), new Point(10, 10))),
                1.0,
                Color.BLACK,
                user,
                user);
    }

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
        networkService = mock(NetworkService.class);
        clientManager = new ClientActionManager(userId, canvasState, networkService);
    }

    @Test
    void testGetters() {
        assertNotNull(clientManager.getActionFactory());
        assertEquals(canvasState, clientManager.getCanvasState());
        assertNotNull(clientManager.getUndoRedoManager());
    }

    @Test
    void testSetOnUpdate() {
        AtomicBoolean called = new AtomicBoolean(false);
        clientManager.setOnUpdate(() -> called.set(true));
        // Trigger update via incoming message
        String json = "{\"s1\": null}"; // valid map json
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, json);
        clientManager.processIncomingMessage(msg);
        assertTrue(called.get());
    }

    @Test
    void testSetOnUpdate_Null() {
        clientManager.setOnUpdate(null);
        // Should not throw NPE on update
        String json = "{}";
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, json);
        assertDoesNotThrow(() -> clientManager.processIncomingMessage(msg));
    }

    @Test
    void testRequestCreate() {
        Shape shape = createLineShape(new ShapeId("s1"), userId);
        clientManager.requestCreate(shape);
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.NORMAL));
    }

    @Test
    void testRequestCreate_Exception() {
        // Mock network service to throw runtime exception
        doThrow(new RuntimeException("Send failed")).when(networkService).sendMessageToHost(any());
        Shape shape = createLineShape(new ShapeId("s1"), userId);
        assertDoesNotThrow(() -> clientManager.requestCreate(shape));
    }

    @Test
    void testRequestModify() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, userId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        clientManager.requestModify(canvasState.getShapeState(id), createLineShape(id, userId));
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.NORMAL));
    }

    @Test
    void testRequestModify_Exception() {
        // Try to modify non-existent shape -> throws IllegalStateException inside
        // ActionFactory
        ShapeId id = new ShapeId("non-existent");
        Shape shape = createLineShape(id, userId);
        // Pass dummy state (not in canvas)
        ShapeState dummyState = new ShapeState(shape, false, 100L);

        // Since factory throws before network send, this tests exception handling
        assertDoesNotThrow(() -> clientManager.requestModify(dummyState, shape));
    }

    @Test
    void testRequestDelete() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, userId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        clientManager.requestDelete(canvasState.getShapeState(id));
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.NORMAL));
    }

    @Test
    void testRequestUndo() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        clientManager.getUndoRedoManager().push(action);

        clientManager.requestUndo();
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.UNDO));
    }

    @Test
    void testRequestRedo() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        clientManager.getUndoRedoManager().push(action);
        clientManager.getUndoRedoManager().applyHostUndo();

        clientManager.requestRedo();
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.REDO));
    }

    @Test
    void testSaveAndRestoreMap() {
        String map = clientManager.saveMap();
        assertNotNull(map);
        // Restore locally logs but does nothing
        assertDoesNotThrow(() -> clientManager.restoreMap("{}"));
    }

    @Test
    void testProcessIncomingMessage_Restore() {
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{}");
        clientManager.processIncomingMessage(msg);
        assertTrue(canvasState.getAllStates().isEmpty());
    }

    @Test
    void testProcessIncomingMessage_Restore_Invalid() {
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{bad-json}");
        assertDoesNotThrow(() -> clientManager.processIncomingMessage(msg));
    }

    @Test
    void testProcessIncomingMessage_Normal_MyAction() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        String json = NetActionSerializer.serializeAction(action);

        byte[] data = null;
        try {
            data = DataSerializer.serialize(json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);

        clientManager.processIncomingMessage(msg);
        assertNotNull(canvasState.getShapeState(new ShapeId("s1")));
        assertTrue(clientManager.getUndoRedoManager().canUndo());
    }

    @Test
    void testProcessIncomingMessage_Normal_OtherAction() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), "OTHER"), "OTHER");
        String json = NetActionSerializer.serializeAction(action);

        byte[] data = null;
        try {
            data = DataSerializer.serialize(json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);

        clientManager.processIncomingMessage(msg);
        assertNotNull(canvasState.getShapeState(new ShapeId("s1")));
        assertFalse(clientManager.getUndoRedoManager().canUndo());
    }

    @Test
    void testProcessIncomingMessage_BadAction() {
        String badJson = "bad-json";

        byte[] data = null;
        try {
            data = DataSerializer.serialize(badJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, data);
        assertDoesNotThrow(() -> clientManager.processIncomingMessage(msg));
    }
}