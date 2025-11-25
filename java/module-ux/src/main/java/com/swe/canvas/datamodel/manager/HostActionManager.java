/*
 * -----------------------------------------------------------------------------
 * File: HostActionManager.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

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
import com.swe.canvas.datamodel.serialization.NetActionSerializer;

import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;

import com.swe.controller.RPC;
import com.swe.controller.RPCinterface.AbstractRPC;

/**
 * The ActionManager implementation for the Host role.
 *
 * <p>
 * The Host is the source of truth. It validates incoming actions against
 * the current state, manages the central Undo/Redo stack, and broadcasts
 * confirmed actions to all clients.
 * </p>
 */
public class HostActionManager implements ActionManager {

    /**
     * The unique identifier of the host user.
     */
    private final String userId;

    /**
     * The central state of the canvas managed by the host.
     */
    private final CanvasState canvasState;

    /**
     * Factory for creating action objects.
     */
    private final ActionFactory actionFactory;

    /**
     * Manager for the undo/redo history stack.
     */
    private final UndoRedoManager undoRedoManager;

    /**
     * Service for network communication.
     */
    private final NetworkService networkService;
    
    /**
     * Callback to run when the state updates.
     */
    private Runnable onUpdateCallback = () -> { };

    private AbstractRPC rpc;

    /**
     * Constructs a new HostActionManager.
     *
     * @param hostId     The unique ID of the host user.
     * @param state      The shared canvas state.
     * @param netService The network service for broadcasting.
     */
    public HostActionManager(final String hostId,
                             final CanvasState state,
                             final NetworkService netService) {
        this.userId = hostId;
        this.canvasState = state;
        this.networkService = netService;
        this.actionFactory = new ActionFactory();
        this.undoRedoManager = new UndoRedoManager();

        this.rpc = RPC.getInstance();
        this.rpc.subscribe("canvas:update", this::handleUpdate);
    }

    @Override
    public ActionFactory getActionFactory() {
        return actionFactory;
    }

    @Override
    public CanvasState getCanvasState() {
        return canvasState;
    }

    @Override
    public UndoRedoManager getUndoRedoManager() {
        return undoRedoManager;
    }

    @Override
    public void setOnUpdate(final Runnable callback) {
        if (callback != null) {
            this.onUpdateCallback = callback;
        }
    }

    private boolean validate(final Action action) {
        // CREATE actions are always valid if the ID is unique (UUIDs assumed unique)
        if (action.getActionType() == ActionType.CREATE) {
            return true;
        }

        // For Modify/Delete, ensure the client's view of "PrevState" matches the Host's
        // current state.
        final ShapeState currentState = canvasState.getShapeState(action.getShapeId());
        final ShapeState actionPrevState = action.getPrevState();

        // Use Objects.equals to handle potential nulls safely
        return Objects.equals(currentState, actionPrevState);
    }

    private void applyAndBroadcast(final Action action, final NetworkMessage originalMessage) {
        canvasState.applyState(action.getShapeId(), action.getNewState());
        networkService.broadcastMessage(originalMessage);
    }

    @Override
    public void requestCreate(final Shape newShape) {
        try {
            final Action action = actionFactory.createCreateAction(newShape, userId);
            final String sa = NetActionSerializer.serializeAction(action);
            processIncomingMessage(new NetworkMessage(MessageType.NORMAL, sa.getBytes()));
        } catch (Exception e) {
            System.err.println("Host failed to create shape: " + e.getMessage());
        }
    }

    @Override
    public void requestModify(final ShapeState prevState, final Shape modifiedShape) {
        try {
            final Action action = actionFactory.createModifyAction(
                    canvasState, prevState.getShapeId(), modifiedShape, userId);
            final String sa = NetActionSerializer.serializeAction(action);
            processIncomingMessage(new NetworkMessage(MessageType.NORMAL, sa.getBytes()));
        } catch (Exception e) {
            System.err.println("Host failed to modify shape: " + e.getMessage());
        }
    }

    @Override
    public void requestDelete(final ShapeState shapeToDelete) {
        try {
            final Action action = actionFactory.createDeleteAction(
                    canvasState, shapeToDelete.getShapeId(), userId);
            final String sa = NetActionSerializer.serializeAction(action);
            processIncomingMessage(new NetworkMessage(MessageType.NORMAL, sa.getBytes()));
        } catch (Exception e) {
            System.err.println("Host failed to delete shape: " + e.getMessage());
        }
    }

    @Override
    public void requestUndo() {
        try {
            final Action actionToUndo = undoRedoManager.getActionToUndo();
            if (actionToUndo != null) {
                final Action inverse = actionFactory.createInverseAction(actionToUndo, userId);
                final String sa = NetActionSerializer.serializeAction(inverse);
                processIncomingMessage(new NetworkMessage(MessageType.UNDO, sa.getBytes()));
            }
        } catch (Exception e) {
            System.err.println("Host failed to process undo: " + e.getMessage());
        }
    }

    @Override
    public void requestRedo() {
        try {
            final Action actionToRedo = undoRedoManager.getActionToRedo();
            if (actionToRedo != null) {
                final String sa = NetActionSerializer.serializeAction(actionToRedo);
                processIncomingMessage(new NetworkMessage(MessageType.REDO, sa.getBytes()));
            }
        } catch (Exception e) {
            System.err.println("Host failed to process redo: " + e.getMessage());
        }
    }

    @Override
    public String saveMap() {
        return ShapeSerializer.serializeShapesMap(canvasState.getAllStates());
    }

    @Override
    public void restoreMap(final String json) {
        try {
            // 1. Deserialize locally
            final Map<ShapeId, ShapeState> newMap = ShapeSerializer.deserializeShapesMap(json);

            // 2. Apply locally
            canvasState.setAllStates(newMap);
            undoRedoManager.clear();

            // 3. Broadcast RESTORE message to clients
            final NetworkMessage restoreMsg = new NetworkMessage(MessageType.RESTORE, null, json);
            networkService.broadcastMessage(restoreMsg);

            onUpdateCallback.run();
        } catch (Exception e) {
            System.err.println("[Host] Failed to restore map: " + e.getMessage());
        }
    }

    private byte[] handleUpdate(final byte[] data) {
        // Placeholder for handling updates via RPC if needed
        String dataString = data.toString();
        NetworkMessage msg = NetworkMessage.deserialize(dataString);
        
        processIncomingMessage(msg);

        return data;
    }

    @Override
    public void processIncomingMessage(final NetworkMessage message) {
        // Host ignores incoming RESTORE (it originates them)
        if (message.getMessageType() == MessageType.RESTORE) {
            return;
        }

        try {
            final Action action = NetActionSerializer.deserializeAction(message.getSerializedAction().toString());

            if (action == null) {
                return;
            }

            final boolean isHostSelfAction = action.getNewState()
                    .getShape().getLastUpdatedBy().equals(userId);

            if (validate(action)) {
                if (isHostSelfAction) {
                    switch (message.getMessageType()) {
                        case NORMAL -> undoRedoManager.push(action);
                        case UNDO -> undoRedoManager.applyHostUndo();
                        case REDO -> undoRedoManager.applyHostRedo();
                        default -> { }
                    }
                }
                applyAndBroadcast(action, message);
            } else {
                System.err.println("[Host] Conflict detected. Action rejected.");
            }
            onUpdateCallback.run();
        } catch (Exception e) {
            System.err.println("Host process message failed: " + e.getMessage());
        }
    }
}