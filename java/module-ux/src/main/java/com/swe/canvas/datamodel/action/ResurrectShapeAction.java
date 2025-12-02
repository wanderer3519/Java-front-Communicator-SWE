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
 * <p>For this action:</p>
 * <ul>
 * <li>`prevState` is the deleted state (`isDeleted=true`).</li>
 * <li>`newState` is the restored state (`isDeleted=false`).</li>
 * </ul>
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
     * @param actionIdVal  A unique ID for this action.
     * @param userIdVal    The user performing the action.
     * @param timestampVal The time of action creation.
     * @param shapeIdVal   The target shape's ID.
     * @param prevStateVal The state before the action (isDeleted=true).
     * @param newStateVal  The state after the action (isDeleted=false).
     */
    public ResurrectShapeAction(final String actionIdVal,
                                final String userIdVal,
                                final long timestampVal,
                                final ShapeId shapeIdVal,
                                final ShapeState prevStateVal,
                                final ShapeState newStateVal) {
        super(actionIdVal, userIdVal, timestampVal, ActionType.RESURRECT, shapeIdVal, prevStateVal, newStateVal);

        if (!prevStateVal.isDeleted()) {
            throw new IllegalArgumentException("ResurrectShapeAction prevState must be 'deleted'.");
        }
        if (newStateVal.isDeleted()) {
            throw new IllegalArgumentException("ResurrectShapeAction newState must not be 'deleted'.");
        }
    }
}