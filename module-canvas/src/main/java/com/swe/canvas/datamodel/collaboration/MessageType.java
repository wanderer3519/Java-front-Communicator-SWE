/*
 * -----------------------------------------------------------------------------
 * File: MessageType.java
 * Owner: B S S Krishna
 * Roll Number: 112201013
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.collaboration;

/**
 * Defines the type of action being sent over the network.
 *
 * <p>This enum categorizes the payload within a {@link NetworkMessage},
 * instructing the receiver on how to process the data (e.g., as a
 * modification to the canvas, an undo command, or a full state restoration).</p>
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
    RESTORE,

    /**
     * An unknown type, used primarily for testing default/fallback paths.
     */
    UNKNOWN
}