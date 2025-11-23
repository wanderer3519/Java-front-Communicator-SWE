package com.swe.canvas.datamodel.manager;

import java.util.Map;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.MessageType;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.serialization.DefaultActionDeserializer;
import com.swe.canvas.datamodel.serialization.DefaultActionSerializer;
import com.swe.canvas.datamodel.serialization.SerializedAction;
import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;

public class ClientActionManager implements ActionManager {

    // ... [Fields and Constructor] ...
    private final String userId;
    private final CanvasState canvasState;
    private final ActionFactory actionFactory;
    private final UndoRedoManager undoRedoManager;
    private final NetworkService networkService;
    private final DefaultActionSerializer serializer;
    private final DefaultActionDeserializer deserializer;
    private Runnable onUpdateCallback = () -> {};

    public ClientActionManager(String userId, CanvasState canvasState, NetworkService networkService) {
        this.userId = userId;
        this.canvasState = canvasState;
        this.networkService = networkService;
        this.actionFactory = new ActionFactory();
        this.undoRedoManager = new UndoRedoManager();
        this.serializer = new DefaultActionSerializer();
        this.deserializer = new DefaultActionDeserializer();
        this.networkService.registerClient(this);
    }

    @Override public ActionFactory getActionFactory() { return actionFactory; }
    @Override public CanvasState getCanvasState() { return canvasState; }
    @Override public UndoRedoManager getUndoRedoManager() { return undoRedoManager; }
    @Override public void setOnUpdate(Runnable callback) { this.onUpdateCallback = callback; }

    private void sendActionToHost(Action action, MessageType type) {
        try {
            SerializedAction serializedAction = serializer.serialize(action);
            NetworkMessage message = new NetworkMessage(type, serializedAction.getData());
            networkService.sendMessageToHost(message);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ... [Request methods identical to previous file] ...
    @Override public void requestCreate(Shape newShape) {
        Action action = actionFactory.createCreateAction(newShape, userId);
        sendActionToHost(action, MessageType.NORMAL);
    }
    @Override public void requestModify(ShapeState prevState, Shape modifiedShape) {
        Action action = actionFactory.createModifyAction(canvasState, prevState.getShapeId(), modifiedShape, userId);
        sendActionToHost(action, MessageType.NORMAL);
    }
    @Override public void requestDelete(ShapeState shapeToDelete) {
        Action action = actionFactory.createDeleteAction(canvasState, shapeToDelete.getShapeId(), userId);
        sendActionToHost(action, MessageType.NORMAL);
    }
    @Override public void requestUndo() {
        Action a = undoRedoManager.getActionToUndo();
        if (a != null) sendActionToHost(actionFactory.createInverseAction(a, userId), MessageType.UNDO);
    }
    @Override public void requestRedo() {
        Action a = undoRedoManager.getActionToRedo();
        if (a != null) sendActionToHost(a, MessageType.REDO);
    }

    // =========================================================================
    // NEW: Save / Restore Stubs
    // =========================================================================
    @Override
    public String saveMap() {
        // Client usually doesn't save, but can return its view
        return ShapeSerializer.serializeShapesMap(canvasState.getAllStates());
    }

    @Override
    public void restoreMap(String json) {
        // Clients usually do not trigger restore locally, they receive it via network.
        // But if needed:
        System.out.println("[Client] Restore triggered locally (uncommon).");
    }

    // =========================================================================

    @Override
    public void processIncomingMessage(NetworkMessage message) {
        // 1. Handle RESTORE
        if (message.getMessageType() == MessageType.RESTORE) {
            System.out.println("[Client " + userId + "] Received RESTORE command.");
            if (message.getPayload() != null) {
                Map<ShapeId, ShapeState> newMap = ShapeSerializer.deserializeShapesMap(message.getPayload());
                canvasState.setAllStates(newMap);
                undoRedoManager.clear(); // Clear stack as history is invalidated
                onUpdateCallback.run();
            }
            return;
        }

        // 2. Handle Normal Actions
        try {
            Action action = deserializer.deserialize(new SerializedAction(message.getSerializedAction()));
            if (action == null) return;

            boolean isMyAction = action.getNewState().getShape().getLastUpdatedBy().equals(userId);
            canvasState.applyState(action.getShapeId(), action.getNewState());

            if (isMyAction) {
                switch (message.getMessageType()) {
                    case NORMAL: undoRedoManager.push(action); break;
                    case UNDO: undoRedoManager.applyHostUndo(); break;
                    case REDO: undoRedoManager.applyHostRedo(); break;
                }
            }

            onUpdateCallback.run();
        } catch (Exception e) {
            System.err.println("Client failed to process message: " + e.getMessage());
        }
    }
}