/*
 * -----------------------------------------------------------------------------
 * File: UndoRedoManagerTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */
package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.action.ActionType;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.MessageType;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.serialization.DefaultActionDeserializer;
import com.swe.canvas.datamodel.serialization.DefaultActionSerializer;
import com.swe.canvas.datamodel.serialization.SerializedAction;
import com.swe.canvas.datamodel.shape.LineShape;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientActionManagerTest {

    private ClientActionManager clientManager;
    private CanvasState canvasState;
    private NetworkService networkService;
    private final String userId = "CLIENT_USER";

    // --- Helper Classes & Methods ---

    private Shape createLineShape(ShapeId id, String user) {
        return new LineShape(
                id,
                new ArrayList<>(Arrays.asList(new Point(0, 0), new Point(10, 10))),
                1.0,
                Color.BLACK,
                user,
                user);
    }

    private void injectMock(Object target, String fieldName, Object mockValue) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mockValue);
    }

    static class ExceptionThrowingSerializer extends DefaultActionSerializer {
        @Override
        public SerializedAction serialize(Action action) {
            throw new RuntimeException("Serialization Boom!");
        }
    }

    static class NullReturningDeserializer extends DefaultActionDeserializer {
        @Override
        public Action deserialize(SerializedAction data) {
            return null;
        }
    }

    // FIX: Stub UndoRedoManager to force exceptions without Mockito
    static class ExceptionThrowingUndoRedoManager extends UndoRedoManager {
        @Override
        public Action getActionToRedo() {
            throw new RuntimeException("Redo access failed");
        }
    }

    static class ForceExceptionAction extends Action {
        private static final long serialVersionUID = 1L;

        public ForceExceptionAction(ShapeId shapeId) {
            super("bad-action", "user", 100L, ActionType.UNKNOWN, shapeId, null,
                    new ShapeState(
                            new LineShape(shapeId, new ArrayList<>(Arrays.asList(new Point(0, 0), new Point(1, 1))),
                                    1.0, Color.RED, "u", "u"),
                            false, 1L));
        }
    }

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
        networkService = mock(NetworkService.class);
        clientManager = new ClientActionManager(userId, canvasState, networkService);
        clearInvocations(networkService);
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

        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{}");
        clientManager.processIncomingMessage(msg);

        assertTrue(called.get());
    }

    @Test
    void testSetOnUpdate_Null() {
        clientManager.setOnUpdate(null);
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{}");
        assertDoesNotThrow(() -> clientManager.processIncomingMessage(msg));
    }

    // --- Outgoing Requests (Sending to Host) ---

    @Test
    void testRequestCreate() {
        Shape shape = createLineShape(new ShapeId("s1"), userId);
        clientManager.requestCreate(shape);
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.NORMAL));
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
    void testRequestDelete() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, userId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        clientManager.requestDelete(canvasState.getShapeState(id));
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.NORMAL));
    }

    @Test
    void testRequestUndo_WithAction() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        clientManager.getUndoRedoManager().push(action);

        clientManager.requestUndo();
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.UNDO));
    }

    @Test
    void testRequestRedo_WithAction() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        clientManager.getUndoRedoManager().push(action);
        clientManager.getUndoRedoManager().applyHostUndo();
        assertTrue(clientManager.getUndoRedoManager().canRedo(), "Should be able to Redo after Undo");

        clientManager.requestRedo();
        verify(networkService).sendMessageToHost(argThat(msg -> msg.getMessageType() == MessageType.REDO));
    }

    @Test
    void testRequestUndo_EmptyStack() {
        clientManager.requestUndo();
        verify(networkService, never()).sendMessageToHost(any());
    }

    @Test
    void testRequestRedo_EmptyStack() {
        clientManager.requestRedo();
        verify(networkService, never()).sendMessageToHost(any());
    }

    // --- Exception Handling in Requests ---

    @Test
    void testRequestCreate_SerializationError() throws Exception {
        injectMock(clientManager, "serializer", new ExceptionThrowingSerializer());

        Shape shape = createLineShape(new ShapeId("s1"), userId);
        assertDoesNotThrow(() -> clientManager.requestCreate(shape));
    }

    @Test
    void testRequestUndo_Exception() {
        Action badAction = new ForceExceptionAction(new ShapeId("s1"));
        clientManager.getUndoRedoManager().push(badAction);

        assertDoesNotThrow(() -> clientManager.requestUndo());
    }

    @Test
    void testRequestRedo_Exception() throws Exception {
        // Inject Stub UndoRedoManager that throws exception on getActionToRedo
        injectMock(clientManager, "undoRedoManager", new ExceptionThrowingUndoRedoManager());

        assertDoesNotThrow(() -> clientManager.requestRedo());
    }

    // --- Incoming Messages (Processing) ---

    @Test
    void testProcessIncomingMessage_Restore_Success() {
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{}");
        clientManager.processIncomingMessage(msg);
        assertEquals(0, canvasState.getAllStates().size());
    }

    @Test
    void testProcessIncomingMessage_Restore_NullPayload() {
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, null);
        clientManager.processIncomingMessage(msg);
    }

    @Test
    void testProcessIncomingMessage_Restore_BadPayload() {
        NetworkMessage msg = new NetworkMessage(MessageType.RESTORE, null, "{bad-json}");
        assertDoesNotThrow(() -> clientManager.processIncomingMessage(msg));
    }

    @Test
    void testProcessIncomingMessage_Normal_MyAction() throws Exception {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        DefaultActionSerializer serializer = new DefaultActionSerializer();
        SerializedAction sa = serializer.serialize(action);
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, sa.getData());

        clientManager.processIncomingMessage(msg);

        assertNotNull(canvasState.getShapeState(new ShapeId("s1")));
        assertTrue(clientManager.getUndoRedoManager().canUndo());
    }

    @Test
    void testProcessIncomingMessage_Normal_OtherUserAction() throws Exception {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), "OTHER"), "OTHER");
        DefaultActionSerializer serializer = new DefaultActionSerializer();
        SerializedAction sa = serializer.serialize(action);
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, sa.getData());

        clientManager.processIncomingMessage(msg);

        assertNotNull(canvasState.getShapeState(new ShapeId("s1")));
        assertFalse(clientManager.getUndoRedoManager().canUndo());
    }

    @Test
    void testProcessIncomingMessage_Undo_MyAction() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        clientManager.getUndoRedoManager().push(action);

        try {
            DefaultActionSerializer serializer = new DefaultActionSerializer();
            SerializedAction sa = serializer.serialize(action);
            NetworkMessage msg = new NetworkMessage(MessageType.UNDO, sa.getData());

            clientManager.processIncomingMessage(msg);

            assertFalse(clientManager.getUndoRedoManager().canUndo());
        } catch (Exception e) {
            fail("Setup failed");
        }
    }

    @Test
    void testProcessIncomingMessage_Redo_MyAction() {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        clientManager.getUndoRedoManager().push(action);
        clientManager.getUndoRedoManager().applyHostUndo();

        try {
            DefaultActionSerializer serializer = new DefaultActionSerializer();
            SerializedAction sa = serializer.serialize(action);
            NetworkMessage msg = new NetworkMessage(MessageType.REDO, sa.getData());

            clientManager.processIncomingMessage(msg);

            assertTrue(clientManager.getUndoRedoManager().canUndo());
        } catch (Exception e) {
            fail("Setup failed");
        }
    }

    @Test
    void testProcessIncomingMessage_UnknownType() throws Exception {
        Action action = new ActionFactory().createCreateAction(createLineShape(new ShapeId("s1"), userId), userId);
        DefaultActionSerializer serializer = new DefaultActionSerializer();
        SerializedAction sa = serializer.serialize(action);

        NetworkMessage msg = new NetworkMessage(MessageType.UNKNOWN, sa.getData());

        clientManager.processIncomingMessage(msg);
    }

    @Test
    void testProcessIncomingMessage_DeserializationNull() throws Exception {
        injectMock(clientManager, "deserializer", new NullReturningDeserializer());
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, "{}".getBytes());

        clientManager.processIncomingMessage(msg);
        assertEquals(0, canvasState.getAllStates().size());
    }

    @Test
    void testProcessIncomingMessage_DeserializationException() {
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, "bad-bytes".getBytes());
        assertDoesNotThrow(() -> clientManager.processIncomingMessage(msg));
    }

    @Test
    void testSaveMap() {
        String json = clientManager.saveMap();
        assertNotNull(json);
    }

    @Test
    void testRestoreMap() {
        assertDoesNotThrow(() -> clientManager.restoreMap("{}"));
    }
}