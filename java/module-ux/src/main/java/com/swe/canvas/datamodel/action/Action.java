/*
 * -----------------------------------------------------------------------------
 * File: Action.java
 * Owner: Gajula Sri Siva Sai Shashank
 * Roll Number: 112201014
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.ShapeId;
import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base class for all operations (Commands) on the canvas.
 *
 * <p>This class implements the <b>Command Pattern</b>. Each action is a
 * self-contained object that describes a single change to the canvas state.</p>
 *
 * <p>It also incorporates the <b>Memento Pattern</b> by storing
 * {@link ShapeState} snapshots:</p>
 * <ul>
 * <li>`prevState`: The state of the shape *before* the action. Used by the
 * Host for validation and conflict detection.</li>
 * <li>`newState`: The state of the shape *after* the action. This is what
 * gets applied to the {@link com.swe.canvas.datamodel.canvas.CanvasState}
 * upon successful validation.</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is immutable and therefore thread-safe.
 * It is designed to be passed between threads and serialized over the network.</p>
 *
 * <p><b>Design Pattern:</b> Command, Memento</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public abstract class Action implements Serializable {

    /**
     * Used for Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Substring length for toString() representation.
     */
    private static final int SUBSTRING_LENGTH = 8;

    /**
     * Unique identifier for this specific action instance (e.g., a UUID).
     */
    private final String actionId;

    /**
     * The ID of the user who initiated this action.
     */
    private final String userId;

    /**
     * The timestamp of when the action was created by the client.
     */
    private final long timestamp;

    /**
     * The type of action (CREATE, MODIFY, etc.).
     */
    private final ActionType actionType;

    /**
     * The ID of the shape this action targets.
     */
    private final ShapeId shapeId;

    /**
     * The <b>Memento</b> of the state *before* this action.
     * This is {@code null} for {@link CreateShapeAction}.
     */
    private final ShapeState prevState;

    /**
     * The <b>Memento</b> of the state *after* this action.
     */
    private final ShapeState newState;

    /**
     * Constructs a new Action.
     *
     * @param actionIdVal   A unique ID for this action.
     * @param userIdVal     The user performing the action.
     * @param timestampVal  The time of action creation.
     * @param actionTypeVal The type of action.
     * @param shapeIdVal    The target shape's ID.
     * @param prevStateVal  The state before the action (null for CREATE).
     * @param newStateVal   The state after the action.
     */
    protected Action(final String actionIdVal,
                     final String userIdVal,
                     final long timestampVal,
                     final ActionType actionTypeVal,
                     final ShapeId shapeIdVal,
                     final ShapeState prevStateVal,
                     final ShapeState newStateVal) {
        this.actionId = Objects.requireNonNull(actionIdVal, "actionId cannot be null");
        this.userId = Objects.requireNonNull(userIdVal, "userId cannot be null");
        this.timestamp = timestampVal;
        this.actionType = Objects.requireNonNull(actionTypeVal, "actionType cannot be null");
        this.shapeId = Objects.requireNonNull(shapeIdVal, "shapeId cannot be null");
        // prevState can be null (for CREATE), but newState cannot
        this.prevState = prevStateVal;
        this.newState = Objects.requireNonNull(newStateVal, "newState cannot be null");
    }

    // --- Getters ---

    /**
     * Gets the unique action ID.
     *
     * @return The unique action ID.
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * Gets the ID of the user who initiated the action.
     *
     * @return The ID of the user who initiated the action.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return The creation timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the {@link ActionType}.
     *
     * @return The {@link ActionType}.
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * Gets the {@link ShapeId} of the target shape.
     *
     * @return The {@link ShapeId} of the target shape.
     */
    public ShapeId getShapeId() {
        return shapeId;
    }

    /**
     * Gets the {@link ShapeState} before the action.
     *
     * @return The {@link ShapeState} before the action (the "prevState" Memento).
     */
    public ShapeState getPrevState() {
        return prevState;
    }

    /**
     * Gets the {@link ShapeState} after the action.
     *
     * @return The {@link ShapeState} after the action (the "newState" Memento).
     */
    public ShapeState getNewState() {
        return newState;
    }

    @Override
    public String toString() {
        return String.format("%s[actionId=%s..., user=%s, shapeId=%s...]",
                actionType,
                actionId.substring(0, SUBSTRING_LENGTH),
                userId,
                shapeId.getValue().substring(0, SUBSTRING_LENGTH)
        );
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Action action = (Action) obj;
        // Two actions are identical if their actionId is the same.
        return actionId.equals(action.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId);
    }
}