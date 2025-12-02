/*
 * -----------------------------------------------------------------------------
 * File: HostActionManager.java
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
import com.swe.canvas.datamodel.serialization.NetActionSerializer;
import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.controller.ClientNode;
import com.swe.controller.RPC;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.controller.serialize.DataSerializer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The ActionManager implementation for the Host role.
 * Handles synchronization for new clients.
 */
public class HostActionManager implements ActionManager {

    /** The ID of the user (host). */
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

    /** Track which clients have already been synced to avoid redundant updates. */
    private final Set<String> syncedClients = new HashSet<>();

    /**
     * Constructs a new HostActionManager with default RPC.
     *
     * @param hostId     The unique ID of the host.
     * @param state      The shared canvas state.
     * @param netService The network service instance.
     */
    public HostActionManager(final String hostId,
                             final CanvasState state,
                             final NetworkService netService) {
        this(hostId, state, netService, null);
    }

    /**
     * Constructs a new HostActionManager with specific RPC.
     *
     * @param hostId     The unique ID of the host.
     * @param state      The shared canvas state.
     * @param netService The network service instance.
     * @param rpcObj     The RPC instance to use.
     */
    public HostActionManager(final String hostId,
                             final CanvasState state,
                             final NetworkService netService,
                             final AbstractRPC rpcObj) {
        this.userId = hostId;
        this.canvasState = state;
        this.networkService = netService;
        this.actionFactory = new ActionFactory();
        this.undoRedoManager = new UndoRedoManager();

        // Host marks themselves as synced immediately
        this.syncedClients.add(hostId);

        if (rpcObj != null) {
            this.rpc = rpcObj;
        } else {
            this.rpc = RPC.getInstance();
        }
        this.rpc.subscribe("canvas:update", this::handleUpdate);
    }

    @Override
    public void initialize() {
        // Host doesn't need to request shapes, it IS the source of truth.
        System.out.println("[HostActionManager] Initialized. Ready to serve shapes.");
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

    @Override
    public void handleUserJoined(final String clientId) {
        // Optional: Can still support push-based sync here if needed,
        // but REQUEST_SHAPES logic below handles the pull-based request.
    }

    private boolean validate(final Action action) {
        if (action.getActionType() == ActionType.CREATE) {
            return true;
        }
        final ShapeState currentState = canvasState.getShapeState(action.getShapeId());
        final ShapeState actionPrevState = action.getPrevState();
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
        } catch (final Exception e) {
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
        } catch (final Exception e) {
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
        } catch (final Exception e) {
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
        } catch (final Exception e) {
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
        } catch (final Exception e) {
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
            final Map<ShapeId, ShapeState> newMap = ShapeSerializer.deserializeShapesMap(json);
            canvasState.setAllStates(newMap);
            undoRedoManager.clear();
            final NetworkMessage restoreMsg = new NetworkMessage(MessageType.RESTORE, null, json);
            networkService.broadcastMessage(restoreMsg);
            onUpdateCallback.run();
        } catch (final Exception e) {
            System.err.println("[Host] Failed to restore map: " + e.getMessage());
        }
    }

    @Override
    public byte[] handleUpdate(final byte[] data) {
        final String dataString = new String(data, StandardCharsets.UTF_8);
        final NetworkMessage msg = NetworkMessage.deserialize(dataString);
        processIncomingMessage(msg);
        return data;
    }

    @Override
    public void processIncomingMessage(final NetworkMessage message) {
        if (message == null) {
            return;
        }

        if (message.getMessageType() == MessageType.REQUEST_SHAPES) {
            handleRequestShapes(message);
            return;
        }

        if (message.getMessageType() == MessageType.RESTORE) {
            return;
        }

        handleActionMessage(message);
    }

    private void handleRequestShapes(final NetworkMessage message) {
        try {
            if (message.getPayload() != null && !message.getPayload().isEmpty()) {
                final byte[] clientNodeBytes = message.getPayload()
                        .getBytes(StandardCharsets.UTF_8);
                final ClientNode replyTo = DataSerializer
                        .deserialize(clientNodeBytes, ClientNode.class);

                if (replyTo != null) {
                    System.out.println("[HostActionManager] Request from " + replyTo.hostName());
                    final String shapesJson = saveMap();
                    final NetworkMessage restoreMsg = new NetworkMessage(
                            MessageType.RESTORE, null, shapesJson);
                    networkService.sendToClient(restoreMsg, replyTo.hostName());
                }
            }
        } catch (final Exception e) {
            System.err.println("[HostActionManager] Shape request error: " + e.getMessage());
        }
    }

    private void handleActionMessage(final NetworkMessage message) {
        try {
            final String json = new String(message.getSerializedAction(), StandardCharsets.UTF_8);
            final Action action = NetActionSerializer.deserializeAction(json);

            if (action == null) {
                return;
            }

            if (validate(action)) {
                processValidAction(action, message);
            } else {
                System.err.println("[Host] Conflict detected. Action rejected.");
            }
            onUpdateCallback.run();
        } catch (final Exception e) {
            System.err.println("Host process message failed: " + e.getMessage());
        }
    }

    private void processValidAction(final Action action, final NetworkMessage message) {
        final boolean isHostSelfAction = action.getNewState()
                .getShape().getLastUpdatedBy().equals(userId);

        if (isHostSelfAction) {
            switch (message.getMessageType()) {
                case NORMAL -> undoRedoManager.push(action);
                case UNDO -> undoRedoManager.applyHostUndo();
                case REDO -> undoRedoManager.applyHostRedo();
                default -> {
                }
            }
        }
        applyAndBroadcast(action, message);
    }
}