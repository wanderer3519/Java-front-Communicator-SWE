/*
 * -----------------------------------------------------------------------------
 * File: ResurrectShapeAction.java
 * Owner: Gajula Sri Siva Sai Shashank
 * Roll Number: 112201014
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.ShapeId;

/**
 * Concrete action for un-deleting a shape (undoing a delete).
 *
 * <p>For this action:
 * <ul>
 * <li>`prevState` is the deleted state (`isDeleted=true`).</li>
 * <li>`newState` is the restored state (`isDeleted=false`).</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * <p><b>Design Pattern:</b> Command</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public class ResurrectShapeAction extends Action {

    /**
     * Used for Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ResurrectShapeAction.
     *
     * @param actionId  A unique ID for this action.
     * @param userId    The user performing the action.
     * @param timestamp The time of action creation.
     * @param shapeId   The target shape's ID.
     * @param prevState The state before the action (isDeleted=true).
     * @param newState  The state after the action (isDeleted=false).
     */
    public ResurrectShapeAction(final String actionId,
                                final String userId,
                                final long timestamp,
                                final ShapeId shapeId,
                                final ShapeState prevState,
                                final ShapeState newState) {
        super(actionId, userId, timestamp, ActionType.RESURRECT, shapeId, prevState, newState);

        if (!prevState.isDeleted()) {
            throw new IllegalArgumentException("ResurrectShapeAction prevState must be 'deleted'.");
        }
        if (newState.isDeleted()) {
            throw new IllegalArgumentException("ResurrectShapeAction newState must not be 'deleted'.");
        }
    }
}