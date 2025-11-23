/*
 * -----------------------------------------------------------------------------
 * File: ModifyShapeAction.java
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
 * Concrete action for modifying a shape's properties (points, color, etc.).
 *
 * <p>For this action:
 * <ul>
 * <li>`prevState` is the state before modification.</li>
 * <li>`newState` is the state after modification.</li>
 * <li>Both states must have `isDeleted=false`.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * <p><b>Design Pattern:</b> Command</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public class ModifyShapeAction extends Action {

    /**
     * Used for Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ModifyShapeAction.
     *
     * @param actionId  A unique ID for this action.
     * @param userId    The user performing the action.
     * @param timestamp The time of action creation.
     * @param shapeId   The target shape's ID.
     * @param prevState The state before the action.
     * @param newState  The state after the action.
     */
    public ModifyShapeAction(final String actionId,
                             final String userId,
                             final long timestamp,
                             final ShapeId shapeId,
                             final ShapeState prevState,
                             final ShapeState newState) {
        super(actionId, userId, timestamp, ActionType.MODIFY, shapeId, prevState, newState);

        if (prevState.isDeleted() || newState.isDeleted()) {
            throw new IllegalArgumentException("ModifyShapeAction cannot be performed on a deleted shape.");
        }
    }
}