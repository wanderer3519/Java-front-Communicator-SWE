package com.swe.canvas.ui;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.manager.ActionManager;
import com.swe.canvas.mvvm.CanvasViewModel; // CHANGED
import com.swe.canvas.mvvm.ToolType;
import com.swe.canvas.ui.util.ColorConverter;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Controller for the fxml view.
 * Now includes Pan and Zoom logic, and passes transient shape to renderer.
 * It is initialized with an IActionManager (Host or Client) by the Main app.
 */
public class CanvasController {

    // --- FXML Control References ---
    @FXML private ToggleButton selectBtn;
    @FXML private ToggleButton freehandBtn;
    @FXML private ToggleButton rectBtn;
    @FXML private ToggleButton ellipseBtn;
    @FXML private ToggleButton lineBtn;
    @FXML private ToggleButton triangleBtn;
    @FXML private Button captureBtn;
    @FXML private Slider sizeSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Button deleteBtn;
    @FXML private Button regularizeBtn;
    @FXML private Button undoBtn;
    @FXML private Button redoBtn;
    @FXML private Canvas canvas;
    @FXML private StackPane canvasContainer; 
    @FXML private StackPane canvasHolder; 

    private CanvasViewModel viewModel;
    private ActionManager actionManager; // To set the callback
    private CanvasRenderer renderer;
    private boolean isUpdatingUI = false;

    // --- Pan and Zoom State ---
    private Translate canvasTranslate;
    private Scale canvasScale;
    private boolean isPanning = false;
    private double panStartX, panStartY;
    
    private static final double ZOOM_FACTOR = 1.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double MIN_ZOOM = 0.5;

    /**
     * This method must be called by the Main app to inject the
     * correct ActionManager (Host or Client).
     */
    public void initModel(ActionManager manager) {
        this.actionManager = manager;
        // Generate a simple unique-enough user ID
        this.viewModel = new CanvasViewModel("user-" + System.nanoTime() % 10000, manager);
        
        // Now that VM is created, finish initialization
        initializeControls();
    }

    /**
     * Initializes the view/GUI
     */
    @FXML
    public void initialize() {
        // We can't create the ViewModel here anymore,
        // because it needs the IActionManager to be injected.
        // The logic is moved to initModel() and initializeControls().
    }

