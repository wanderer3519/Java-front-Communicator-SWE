/*
 * -----------------------------------------------------------------------------
 * File: ActionManager.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.shape.Shape;

/**
 * Interface defining the contract for managing canvas actions.
 */
public interface ActionManager {

    /**
     * Initializes the manager.
     * For clients, this triggers the handshake to fetch initial state from the Host.
     */
    void initialize();

    /**
     * Gets the factory used for creating action instances.
     *
     * @return The ActionFactory instance.
     */
    ActionFactory getActionFactory();

    /**
     * Gets the current state of the canvas.
     *
     * @return The CanvasState instance.
     */
    CanvasState getCanvasState();

    /**
     * Gets the manager responsible for handling undo and redo operations.
     *
     * @return The UndoRedoManager instance.
     */
    UndoRedoManager getUndoRedoManager();

    /**
     * Sets a callback to be executed when the canvas state is updated.
     *
     * @param callback The Runnable to execute on update.
     */
    void setOnUpdate(Runnable callback);

    /**
     * Requests the creation of a new shape on the canvas.
     *
     * @param newShape The shape to create.
     */
    void requestCreate(Shape newShape);

    /**
     * Requests the modification of an existing shape.
     *
     * @param prevState     The state of the shape before modification.
     * @param modifiedShape The shape with modified properties.
     */
    void requestModify(ShapeState prevState, Shape modifiedShape);

    /**
     * Requests the deletion of a shape.
     *
     * @param shapeToDelete The state of the shape to be deleted.
     */
    void requestDelete(ShapeState shapeToDelete);

    /**
     * Requests the undoing of the last action.
     */
    void requestUndo();

    /**
     * Requests the redoing of the last undone action.
     */
    void requestRedo();

    /**
     * Serializes the current canvas map (state) to a string.
     *
     * @return A JSON string representation of the canvas map.
     */
    String saveMap();

    /**
     * Restores the canvas map (state) from a serialized string.
     *
     * @param json The JSON string representation of the map.
     */
    void restoreMap(String json);

    /**
     * Processes a network message received from the communication layer.
     *
     * @param message The network message to process.
     */
    void processIncomingMessage(NetworkMessage message);

    /**
     * Handles raw data updates, typically from the controller or RPC layer.
     *
     * @param data The raw byte data of the update.
     * @return A byte array response or result.
     */
    byte[] handleUpdate(byte[] data);

    /**
     * Handles logic required when a new user joins the session.
     *
     * @param userId The ID or email of the joining user.
     */
    void handleUserJoined(String userId);
}