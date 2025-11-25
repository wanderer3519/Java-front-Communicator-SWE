/*
 * -----------------------------------------------------------------------------
 * File: ClientActionManager.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.MessageType;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.serialization.NetActionSerializer;
import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import java.util.Map;

import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.controller.RPC;

/**
 * The ActionManager implementation for the Client role.
 *
 * <p>
 * Clients send their local requests to the Host for validation and
 * apply updates only when they receive the broadcasted confirmation.
 * </p>
 */
public class ClientActionManager implements ActionManager {

    /**
     * The unique identifier of the client user.
     */
    private final String userId;

    /**
     * The local state of the canvas.
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
    private Runnable onUpdateCallback = () -> {
    };

    private final AbstractRPC rpc;

    /**
     * Constructs a new ClientActionManager.
     *
     * @param clientId   The unique ID of the client user.
     * @param state      The local canvas state.
     * @param netService The network service for communicating with the host.
     */
    public ClientActionManager(final String clientId,
            final CanvasState state,
            final NetworkService netService) {
        this.userId = clientId;
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

    /**
     * Serializes and sends an action to the host.
     *
     * @param action The action to send.
     * @param type   The type of message (NORMAL, UNDO, REDO).
     */
    private void sendActionToHost(final Action action, final MessageType type) {
        try {
            final String serializedAction = NetActionSerializer.serializeAction(action);
            final NetworkMessage message = new NetworkMessage(type, serializedAction.getBytes());
            networkService.sendMessageToHost(message);
        } catch (Exception e) {
            System.err.println("Client failed to send message: " + e.getMessage());
        }
    }

    @Override
    public void requestCreate(final Shape newShape) {
        try {
            final Action action = actionFactory.createCreateAction(newShape, userId);
            sendActionToHost(action, MessageType.NORMAL);
        } catch (Exception e) {
            System.err.println("Client create request failed: " + e.getMessage());
        }
    }

    @Override
    public void requestModify(final ShapeState prevState, final Shape modifiedShape) {
        try {
            final Action action = actionFactory.createModifyAction(
                    canvasState, prevState.getShapeId(), modifiedShape, userId);
            sendActionToHost(action, MessageType.NORMAL);
        } catch (Exception e) {
            System.err.println("Client modify request failed: " + e.getMessage());
        }
    }

    @Override
    public void requestDelete(final ShapeState shapeToDelete) {
        try {
            final Action action = actionFactory.createDeleteAction(
                    canvasState, shapeToDelete.getShapeId(), userId);
            sendActionToHost(action, MessageType.NORMAL);
        } catch (Exception e) {
            System.err.println("Client delete request failed: " + e.getMessage());
        }
    }

    @Override
    public void requestUndo() {
        try {
            final Action action = undoRedoManager.getActionToUndo();
            if (action != null) {
                final Action inverse = actionFactory.createInverseAction(action, userId);
                sendActionToHost(inverse, MessageType.UNDO);
            }
        } catch (Exception e) {
            System.err.println("Client undo request failed: " + e.getMessage());
        }
    }

    @Override
    public void requestRedo() {
        try {
            final Action action = undoRedoManager.getActionToRedo();
            if (action != null) {
                sendActionToHost(action, MessageType.REDO);
            }
        } catch (Exception e) {
            System.err.println("Client redo request failed: " + e.getMessage());
        }
    }

    @Override
    public String saveMap() {
        // Return local view state
        return ShapeSerializer.serializeShapesMap(canvasState.getAllStates());
    }

    @Override
    public void restoreMap(final String json) {
        // Clients typically receive RESTORE via network, but we log if called locally
        System.out.println("[Client] Local restore requested. (No-op in typical flow)");
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
        // 1. Handle RESTORE
        if (message.getMessageType() == MessageType.RESTORE) {
            if (message.getPayload() != null) {
                try {
                    final Map<ShapeId, ShapeState> newMap = ShapeSerializer.deserializeShapesMap(message.getPayload());
                    canvasState.setAllStates(newMap);
                    undoRedoManager.clear();
                    onUpdateCallback.run();
                } catch (Exception e) {
                    System.err.println("Client restore failed: " + e.getMessage());
                }
            }
            return;
        }

        // 2. Handle Normal Actions
        try {
            // final String sa = new SerializedAction(message.getSerializedAction());
            final Action action = NetActionSerializer.deserializeAction(message.getSerializedAction().toString());
            if (action == null) {
                return;
            }

            final boolean isMyAction = action.getNewState()
                    .getShape().getLastUpdatedBy().equals(userId);

            canvasState.applyState(action.getShapeId(), action.getNewState());

            if (isMyAction) {
                switch (message.getMessageType()) {
                    case NORMAL -> undoRedoManager.push(action);
                    case UNDO -> undoRedoManager.applyHostUndo();
                    case REDO -> undoRedoManager.applyHostRedo();
                    default -> {
                    }
                }
            }
            onUpdateCallback.run();
        } catch (Exception e) {
            System.err.println("Client failed to process message: " + e.getMessage());
        }
    }
}