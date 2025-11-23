package com.swe.canvas.datamodel.manager;

import java.util.Map;
import java.util.Objects;

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
import com.swe.canvas.datamodel.serialization.SerializationException;
import com.swe.canvas.datamodel.serialization.SerializedAction;
import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;

public class HostActionManager implements ActionManager {

    // ... [Fields and Constructor remain the same] ...
    private final String userId;
    private final CanvasState canvasState;
    private final ActionFactory actionFactory;
    private final UndoRedoManager undoRedoManager;
    private final NetworkService networkService;
    private final DefaultActionSerializer serializer;
    private final DefaultActionDeserializer deserializer;
    private Runnable onUpdateCallback = () -> {};

    public HostActionManager(String userId, CanvasState canvasState, NetworkService networkService) {
        this.userId = userId;
        this.canvasState = canvasState;
        this.networkService = networkService;
        this.actionFactory = new ActionFactory();
        this.undoRedoManager = new UndoRedoManager();
        this.serializer = new DefaultActionSerializer();
        this.deserializer = new DefaultActionDeserializer();
        this.networkService.registerHost(this);
    }

    @Override public ActionFactory getActionFactory() { return actionFactory; }
    @Override public CanvasState getCanvasState() { return canvasState; }
    @Override public UndoRedoManager getUndoRedoManager() { return undoRedoManager; }
    @Override public void setOnUpdate(Runnable callback) { this.onUpdateCallback = callback; }

    // ... [Existing methods: validate, applyAndBroadcast, requestCreate/Modify/Delete/Undo/Redo] ...

    private boolean validate(Action action) {
        if (action.getActionType() == ActionType.CREATE) return true;
        ShapeState currentState = canvasState.getShapeState(action.getShapeId());
        ShapeState actionPrevState = action.getPrevState();
        return Objects.equals(currentState, actionPrevState);
    }

    private void applyAndBroadcast(Action action, NetworkMessage originalMessage) {
        canvasState.applyState(action.getShapeId(), action.getNewState());
        networkService.broadcastMessage(originalMessage);
    }

    @Override
    public void requestCreate(Shape newShape) {
        Action action = actionFactory.createCreateAction(newShape, userId);
        try {
            SerializedAction sa = serializer.serialize(action);
            processIncomingMessage(new NetworkMessage(MessageType.NORMAL, sa.getData()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void requestModify(ShapeState prevState, Shape modifiedShape) {
        Action action = actionFactory.createModifyAction(canvasState, prevState.getShapeId(), modifiedShape, userId);
        try {
            SerializedAction sa = serializer.serialize(action);
            processIncomingMessage(new NetworkMessage(MessageType.NORMAL, sa.getData()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void requestDelete(ShapeState shapeToDelete) {
        Action action = actionFactory.createDeleteAction(canvasState, shapeToDelete.getShapeId(), userId);
        try {
            SerializedAction sa = serializer.serialize(action);
            processIncomingMessage(new NetworkMessage(MessageType.NORMAL, sa.getData()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void requestUndo() {
        Action actionToUndo = undoRedoManager.getActionToUndo();
        if (actionToUndo != null) {
            Action inverse = actionFactory.createInverseAction(actionToUndo, userId);
            try {
                SerializedAction sa = serializer.serialize(inverse);
                processIncomingMessage(new NetworkMessage(MessageType.UNDO, sa.getData()));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void requestRedo() {
        Action actionToRedo = undoRedoManager.getActionToRedo();
        if (actionToRedo != null) {
            try {
                SerializedAction sa = serializer.serialize(actionToRedo);
                processIncomingMessage(new NetworkMessage(MessageType.REDO, sa.getData()));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // =========================================================================
    // NEW: Save / Restore Implementation
    // =========================================================================

    @Override
    public String saveMap() {
        // Serialize the internal map to JSON
        return ShapeSerializer.serializeShapesMap(canvasState.getAllStates());
    }

    @Override
    public void restoreMap(String json) {
        try {
            // 1. Deserialize locally
            Map<ShapeId, ShapeState> newMap = ShapeSerializer.deserializeShapesMap(json);

            // 2. Apply locally
            canvasState.setAllStates(newMap);
            undoRedoManager.clear(); // Clear stack on restore

            // 3. Broadcast RESTORE message to clients
            System.out.println("[Host] Broadcasting RESTORE...");
            NetworkMessage restoreMsg = new NetworkMessage(MessageType.RESTORE, null, json);
            networkService.broadcastMessage(restoreMsg);

            onUpdateCallback.run();
        } catch (Exception e) {
            System.err.println("[Host] Failed to restore map: " + e.getMessage());
        }
    }

    // =========================================================================

    @Override
    public void processIncomingMessage(NetworkMessage message) {
        // Host ignores incoming RESTORE (it is the source)
        if (message.getMessageType() == MessageType.RESTORE) return;

        try {
            Action action = deserializer.deserialize(new SerializedAction(message.getSerializedAction()));
            if (action == null) return;

            boolean isHostSelfAction = action.getNewState().getShape().getLastUpdatedBy().equals(userId);

            if (validate(action)) {
                if (isHostSelfAction) {
                    switch (message.getMessageType()) {
                        case NORMAL: undoRedoManager.push(action); break;
                        case UNDO: undoRedoManager.applyHostUndo(); break;
                        case REDO: undoRedoManager.applyHostRedo(); break;
                    }
                }
                applyAndBroadcast(action, message);
            } else {
                System.err.println("[Host] Conflict detected. Action rejected.");
            }
            onUpdateCallback.run();
        } catch (Exception e) {
            System.err.println("Host process failed: " + e.getMessage());
        }
    }
}