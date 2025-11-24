/**
 * Implements the core logic for action processing, distributed consistency, and undo/redo.
 *
 * <p>This package is the "brain" of the data model and uses several
 * design patterns:
 * <ul>
 * <li><b>Strategy Pattern:</b> {@link com.swe.canvas.datamodel.manager.ActionManager}
 * is an interface implemented by {@link com.swe.canvas.datamodel.manager.HostActionManagerTest}
 * and {@link com.swe.canvas.datamodel.manager.ParticipantActionManager}. The system's
 * behavior for processing actions changes based on which strategy (i.e., role)
 * is active.</li>
 * <li><b>Template Method Pattern:</b> An abstract base class could be used
 * to define the skeleton of action processing (deserialize -> validate -> apply),
 * while letting subclasses (Host/Participant) override the specific
 * `validate` and `onSuccess` steps. (Implemented here via the interface).</li>
 * </ul>
 * </p>
 *
 * <p>It also includes:
 * <ul>
 * <li>{@link com.swe.canvas.datamodel.manager.UndoRedoStack}: A thread-safe
 * manager for local undo/redo stacks.</li>
 * </ul>
 * </p>
 *
 * @author Gajjala Bhavani Shankar
 
 
 */
package com.swe.canvas.datamodel.manager;