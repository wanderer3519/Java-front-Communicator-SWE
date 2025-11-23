package com.swe.canvas.datamodel.canvas;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;

public class CanvasState {

    private final ConcurrentMap<ShapeId, ShapeState> state;
    private Runnable onUpdateCallback = () -> {};

    public CanvasState() {
        this.state = new ConcurrentHashMap<>();
    }

    public void setOnUpdate(Runnable onUpdate) {
        if (onUpdate != null) this.onUpdateCallback = onUpdate;
        else this.onUpdateCallback = () -> {};
    }

    public void notifyUpdate() {
        this.onUpdateCallback.run();
    }

    public ShapeState getShapeState(final ShapeId shapeId) {
        return state.get(shapeId);
    }

    public void applyState(final ShapeId shapeId, final ShapeState newState) {
        Objects.requireNonNull(shapeId);
        Objects.requireNonNull(newState);
        state.put(shapeId, newState);
    }

    public Collection<Shape> getVisibleShapes() {
        return state.values().stream()
                .filter(shapeState -> !shapeState.isDeleted())
                .map(ShapeState::getShape)
                .collect(Collectors.toUnmodifiableList());
    }

    // --- NEW METHODS FOR SAVE/RESTORE ---

    /**
     * Returns a read-only view of the internal map for serialization.
     */
    public Map<ShapeId, ShapeState> getAllStates() {
        return Collections.unmodifiableMap(state);
    }

    /**
     * Replaces the entire state of the canvas with the provided map.
     * Used during restoration.
     */
    public void setAllStates(Map<ShapeId, ShapeState> newStates) {
        state.clear();
        if (newStates != null) {
            state.putAll(newStates);
        }
        notifyUpdate();
    }
    // -----------------------------------

    public void clear() {
        state.clear();
        notifyUpdate();
    }
}