/*
 * -----------------------------------------------------------------------------
 * File: ActionType.java
 * Owner: Gajula Sri Siva Sai Shashank
 * Roll Number: 112201014
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.action;

/**
 * Enumerates the types of actions that can be performed on the canvas.
 *
 * <p>Used for serialization and by the {@link ActionFactory} and
 * {@link com.swe.canvas.datamodel.manager.ActionManager}.
 * </p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public enum ActionType {
    /**
     * An action that creates a new shape.
     */
    CREATE,

    /**
     * An action that modifies an existing shape.
     */
    MODIFY,

    /**
     * An action that soft-deletes an existing shape.
     */
    DELETE,

    /**
     * An action that un-deletes a soft-deleted shape (used for undo).
     */
    RESURRECT,

    /**
     * An unknown action type, primarily used for testing unreachable code paths/defaults.
     */
    UNKNOWN
}