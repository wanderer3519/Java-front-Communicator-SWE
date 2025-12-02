/*
 * -----------------------------------------------------------------------------
 * File: UndoRedoManager.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the history of actions for Undo/Redo functionality.
 *
 * <p>This class implements a linear history stack using a list and a pointer.
 * It supports standard push, undo, and redo operations, as well as specific
 * adjustments for network synchronization (Host-driven consistency).</p>
 */
public class UndoRedoManager {

    /**
     * The list storing the history of actions.
     */
    private final List<Action> history = new ArrayList<>();

    /**
     * The index of the last applied action. -1 indicates an empty effective state.
     */
    private int currentIndex = -1;

    /**
     * Pushes a new action onto the history stack.
     *
     * <p>This operation invalidates any redo history (actions ahead of the
     * current pointer).</p>
     *
     * @param action The action to add to the history.
     */
    public synchronized void push(final Action action) {
        // Clear the "redo" future if we diverge
        while (history.size() - 1 > currentIndex) {
            history.remove(history.size() - 1);
        }
        history.add(action);
        currentIndex++;
    }

    /**
     * Retrieves the action to be undone.
     *
     * <p>This method does NOT remove the action or change the state immediately;
     * it merely returns the action that <i>would</i> be undone so the caller
     * can request an inverse operation.</p>
     *
     * @return The action at the current index, or null if history is empty.
     */
    public synchronized Action getActionToUndo() {
        if (canUndo()) {
            return history.get(currentIndex);
        }
        return null;
    }

    /**
     * Retrieves the action to be redone.
     *
     * <p>This method returns the next action in the history list (ahead of the
     * pointer) without moving the pointer.</p>
     *
     * @return The next action, or null if at the end of history.
     */
    public synchronized Action getActionToRedo() {
        if (canRedo()) {
            return history.get(currentIndex + 1);
        }
        return null;
    }

    /**
     * Moves the history pointer back by one step.
     *
     * <p>This is typically called when the Host confirms an Undo operation.</p>
     */
    public synchronized void applyHostUndo() {
        if (canUndo()) {
            currentIndex--;
        }
    }

    /**
     * Moves the history pointer forward by one step.
     *
     * <p>This is typically called when the Host confirms a Redo operation.</p>
     */
    public synchronized void applyHostRedo() {
        if (canRedo()) {
            currentIndex++;
        }
    }

    /**
     * Checks if an undo operation is possible.
     *
     * @return True if there are actions to undo.
     */
    public synchronized boolean canUndo() {
        return currentIndex > -1;
    }

    /**
     * Checks if a redo operation is possible.
     *
     * @return True if there are actions to redo.
     */
    public synchronized boolean canRedo() {
        return currentIndex < history.size() - 1;
    }

    /**
     * Clears the entire history and resets the pointer.
     */
    public synchronized void clear() {
        history.clear();
        currentIndex = -1;
    }
}