    /**
     * This logic is now run *after* initModel is called.
     */
    private void initializeControls() {
        renderer = new CanvasRenderer(canvas);

        // --- Setup Transforms for Pan and Zoom ---
        canvasTranslate = new Translate();
        canvasScale = new Scale();
        canvasHolder.getTransforms().addAll(canvasTranslate, canvasScale);

        // --- Initialize Controls ---
        sizeSlider.setValue(viewModel.activeStrokeWidth.get());
        colorPicker.setValue(viewModel.activeColor.get());

        // --- Bindings & Listeners ---
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingUI) return;
            final double thickness = newVal.doubleValue();
            viewModel.activeStrokeWidth.set(thickness);
            if (viewModel.selectedShapeId.get() != null) {
                viewModel.updateSelectedShapeThickness(thickness);
            }
        });

        deleteBtn.disableProperty().bind(viewModel.selectedShapeId.isNull());
        regularizeBtn.disableProperty().bind(viewModel.selectedShapeId.isNull());

        viewModel.selectedShapeId.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ShapeState state = viewModel.getCanvasState().getShapeState(newVal);
                if (state != null && !state.isDeleted()) {
                    isUpdatingUI = true;
                    try {
                        colorPicker.setValue(ColorConverter.toFx(state.getShape().getColor()));
                        sizeSlider.setValue(state.getShape().getThickness());
                        viewModel.activeColor.set(colorPicker.getValue());
                        viewModel.activeStrokeWidth.set(sizeSlider.getValue());
                    } finally {
                        isUpdatingUI = false;
                    }
                }
            }
            redraw();
        });

        // --- Standard Setup ---
        canvas.setFocusTraversable(true);
        canvasContainer.setOnMouseClicked(e -> canvas.requestFocus());
        canvasContainer.setOnKeyPressed(this::onKeyPressed);

        // Set the redraw callback
        // this.actionManager.setOnUpdate(this::redraw);
        // FIX: Ensure both logic update and redraw happen on the UI thread sequentially
        this.actionManager.setOnUpdate(() -> {
            Platform.runLater(() -> {
                if (viewModel != null) {
                    viewModel.handleValidatedUpdate();
                }
                redraw();
            });
        });


        // Set UserData for tool selection
        freehandBtn.setUserData(ToolType.FREEHAND);
        selectBtn.setUserData(ToolType.SELECT);
        rectBtn.setUserData(ToolType.RECTANGLE);
        ellipseBtn.setUserData(ToolType.ELLIPSE);
        lineBtn.setUserData(ToolType.LINE);
        triangleBtn.setUserData(ToolType.TRIANGLE);
        
        redraw();
    }

    private void redraw() {
        // We must run this on the JavaFX Application Thread
        // to prevent concurrency issues from network callbacks
        Platform.runLater(() -> {
            if (renderer != null && viewModel != null) {
                // Pass the transient/ghost shape to the renderer
                renderer.render(viewModel.getCanvasState(), viewModel.getTransientShape(), viewModel.selectedShapeId.get(), viewModel.isDraggingSelection);
            }
        });
    }

    
    // =========================================================================
    // --- Pan and Zoom Event Handlers (Attached to canvasContainer) ---
    // =========================================================================
    @FXML
    private void onScroll(ScrollEvent event) {
        event.consume();
        double delta = event.getDeltaY();
        if (delta == 0) return;
        double zoomFactor = (delta > 0) ? ZOOM_FACTOR : (1.0 / ZOOM_FACTOR);
        double newScale = canvasScale.getX() * zoomFactor;
        newScale = Math.max(MIN_ZOOM, Math.min(newScale, MAX_ZOOM));
        Point2D mouseLogicalCoords = canvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        canvasScale.setPivotX(mouseLogicalCoords.getX());
        canvasScale.setPivotY(mouseLogicalCoords.getY());
        canvasScale.setX(newScale);
        canvasScale.setY(newScale);
    }
    
    @FXML
    private void onViewportMousePressed(MouseEvent event) {
        if (event.isSecondaryButtonDown()) {
            isPanning = true;
            panStartX = event.getSceneX();
            panStartY = event.getSceneY();
            event.consume();
        }
    }

    @FXML
    private void onViewportMouseDragged(MouseEvent event) {
        if (isPanning && event.isSecondaryButtonDown()) {
            double dx = event.getSceneX() - panStartX;
            double dy = event.getSceneY() - panStartY;
            canvasTranslate.setX(canvasTranslate.getX() + dx);
            canvasTranslate.setY(canvasTranslate.getY() + dy);
            panStartX = event.getSceneX();
            panStartY = event.getSceneY();
            event.consume();
        }
    }

    @FXML
    private void onViewportMouseReleased(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            isPanning = false;
            event.consume();
        }
    }

    // =========================================================================
    // --- Drawing Event Handlers (Attached to Canvas) ---
    // =========================================================================
    private Point2D getLogicalCoords(MouseEvent event) {
        return canvas.sceneToLocal(event.getSceneX(), event.getSceneY());
    }

    @FXML 
    private void onCanvasMousePressed(final MouseEvent e) {
        if (e.isPrimaryButtonDown()) {
            Point2D logicalCoords = getLogicalCoords(e);
            viewModel.onMousePressed(logicalCoords.getX(), logicalCoords.getY()); 
            redraw();
            e.consume();
        }
    }

    @FXML 
    private void onCanvasMouseDragged(final MouseEvent e) {
        if (e.isPrimaryButtonDown()) {
            Point2D logicalCoords = getLogicalCoords(e);
            viewModel.onMouseDragged(logicalCoords.getX(), logicalCoords.getY()); 
            redraw();
            e.consume();
        }
    }

    @FXML 
    private void onCanvasMouseReleased(final MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            Point2D logicalCoords = getLogicalCoords(e);
            viewModel.onMouseReleased(logicalCoords.getX(), logicalCoords.getY()); 
            redraw();
            e.consume();
        }
    }

    // =========================================================================
    // --- Toolbar Button Handlers (Unchanged) ---
    // =========================================================================
    @FXML
    private void onColorSelected(final ActionEvent event) {
        if (isUpdatingUI) return;
        final Color selectedColor = colorPicker.getValue();
        viewModel.activeColor.set(selectedColor);
        if (viewModel.selectedShapeId.get() != null) {
            viewModel.updateSelectedShapeColor(selectedColor);
        }
        redraw();
    }

    @FXML
    private void onToolSelected(final ActionEvent event) {
        final ToggleButton source = (ToggleButton) event.getSource();
        if (source.getUserData() != null) {
            viewModel.activeTool.set((ToolType) source.getUserData());
            if (viewModel.activeTool.get() != ToolType.SELECT) {
                viewModel.selectedShapeId.set(null);
            }
            redraw();
        }
    }

    @FXML
    private void onDelete() {
        viewModel.deleteSelectedShape();
    }

    @FXML
    private void onCapture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Canvas as PNG");
        fileChooser.setInitialFileName("canvas-capture.png");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"));

        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());

        if (file != null) {
            try {
                WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                canvas.snapshot(null, writableImage);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(bufferedImage, "png", file);
                System.out.println("Canvas captured and saved to: " + file.getAbsolutePath());
            } catch (IOException ex) {
                System.err.println("Error capturing or saving canvas: " + ex.getMessage());
            }
        } else {
            System.out.println("Canvas capture cancelled by user.");
        }
    }
    
    @FXML
    private void onRegularize() {
        System.out.println("Regularize button clicked (no logic assigned).");
    }

    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            viewModel.deleteSelectedShape();
        }
    }

    @FXML private void onUndo() {
        viewModel.undo();
    }

    @FXML private void onRedo() {
        viewModel.redo();
    }
}