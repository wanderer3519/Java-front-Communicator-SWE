/**
 * Provides classes for managing the concurrent state of the collaborative canvas.
 *
 * <p>This package includes:
 * <ul>
 * <li>{@link com.swe.canvas.datamodel.canvas.CanvasState}: The main thread-safe
 * state container, using a ConcurrentHashMap to store shape states.</li>
 * <li>{@link com.swe.canvas.datamodel.canvas.ShapeState}: A wrapper object that
 * represents the complete state of a single shape, including its deletion
 * status and timestamp. This acts as the Memento.</li>
 * </ul>
 * </p>
 *
 * @author Darla Manohar
 */
package com.swe.canvas.datamodel.canvas;