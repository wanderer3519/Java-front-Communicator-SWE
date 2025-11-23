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
 * <p>For this action:
 * <ul>
 * <li>`prevState` is always {@code null}.</li>
 * <li>`newState` contains the newly created shape and has `isDeleted=false`.</li>
 * </ul>
 * </p>
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
     * @param actionId  A unique ID for this action.
     * @param userId    The user performing the action.
     * @param timestamp The time of action creation.
     * @param shapeId   The target shape's ID.
     * @param newState  The state after the action (shape created, isDeleted=false).
     */
    public CreateShapeAction(final String actionId,
                             final String userId,
                             final long timestamp,
                             final ShapeId shapeId,
                             final ShapeState newState) {
        super(actionId, userId, timestamp, ActionType.CREATE, shapeId, null, newState);

        if (newState.isDeleted()) {
            throw new IllegalArgumentException("CreateShapeAction newState cannot be 'deleted'.");
        }
    }
}