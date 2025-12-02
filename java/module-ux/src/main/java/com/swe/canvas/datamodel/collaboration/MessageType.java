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
     * A request from a new client asking the host for the current canvas state.
     */
    REQUEST_SHAPES,

    /**
     * An unknown type, used primarily for testing default/fallback paths.
     */
    UNKNOWN
}