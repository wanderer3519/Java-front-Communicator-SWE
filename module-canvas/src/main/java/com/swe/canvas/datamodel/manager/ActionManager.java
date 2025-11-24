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
 *
 * <p>This interface abstracts the differences between Host and Client behaviors
 * regarding action creation, application, and network propagation.</p>
 */
public interface ActionManager {

    /**
     * Gets the factory used to create actions.
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
     * Gets the manager responsible for Undo/Redo history.
     *
     * @return The UndoRedoManager instance.
     */
    UndoRedoManager getUndoRedoManager();

    /**
     * Sets a callback to be executed whenever the canvas state changes.
     *
     * @param callback The Runnable to execute on update.
     */
    void setOnUpdate(Runnable callback);

    /**
     * Requests the creation of a new shape.
     *
     * @param newShape The shape object to create.
     */
    void requestCreate(Shape newShape);

    /**
     * Requests the modification of an existing shape.
     *
     * @param prevState     The state of the shape before modification.
     * @param modifiedShape The shape object containing the new properties.
     */
    void requestModify(ShapeState prevState, Shape modifiedShape);

    /**
     * Requests the deletion of a shape.
     *
     * @param shapeToDelete The state of the shape to delete.
     */
    void requestDelete(ShapeState shapeToDelete);

    /**
     * Requests to undo the last action.
     */
    void requestUndo();

    /**
     * Requests to redo the last undone action.
     */
    void requestRedo();

    /**
     * Serializes the current canvas state to a JSON string.
     *
     * @return A JSON string representing the entire canvas map.
     */
    String saveMap();

    /**
     * Restores the canvas state from a JSON string.
     *
     * @param json The JSON string to restore from.
     */
    void restoreMap(String json);

    /**
     * Processes a message received from the network layer.
     *
     * @param message The network message containing the action or command.
     */
    void processIncomingMessage(NetworkMessage message);
}