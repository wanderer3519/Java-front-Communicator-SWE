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
import com.swe.controller.ClientNode;
import com.swe.controller.RPC;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.controller.serialize.DataSerializer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The ActionManager implementation for the Client role.
 */
public class ClientActionManager implements ActionManager {

    /** The ID of the user (client). */
    private final String userId;

    /** The shared canvas state. */
    private final CanvasState canvasState;

    /** Factory for creating actions. */
    private final ActionFactory actionFactory;

    /** Manager for handling undo/redo stacks. */
    private final UndoRedoManager undoRedoManager;

    /** Service for network communication. */
    private final NetworkService networkService;

    /** Callback to execute on state updates. */
    private Runnable onUpdateCallback = () -> {
    };

    /** RPC interface for communication. */
    private final AbstractRPC rpc;

    /**
     * Constructs a new ClientActionManager with default RPC.
     *
     * @param clientId   The unique ID of the client.
     * @param state      The shared canvas state.
     * @param netService The network service instance.
     */
    public ClientActionManager(final String clientId,
                               final CanvasState state,
                               final NetworkService netService) {
        this(clientId, state, netService, null);
    }

    /**
     * Constructs a new ClientActionManager with specific RPC.
     *
     * @param clientId   The unique ID of the client.
     * @param state      The shared canvas state.
     * @param netService The network service instance.
     * @param rpcParam   The RPC instance to use.
     */
    public ClientActionManager(final String clientId,
                               final CanvasState state,
                               final NetworkService netService,
                               final AbstractRPC rpcParam) {
        this.userId = clientId;
        this.canvasState = state;
        this.networkService = netService;
        this.actionFactory = new ActionFactory();
        this.undoRedoManager = new UndoRedoManager();

        if (rpcParam != null) {
            this.rpc = rpcParam;
        } else {
            this.rpc = RPC.getInstance();
        }
        this.rpc.subscribe("canvas:update", this::handleUpdate);
    }

    /**
     * Initializes the client by requesting identity and then syncing history.
     * 1. Call "canvas:whoami" to get ClientNode.
     * 2. Send REQUEST_SHAPES with ClientNode payload to Host.
     */
    @Override
    public void initialize() {
        System.out.println("[ClientActionManager] Initializing... requesting whoami.");

        CompletableFuture.runAsync(() -> {
            try {
                // 1. Request identity
                final byte[] whoAmIResponse = rpc.call("canvas:whoami", new byte[0]).get();
                if (whoAmIResponse == null || whoAmIResponse.length == 0) {
                    System.err.println("[ClientActionManager] Failed to get identity from canvas:whoami");
                    return;
                }

                // 2. Deserialize ClientNode
                final ClientNode myClientNode = DataSerializer.deserialize(whoAmIResponse, ClientNode.class);
                if (myClientNode == null) {
                    System.err.println("[ClientActionManager] Deserialized ClientNode is null.");
                    return;
                }

                // 3. Prepare Payload (Serialize ClientNode to JSON string)
                final byte[] payloadBytes = DataSerializer.serialize(myClientNode);
                final String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

                // 4. Create Network Message
                final NetworkMessage requestMsg = new NetworkMessage(MessageType.REQUEST_SHAPES, null, payloadJson);

                // 5. Send to Host
                System.out.println("[ClientActionManager] Sending REQUEST_SHAPES to Host.");
                networkService.sendMessageToHost(requestMsg);

            } catch (final Exception ex) {
                System.err.println("[ClientActionManager] Initialization failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
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

    private void sendActionToHost(final Action action, final MessageType type) {
        try {
            final String serializedAction = NetActionSerializer.serializeAction(action);
            final NetworkMessage message = new NetworkMessage(type, serializedAction.getBytes());
            networkService.sendMessageToHost(message);
        } catch (final Exception e) {
            System.err.println("Client failed to send message: " + e.getMessage());
        }
    }

    @Override
    public void requestCreate(final Shape newShape) {
        try {
            final Action action = actionFactory.createCreateAction(newShape, userId);
            sendActionToHost(action, MessageType.NORMAL);
        } catch (final Exception e) {
            System.err.println("Client create request failed: " + e.getMessage());
        }
    }

    @Override
    public void requestModify(final ShapeState prevState, final Shape modifiedShape) {
        try {
            final Action action = actionFactory.createModifyAction(
                    canvasState, prevState.getShapeId(), modifiedShape, userId);
            sendActionToHost(action, MessageType.NORMAL);
        } catch (final Exception e) {
            System.err.println("Client modify request failed: " + e.getMessage());
        }
    }

    @Override
    public void requestDelete(final ShapeState shapeToDelete) {
        try {
            final Action action = actionFactory.createDeleteAction(
                    canvasState, shapeToDelete.getShapeId(), userId);
            sendActionToHost(action, MessageType.NORMAL);
        } catch (final Exception e) {
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
        } catch (final Exception e) {
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
        } catch (final Exception e) {
            System.err.println("Client redo request failed: " + e.getMessage());
        }
    }

    @Override
    public String saveMap() {
        return ShapeSerializer.serializeShapesMap(canvasState.getAllStates());
    }

    @Override
    public void restoreMap(final String json) {
        System.out.println("[Client] Local restore requested. (No-op in typical flow)");
    }

    @Override
    public byte[] handleUpdate(final byte[] data) {
        final String dataString = new String(data, StandardCharsets.UTF_8);
        final NetworkMessage msg = NetworkMessage.deserialize(dataString);
        processIncomingMessage(msg);
        return data;
    }

    @Override
    public void handleUserJoined(final String joiningUserId) {
        // Client does nothing when other users join.
    }

    @Override
    public void processIncomingMessage(final NetworkMessage message) {
        if (message == null) {
            return;
        }

        if (message.getMessageType() == MessageType.RESTORE) {
            handleRestoreMessage(message);
        } else {
            handleActionMessage(message);
        }
    }

    private void handleRestoreMessage(final NetworkMessage message) {
        if (message.getPayload() != null) {
            try {
                System.out.println("[Client] Received RESTORE/SYNC from Host.");
                final Map<ShapeId, ShapeState> newMap = ShapeSerializer
                        .deserializeShapesMap(message.getPayload());
                canvasState.setAllStates(newMap);
                undoRedoManager.clear();
                onUpdateCallback.run();
            } catch (final Exception e) {
                System.err.println("Client restore failed: " + e.getMessage());
            }
        }
    }

    private void handleActionMessage(final NetworkMessage message) {
        try {
            final String json = new String(message.getSerializedAction(), StandardCharsets.UTF_8);
            final Action action = NetActionSerializer.deserializeAction(json);

            if (action == null) {
                return;
            }

            final boolean isMyAction = action.getNewState()
                    .getShape().getLastUpdatedBy().equals(userId);

            canvasState.applyState(action.getShapeId(), action.getNewState());

            if (isMyAction) {
                updateUndoRedoStack(message.getMessageType(), action);
            }
            onUpdateCallback.run();
        } catch (final Exception e) {
            System.err.println("Client failed to process message: " + e.getMessage());
        }
    }

    private void updateUndoRedoStack(final MessageType type, final Action action) {
        switch (type) {
            case NORMAL -> undoRedoManager.push(action);
            case UNDO -> undoRedoManager.applyHostUndo();
            case REDO -> undoRedoManager.applyHostRedo();
            default -> {
            }
        }
    }
}