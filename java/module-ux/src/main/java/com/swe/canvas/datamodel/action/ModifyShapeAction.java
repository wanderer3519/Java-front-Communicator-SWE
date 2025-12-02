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
 * <p>For this action:</p>
 * <ul>
 * <li>`prevState` is the state before modification.</li>
 * <li>`newState` is the state after modification.</li>
 * <li>Both states must have `isDeleted=false`.</li>
 * </ul>
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
     * @param actionIdVal  A unique ID for this action.
     * @param userIdVal    The user performing the action.
     * @param timestampVal The time of action creation.
     * @param shapeIdVal   The target shape's ID.
     * @param prevStateVal The state before the action.
     * @param newStateVal  The state after the action.
     */
    public ModifyShapeAction(final String actionIdVal,
                             final String userIdVal,
                             final long timestampVal,
                             final ShapeId shapeIdVal,
                             final ShapeState prevStateVal,
                             final ShapeState newStateVal) {
        super(actionIdVal, userIdVal, timestampVal, ActionType.MODIFY, shapeIdVal, prevStateVal, newStateVal);

        if (prevStateVal.isDeleted() || newStateVal.isDeleted()) {
            throw new IllegalArgumentException("ModifyShapeAction cannot be performed on a deleted shape.");
        }
    }
}