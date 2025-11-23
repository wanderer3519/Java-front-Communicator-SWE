package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.shape.Shape;

public interface ActionManager {
    ActionFactory getActionFactory();
    CanvasState getCanvasState();
    UndoRedoManager getUndoRedoManager();
    void setOnUpdate(Runnable callback);

    void requestCreate(Shape newShape);
    void requestModify(ShapeState prevState, Shape modifiedShape);
    void requestDelete(ShapeState shapeToDelete);
    void requestUndo();
    void requestRedo();

    // --- NEW: Save/Restore ---
    /**
     * Serializes the current canvas state to a JSON string.
     */
    String saveMap();

    /**
     * Restores the canvas state from a JSON string and broadcasts the update (if Host).
     */
    void restoreMap(String json);
    // ------------------------

    void processIncomingMessage(NetworkMessage message);
}