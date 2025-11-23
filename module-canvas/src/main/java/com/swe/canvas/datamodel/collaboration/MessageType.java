package com.swe.canvas.datamodel.collaboration;

/**
 * Defines the type of action being sent over the network.
 */
public enum MessageType {
    /**
     * A standard create, modify, or delete action.
     */
    NORMAL,

    /**
     * An undo request.
     */
    UNDO,

    /**
     * A redo request.
     */
    REDO,

    /**
     * A restore (full state reset) request.
     */
    RESTORE
}