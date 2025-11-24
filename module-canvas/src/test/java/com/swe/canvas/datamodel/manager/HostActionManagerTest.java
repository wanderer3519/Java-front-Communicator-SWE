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
import com.swe.canvas.datamodel.action.ModifyShapeAction;
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

class HostActionManagerTest {

    private HostActionManager hostManager;
    private CanvasState canvasState;
    private NetworkService networkService;
    private final String hostId = "HOST_USER";

    // --- Helper Classes & Methods ---

    private Shape createLineShape(final ShapeId id, final String userId) {
        return new LineShape(
                id,
                new ArrayList<>(Arrays.asList(new Point(0, 0), new Point(10, 10))),
                1.0,
                Color.BLACK,
                userId,
                userId
        );
    }

    private void injectMock(final Object target, final String fieldName, final Object mockValue) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mockValue);
    }

    // Stub Serializer to force exceptions
    static class ExceptionThrowingSerializer extends DefaultActionSerializer {
        @Override
        public SerializedAction serialize(final Action action) {
            throw new RuntimeException("Serialization Boom!");
        }
    }

    // Stub Deserializer to return null (forces "action == null" branch)
    static class NullReturningDeserializer extends DefaultActionDeserializer {
        @Override
        public Action deserialize(final SerializedAction data) {
            return null;
        }
    }

    // Stub Action to force exceptions via unknown type logic
    static class ForceExceptionAction extends Action {
        private static final long serialVersionUID = 1L;
        public ForceExceptionAction(final ShapeId shapeId) {
            super("bad-action", "user", 100L, ActionType.UNKNOWN, shapeId, null,
                    new ShapeState(
                            new LineShape(shapeId, new ArrayList<>(Arrays.asList(new Point(0,0), new Point(1,1))), 1.0, Color.RED, "u", "u"),
                            false, 1L)
            );
        }
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
    void testSetOnUpdate_Null() {
        hostManager.setOnUpdate(null);
        assertDoesNotThrow(() -> hostManager.restoreMap("{}"));
    }

    @Test
    void testRequestCreate_HappyPath() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);
        assertNotNull(canvasState.getShapeState(shape.getShapeId()));
    }

    @Test
    void testProcessIncomingMessage_SelfAction_Normal() throws Exception {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        ActionFactory factory = new ActionFactory();
        Action action = factory.createCreateAction(shape, hostId);

        DefaultActionSerializer serializer = new DefaultActionSerializer();
        SerializedAction sa = serializer.serialize(action);
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, sa.getData());

        hostManager.processIncomingMessage(msg);

        assertNotNull(canvasState.getShapeState(shape.getShapeId()));
        assertTrue(hostManager.getUndoRedoManager().canUndo());
    }

    @Test
    void testProcessIncomingMessage_SelfAction_Undo() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);
        assertTrue(hostManager.getUndoRedoManager().canUndo());

        hostManager.requestUndo();

        ShapeState state = canvasState.getShapeState(shape.getShapeId());
        assertTrue(state.isDeleted());
    }

    @Test
    void testProcessIncomingMessage_SelfAction_Redo() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);
        hostManager.requestUndo();
        assertTrue(hostManager.getUndoRedoManager().canRedo());

        hostManager.requestRedo();

        assertFalse(canvasState.getShapeState(shape.getShapeId()).isDeleted());
    }

    @Test
    void testProcessIncomingMessage_UnknownType() throws Exception {
        // 1. Create valid action
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        ActionFactory factory = new ActionFactory();
        Action action = factory.createCreateAction(shape, hostId);

        // 2. Serialize
        DefaultActionSerializer serializer = new DefaultActionSerializer();
        SerializedAction sa = serializer.serialize(action);

        // 3. Message with UNKNOWN type (hits the default switch case)
        NetworkMessage msg = new NetworkMessage(MessageType.UNKNOWN, sa.getData());

        // 4. Process - should hit default case and do nothing
        hostManager.processIncomingMessage(msg);

        // 5. Verify no side effects (undo stack should not have grown)
        // Since requestCreate was not called, stack size is 0
        assertFalse(hostManager.getUndoRedoManager().canUndo());
    }

    @Test
    void testProcessIncomingMessage_Conflict_Reject() throws Exception {
        Shape shape = createLineShape(new ShapeId("s1"), "client");
        ShapeState prevState = new ShapeState(shape, false, 100L);
        ShapeState newState = new ShapeState(shape, false, 200L);

        Action conflictAction = new ModifyShapeAction(
                "act-1", "client", 200L, shape.getShapeId(), prevState, newState
        );

        DefaultActionSerializer serializer = new DefaultActionSerializer();
        SerializedAction sa = serializer.serialize(conflictAction);
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, sa.getData());

        hostManager.processIncomingMessage(msg);

        assertNull(canvasState.getShapeState(shape.getShapeId()));
        verify(networkService, never()).broadcastMessage(any());
    }

    @Test
    void testProcessIncomingMessage_DeserializationFailure() {
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, "not-json".getBytes());
        assertDoesNotThrow(() -> hostManager.processIncomingMessage(msg));
    }

    @Test
    void testProcessIncomingMessage_ActionNull() throws Exception {
        // Inject Stub Deserializer that returns null
        injectMock(hostManager, "deserializer", new NullReturningDeserializer());

        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, "{}".getBytes());

        hostManager.processIncomingMessage(msg);

        assertEquals(0, canvasState.getAllStates().size());
    }

    @Test
    void testProcessIncomingMessage_Restore_Ignored() {
        NetworkMessage restoreMsg = new NetworkMessage(MessageType.RESTORE, null, "{}");
        hostManager.processIncomingMessage(restoreMsg);
        assertEquals(0, canvasState.getAllStates().size());
    }

    @Test
    void testRequestUndo_ExceptionHandling() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        Action badAction = new ForceExceptionAction(shape.getShapeId());

        hostManager.getUndoRedoManager().push(badAction);

        assertDoesNotThrow(() -> hostManager.requestUndo());
    }

    @Test
    void testRequestRedo_ExceptionHandling() throws Exception {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);
        hostManager.requestUndo();
        assertTrue(hostManager.getUndoRedoManager().canRedo());

        // Inject Stub Serializer that throws RuntimeException
        injectMock(hostManager, "serializer", new ExceptionThrowingSerializer());

        assertDoesNotThrow(() -> hostManager.requestRedo());
    }

    @Test
    void testRequestRedo_NothingToRedo() {
        hostManager.requestRedo();
    }

    @Test
    void testSaveAndRestoreMap() {
        Shape shape = createLineShape(new ShapeId("s1"), hostId);
        hostManager.requestCreate(shape);

        String json = hostManager.saveMap();
        assertTrue(json.contains("s1"));

        canvasState.clear();
        hostManager.restoreMap(json);

        assertNotNull(canvasState.getShapeState(new ShapeId("s1")));
        verify(networkService).broadcastMessage(argThat(msg -> msg.getMessageType() == MessageType.RESTORE));
    }

    @Test
    void testRequestModify_HappyPath() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, hostId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        hostManager.requestModify(canvasState.getShapeState(id), createLineShape(id, hostId));

        assertTrue(canvasState.getShapeState(id).getLastModified() > 100L);
    }

    @Test
    void testRequestDelete_HappyPath() {
        ShapeId id = new ShapeId("s1");
        Shape s1 = createLineShape(id, hostId);
        canvasState.applyState(id, new ShapeState(s1, false, 100L));

        hostManager.requestDelete(canvasState.getShapeState(id));

        assertTrue(canvasState.getShapeState(id).isDeleted());
    }
}