/*
 * -----------------------------------------------------------------------------
 * File: CanvasState.java
 * Owner: Darla Manohar
 * Roll Number: 112201034
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.canvas;

import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Represents the authoritative state of the entire canvas.
 *
 * <p>This class maintains a thread-safe map of {@link ShapeId} to
 * {@link ShapeState}. It serves as the "Single Source of Truth" for the
 * application state on both Host and Client.</p>
 *
 * <p>It supports notifying listeners (e.g., the UI) when the state changes.</p>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. It uses a
 * {@link ConcurrentMap} for storage.</p>
 *
 * @author Darla Manohar
 */
public class CanvasState {

    /**
     * Thread-safe storage for all shape states.
     */
    private final ConcurrentMap<ShapeId, ShapeState> state;

    /**
     * Callback to invoke when the state changes.
     */
    private Runnable onUpdateCallback = () -> { };

    /**
     * Constructs a new, empty CanvasState.
     */
    public CanvasState() {
        this.state = new ConcurrentHashMap<>();
    }

    /**
     * Sets the callback to be run whenever the canvas state is modified.
     *
     * @param onUpdate The runnable to execute on update. If null, a no-op is set.
     */
    public void setOnUpdate(final Runnable onUpdate) {
        if (onUpdate != null) {
            this.onUpdateCallback = onUpdate;
        } else {
            this.onUpdateCallback = () -> { };
        }
    }

    /**
     * Manually triggers the update callback.
     * Useful if a complex operation finishes and UI needs a refresh.
     */
    public void notifyUpdate() {
        this.onUpdateCallback.run();
    }

    /**
     * Retrieves the state of a specific shape.
     *
     * @param shapeId The ID of the shape to retrieve.
     * @return The {@link ShapeState}, or null if not found.
     */
    public ShapeState getShapeState(final ShapeId shapeId) {
        return state.get(shapeId);
    }

    /**
     * Applies a new state for a specific shape.
     *
     * <p>This inserts or updates the state in the map.</p>
     *
     * @param shapeId  The ID of the shape.
     * @param newState The new state to apply.
     * @throws NullPointerException if shapeId or newState is null.
     */
    public void applyState(final ShapeId shapeId, final ShapeState newState) {
        Objects.requireNonNull(shapeId, "shapeId cannot be null");
        Objects.requireNonNull(newState, "newState cannot be null");
        state.put(shapeId, newState);
    }

    /**
     * Retrieves a collection of all shapes that are currently visible (not deleted).
     *
     * @return An unmodifiable list of visible {@link Shape} objects.
     */
    public Collection<Shape> getVisibleShapes() {
        return state.values().stream()
                .filter(shapeState -> !shapeState.isDeleted())
                .map(ShapeState::getShape)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns an unmodifiable view of all tracked shape states.
     *
     * @return collection of shape states
     */
    public Collection<ShapeState> getShapeStates() {
        return Collections.unmodifiableCollection(state.values());
    }

    // --- NEW METHODS FOR SAVE/RESTORE ---

    /**
     * Returns a read-only view of the internal map for serialization.
     *
     * @return An unmodifiable map of all shape states.
     */
    public Map<ShapeId, ShapeState> getAllStates() {
        return Collections.unmodifiableMap(state);
    }

    /**
     * Replaces the entire state of the canvas with the provided map.
     * Used during restoration.
     *
     * @param newStates The map of new states to set. If null, clears the canvas.
     */
    public void setAllStates(final Map<ShapeId, ShapeState> newStates) {
        state.clear();
        if (newStates != null) {
            state.putAll(newStates);
        }
        notifyUpdate();
    }

    /**
     * Clears the entire canvas state.
     */
    public void clear() {
        state.clear();
        notifyUpdate();
    }
}