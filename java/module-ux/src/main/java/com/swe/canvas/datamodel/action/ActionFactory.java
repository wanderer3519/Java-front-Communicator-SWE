/*
 * -----------------------------------------------------------------------------
 * File: ActionFactory.java
 * Owner: Gajula Sri Siva Sai Shashank
 * Roll Number: 112201014
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import java.util.UUID;

/**
 * Factory for creating concrete {@link Action} instances.
 *
 * <p>This class is intended to be used by the UI layer. It correctly
 * captures the current {@link ShapeState} (the `prevState` Memento)
 * from the {@link CanvasState} at the moment of action creation.</p>
 *
 * <p>It also handles the creation of inverse actions for the undo/redo
 * mechanism.</p>
 *
 * <p><b>Thread Safety:</b> This class is stateless and therefore thread-safe.</p>
 *
 * <p><b>Design Pattern:</b> Factory</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public class ActionFactory {

    /**
     * Creates a new, unique action ID.
     *
     * @return A UUID string.
     */
    private String newActionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets the current system time.
     *
     * @return Time in milliseconds.
     */
    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * Creates a new action to add a shape to the canvas.
     *
     * @param newShape The fully-formed shape object to create.
     * @param userId   The user performing the action.
     * @return A new {@link CreateShapeAction}.
     */
    public Action createCreateAction(final Shape newShape, final String userId) {
        final long timestamp = now();
        newShape.setLastUpdatedBy(userId);

        final ShapeState newState = new ShapeState(newShape.copy(), false, timestamp);

        return new CreateShapeAction(
                newActionId(),
                userId,
                timestamp,
                newShape.getShapeId(),
                newState
        );
    }

    /**
     * Creates a new action to modify an existing shape.
     *
     * @param canvasState   The current state of the canvas (to get prevState).
     * @param shapeId       The ID of the shape to modify.
     * @param modifiedShape A shape object containing the *new* properties.
     * @param userId        The user performing the action.
     * @return A new {@link ModifyShapeAction}.
     * @throws IllegalStateException if the shape does not exist or is deleted.
     */
    public Action createModifyAction(final CanvasState canvasState,
                                     final ShapeId shapeId,
                                     final Shape modifiedShape,
                                     final String userId) {

        final ShapeState prevState = canvasState.getShapeState(shapeId);
        if (prevState == null || prevState.isDeleted()) {
            throw new IllegalStateException("Cannot modify non-existent or deleted shape: " + shapeId);
        }

        final long timestamp = now();

        // Create the new shape for the newState
        final Shape newShape = prevState.getShape().copy(); // Start from previous shape
        newShape.setPoints(modifiedShape.getPoints());
        newShape.setColor(modifiedShape.getColor());
        newShape.setThickness(modifiedShape.getThickness());
        newShape.setLastUpdatedBy(userId);

        final ShapeState newState = new ShapeState(newShape, false, timestamp);

        return new ModifyShapeAction(
                newActionId(),
                userId,
                timestamp,
                shapeId,
                prevState, // The state before modification
                newState   // The state after modification
        );
    }

    /**
     * Creates a new action to soft-delete a shape.
     *
     * @param canvasState The current state of the canvas (to get prevState).
     * @param shapeId     The ID of the shape to delete.
     * @param userId      The user performing the action.
     * @return A new {@link DeleteShapeAction}.
     * @throws IllegalStateException if the shape does not exist or is already deleted.
     */
    public Action createDeleteAction(final CanvasState canvasState, final ShapeId shapeId, final String userId) {
        final ShapeState prevState = canvasState.getShapeState(shapeId);
        if (prevState == null || prevState.isDeleted()) {
            throw new IllegalStateException("Cannot delete non-existent or already deleted shape: " + shapeId);
        }

        final long timestamp = now();

        // New state keeps the same shape data but flags it as deleted
        final Shape shapeCopy = prevState.getShape().copy();
        shapeCopy.setLastUpdatedBy(userId);

        final ShapeState newState = new ShapeState(shapeCopy, true, timestamp);

        return new DeleteShapeAction(
                newActionId(),
                userId,
                timestamp,
                shapeId,
                prevState, // The state before deletion
                newState   // The state after deletion
        );
    }

    /**
     * Creates an inverse action, typically for "undo".
     *
     * @param actionToUndo The action to be undone.
     * @param userId       The user performing the undo.
     * @return A new {@link Action} that reverses the effect of the input action.
     * @throws IllegalArgumentException if the action type cannot be undone.
     */
    public Action createInverseAction(final Action actionToUndo, final String userId) {
        final long timestamp = now();

        // The inverse action's "prevState" is the original action's "newState"
        // The inverse action's "newState" is the original action's "prevState"

        final ShapeState originalNewState = actionToUndo.getNewState();
        final ShapeState originalPrevState = actionToUndo.getPrevState();

        // We must update the lastUpdatedBy field and timestamp in the "new" state
        // of the inverse action.
        final ShapeState inverseNewState;
        final Shape shapeCopy;

        switch (actionToUndo.getActionType()) {
            case CREATE:
                // Inverse of Create is Delete
                // The new state for the delete must have an updated timestamp and user
                shapeCopy = originalNewState.getShape().copy();
                shapeCopy.setLastUpdatedBy(userId);
                inverseNewState = new ShapeState(shapeCopy, true, timestamp);

                return new DeleteShapeAction(
                        newActionId(), userId, timestamp,
                        actionToUndo.getShapeId(),
                        originalNewState, // PrevState (was created)
                        inverseNewState   // NewState (is now deleted)
                );

            case DELETE:
                // Inverse of Delete is Resurrect
                // The resurrected shape needs its 'lastUpdatedBy' set back
                shapeCopy = originalPrevState.getShape().copy();
                shapeCopy.setLastUpdatedBy(userId); // User who resurrected it
                inverseNewState = new ShapeState(shapeCopy, false, timestamp);

                return new ResurrectShapeAction(
                        newActionId(), userId, timestamp,
                        actionToUndo.getShapeId(),
                        originalNewState, // PrevState (was deleted)
                        inverseNewState   // NewState (is now resurrected)
                );

            case MODIFY:
                // Inverse of Modify is another Modify, swapping prev/new states
                shapeCopy = originalPrevState.getShape().copy();
                shapeCopy.setLastUpdatedBy(userId);
                inverseNewState = new ShapeState(shapeCopy, false, timestamp);

                return new ModifyShapeAction(
                        newActionId(), userId, timestamp,
                        actionToUndo.getShapeId(),
                        originalNewState, // PrevState (was modified)
                        inverseNewState   // NewState (is now back to original)
                );

            case RESURRECT:
                // Inverse of Resurrect is Delete
                shapeCopy = originalNewState.getShape().copy();
                shapeCopy.setLastUpdatedBy(userId);
                inverseNewState = new ShapeState(shapeCopy, true, timestamp);

                return new DeleteShapeAction(
                        newActionId(), userId, timestamp,
                        actionToUndo.getShapeId(),
                        originalNewState, // PrevState (was resurrected)
                        inverseNewState   // NewState (is now deleted)
                );

            default:
                throw new IllegalArgumentException("Unknown action type for undo: " + actionToUndo.getActionType());
        }
    }
}