package com.swe.ux.viewmodels;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import com.swe.controller.RPC;
import com.swe.canvas.datamodel.action.ActionFactory;
import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.collaboration.NetworkMessage;
import com.swe.canvas.datamodel.manager.ActionManager;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeFactory;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.ux.canvas.util.ColorConverter;
import com.swe.ux.canvas.util.GeometryUtils;
import com.swe.ux.model.analytics.ShapeCount;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class CanvasViewModel {

    
    private final ActionManager actionManager;
    private final CanvasState canvasState;
    private final ActionFactory actionFactory;
    private final ShapeFactory shapeFactory;
    private final String userId;

    public final ObjectProperty<ToolType> activeTool = new SimpleObjectProperty<>(ToolType.FREEHAND);
    public final ObjectProperty<Color> activeColor = new SimpleObjectProperty<>(Color.BLACK);
    public final DoubleProperty activeStrokeWidth = new SimpleDoubleProperty(2.0);
    public final ObjectProperty<ShapeId> selectedShapeId = new SimpleObjectProperty<>(null);

    private final List<Point> currentPoints = new ArrayList<>();

    // Ghost Shape Logic
    private Shape transientShape = null;
    private Timer ghostTimer = new Timer(true);

    // FIX: Track the intent of the ghost (Are we waiting for it to vanish or appear?)
    private boolean isPendingDelete = false;

    private double lastDragX;
    private double lastDragY;
    public boolean isDraggingSelection = false;
    private ShapeState originalShapeForDrag = null;

    private AbstractRPC rpc;

    private static ShapeCount shapeCount = null;
    
    public CanvasViewModel(final String userId, final ActionManager actionManager) {
        this.rpc = RPC.getInstance();

        this.userId = userId;
        this.actionManager = actionManager;
        this.canvasState = actionManager.getCanvasState();
        this.actionFactory = actionManager.getActionFactory();
        this.shapeFactory = new ShapeFactory();
        shapeCount = new ShapeCount(0, 0, 0, 0, 0);

        rpc.subscribe("canvas:update", this::handleUpdate);
    }

    private byte[] handleUpdate(final byte[] data) {
        final String json = data.toString();
        final NetworkMessage message = NetworkMessage.deserialize(json);

        this.actionManager.processIncomingMessage(message);
        return new byte[0];
    }

    public CanvasState getCanvasState() {
        return canvasState;
    }

    public Shape getTransientShape() {
        return transientShape;
    }

    public void setOnCanvasUpdate(final Runnable r) {
        actionManager.setOnUpdate(r);
    }

    public void updateSelectedShapeColor(final Color newFxColor) {
        updateShapeProperty(s -> s.setColor(ColorConverter.toAwt(newFxColor)));
    }

    public void updateSelectedShapeThickness(final double newThickness) {
        updateShapeProperty(s -> s.setThickness(newThickness));
    }

    private void updateShapeProperty(final Consumer<Shape> modifier) {
        final ShapeId id = selectedShapeId.get();
        if (id != null) {
            final ShapeState currentState = canvasState.getShapeState(id);
            if (currentState != null && !currentState.isDeleted()) {
                final Shape modifiedShape = currentState.getShape().copy();
                modifier.accept(modifiedShape);

                actionManager.requestModify(currentState, modifiedShape);

                // FIX: Intent is Modify (not delete)
                this.isPendingDelete = false;
                showGhostShape(modifiedShape);
            }
        }
    }

    public void onMousePressed(final double x, final double y) {
        lastDragX = x;
        lastDragY = y;
        transientShape = null;
        isPendingDelete = false; // Reset intent

        if (activeTool.get() == ToolType.SELECT) {
            final ShapeId hitShapeId = findHitShape(x, y);
            selectedShapeId.set(hitShapeId);

            if (hitShapeId != null) {
                final ShapeState ss = canvasState.getShapeState(hitShapeId);
                if (ss != null && !ss.isDeleted() && ss.getShape() != null) {
                    isDraggingSelection = true;
                    originalShapeForDrag = ss;
                    // Start ghosting for drag
                    transientShape = ss.getShape().copy();
                    isPendingDelete = false; // Dragging is a modify operation
                } else {
                    isDraggingSelection = false;
                    originalShapeForDrag = null;
                }
            } else {
                isDraggingSelection = false;
                originalShapeForDrag = null;
            }
        } else {
            // Drawing mode
            selectedShapeId.set(null);
            currentPoints.clear();
            currentPoints.add(new Point(x, y));
            currentPoints.add(new Point(x, y));
            updateGhostShape();
        }
    }

    public void onMouseDragged(final double x, final double y) {
        if (activeTool.get() == ToolType.SELECT) {
            if (isDraggingSelection && transientShape != null && originalShapeForDrag != null) {
                final double dx = x - lastDragX;
                final double dy = y - lastDragY;
                transientShape.translate(dx, dy);
                lastDragX = x;
                lastDragY = y;
            }
        } else {
            if (activeTool.get() == ToolType.FREEHAND) {
                currentPoints.add(new Point(x, y));
            } else {
                currentPoints.set(currentPoints.size() - 1, new Point(x, y));
            }
            updateGhostShape();
        }
    }

    public void onMouseReleased(final double x, final double y) {
        if (activeTool.get() == ToolType.SELECT) {
            if (isDraggingSelection && transientShape != null && originalShapeForDrag != null) {
                actionManager.requestModify(originalShapeForDrag, transientShape);

                // FIX: Intent is Modify
                this.isPendingDelete = false;
                startGhostTimer();
            }
            isDraggingSelection = false;
            originalShapeForDrag = null;

        } else if (transientShape != null) {
            actionManager.requestCreate(transientShape);

            // Track shape creation for analytics
            updateShapeCount(transientShape.getShapeType());

            // FIX: Intent is Create
            this.isPendingDelete = false;
            startGhostTimer();
        }

        currentPoints.clear();
    }

    private void showGhostShape(final Shape shape) {
        transientShape = shape;
        startGhostTimer();
    }

    private void startGhostTimer() {
        ghostTimer.cancel();
        ghostTimer = new Timer(true);
        ghostTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                transientShape = null;
                Platform.runLater(() -> {
                    if (actionManager != null) {
                        actionManager.getCanvasState().notifyUpdate();
                    }
                });
            }
        }, 2000);
    }

    private ShapeId findHitShape(final double x, final double y) {
        final List<Shape> shapes = new ArrayList<>(canvasState.getVisibleShapes());
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (GeometryUtils.hitTest(shapes.get(i), x, y)) {
                return shapes.get(i).getShapeId();
            }
        }
        return null;
    }

    private void updateGhostShape() {
        final ShapeType type;
        switch (activeTool.get()) {
            case RECTANGLE:
                type = ShapeType.RECTANGLE;
                break;
            case ELLIPSE:
                type = ShapeType.ELLIPSE;
                break;
            case TRIANGLE:
                type = ShapeType.TRIANGLE;
                break;
            case LINE:
                type = ShapeType.LINE;
                break;
            case FREEHAND:
                type = ShapeType.FREEHAND;
                break;
            default:
                type = ShapeType.FREEHAND;
                break;
        }

        // Creating a new shape (not delete)
        this.isPendingDelete = false;

        transientShape = shapeFactory.createShape(
                type, ShapeId.randomId(), new ArrayList<>(currentPoints),
                activeStrokeWidth.get(), ColorConverter.toAwt(activeColor.get()), userId);
    }

    public void deleteSelectedShape() {
        final ShapeId id = selectedShapeId.get();
        if (id != null) {
            final ShapeState stateToDelete = canvasState.getShapeState(id);
            if (stateToDelete != null && !stateToDelete.isDeleted()) {
                actionManager.requestDelete(stateToDelete);

                // FIX: Set intent to Delete
                this.isPendingDelete = true;

                final Shape ghost = stateToDelete.getShape().copy();
                showGhostShape(ghost);
                selectedShapeId.set(null);
            }
        }
    }

    public void undo() {
        actionManager.requestUndo();
    }

    public void redo() {
        actionManager.requestRedo();
    }

    private void updateShapeCount(final ShapeType shapeType) {
        switch (shapeType) {
            case FREEHAND:
                shapeCount.incrementFreeHand();
                break;
            case LINE:
                shapeCount.incrementStraightLine();
                break;
            case RECTANGLE:
                shapeCount.incrementRectangle();
                break;
            case ELLIPSE:
                shapeCount.incrementEllipse();
                break;
            case TRIANGLE:
                shapeCount.incrementTriangle();
                break;
            default:
                break;
        }
    }

    /**
     * FIX: Revised logic to check for conflict resolution correctly. Only
     * clears the ghost if the server state matches the intent.
     */
    public void handleValidatedUpdate() {
        if (transientShape == null) {
            return;
        }

        final ShapeId tid = transientShape.getShapeId();
        if (tid == null) {
            return;
        }

        final ShapeState st = canvasState.getShapeState(tid);
        final boolean isDeletedInServer = (st == null || st.isDeleted());

        if (isPendingDelete) {
            // CASE: We are deleting.
            // We only clear the ghost if the server confirms it's GONE.
            if (isDeletedInServer) {
                transientShape = null;
                try { ghostTimer.cancel(); } catch (Exception ignored) {}
            }
            // If !isDeletedInServer, implies server hasn't processed delete yet.
            // Keep ghost (so original remains hidden by renderer).
        } else {
            // CASE: Create or Modify.
            // We clear the ghost if the server confirms it EXISTS (and implies it's updated).
            // (If the server says it's deleted, we also clear because our create/modify failed).
            if (!isDeletedInServer) {
                transientShape = null;
                try { ghostTimer.cancel(); } catch (Exception ignored) {}
            }
            else if (st == null) {
                // Special case: If we created a shape with a RandomID, but server assigned a NewID,
                // our RandomID won't exist in state. The ghost usually stays until timer,
                // OR we can choose to clear it to avoid duplication if we assume success.
                // For now, we let the timer handle the ID mismatch scenario, or clear if strict.
                // Safest approach for "Create" with ID swap is to clear ghost on *any* update
                // because the new shape (NewID) is likely already in the VisibleShapes list.
                transientShape = null;
                try { ghostTimer.cancel(); } catch (Exception ignored) {}
            }
        }
    }
}