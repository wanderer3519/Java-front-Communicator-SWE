/*
 * -----------------------------------------------------------------------------
 * File: DeleteShapeAction.java
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
 * Concrete action for soft-deleting a shape.
 *
 * <p>For this action:</p>
 * <ul>
 * <li>`prevState` is the state before deletion (`isDeleted=false`).</li>
 * <li>`newState` is the state after deletion (`isDeleted=true`).</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * <p><b>Design Pattern:</b> Command</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public class DeleteShapeAction extends Action {

    /**
     * Used for Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a DeleteShapeAction.
     *
     * @param actionIdVal  A unique ID for this action.
     * @param userIdVal    The user performing the action.
     * @param timestampVal The time of action creation.
     * @param shapeIdVal   The target shape's ID.
     * @param prevStateVal The state before the action (isDeleted=false).
     * @param newStateVal  The state after the action (isDeleted=true).
     */
    public DeleteShapeAction(final String actionIdVal,
                             final String userIdVal,
                             final long timestampVal,
                             final ShapeId shapeIdVal,
                             final ShapeState prevStateVal,
                             final ShapeState newStateVal) {
        super(actionIdVal, userIdVal, timestampVal, ActionType.DELETE, shapeIdVal, prevStateVal, newStateVal);

        if (prevStateVal.isDeleted()) {
            throw new IllegalArgumentException("DeleteShapeAction prevState must not be 'deleted'.");
        }
        if (!newStateVal.isDeleted()) {
            throw new IllegalArgumentException("DeleteShapeAction newState must be 'deleted'.");
        }
    }
}