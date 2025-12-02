/*
 * -----------------------------------------------------------------------------
 * File: CreateShapeAction.java
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
 * Concrete action for creating a new shape.
 *
 * <p>For this action:</p>
 * <ul>
 * <li>`prevState` is always {@code null}.</li>
 * <li>`newState` contains the newly created shape and has `isDeleted=false`.</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * <p><b>Design Pattern:</b> Command</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public class CreateShapeAction extends Action {

    /**
     * Used for Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a CreateShapeAction.
     *
     * @param actionIdVal  A unique ID for this action.
     * @param userIdVal    The user performing the action.
     * @param timestampVal The time of action creation.
     * @param shapeIdVal   The target shape's ID.
     * @param newStateVal  The state after the action (shape created, isDeleted=false).
     */
    public CreateShapeAction(final String actionIdVal,
                             final String userIdVal,
                             final long timestampVal,
                             final ShapeId shapeIdVal,
                             final ShapeState newStateVal) {
        super(actionIdVal, userIdVal, timestampVal, ActionType.CREATE, shapeIdVal, null, newStateVal);

        if (newStateVal.isDeleted()) {
            throw new IllegalArgumentException("CreateShapeAction newState cannot be 'deleted'.");
        }
    }
}