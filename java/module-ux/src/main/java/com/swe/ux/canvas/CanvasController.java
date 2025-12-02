/*
 * -----------------------------------------------------------------------------
 * File: CanvasController.java
 * Owner: Darla Manohar
 * Roll Number: 112201034
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.ux.canvas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.manager.ActionManager;
import com.swe.canvas.datamodel.manager.HostActionManager;
import com.swe.canvas.datamodel.serialization.ShapeSerializer;
import com.swe.cloud.datastructures.Entity;
import com.swe.cloud.functionlibrary.CloudFunctionLibrary;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.controller.serialize.DataSerializer;
import com.swe.ux.canvas.util.ColorConverter;
import com.swe.ux.viewmodels.CanvasViewModel;
import com.swe.ux.viewmodels.ToolType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 * Controller for the main Canvas View.
 *
 * <p>Handles UI events, user input (mouse/keyboard), and bridges the communication
 * between the JavaFX View and the ViewModel/ActionManager backend.</p>
 *
 * @author Darla Manohar
 */
public class CanvasController {

    /** Entity ID used for Cloud requests (typically -1 for new/generic). */
    private static final int CLOUD_ENTITY_ID = -1;

    /** Modulo for generating random user suffixes. */
    private static final int USER_ID_MODULO = 10000;

    /** Default snapshot width if canvas dimensions are invalid. */
    private static final int DEFAULT_SNAPSHOT_WIDTH = 800;

    /** Default snapshot height if canvas dimensions are invalid. */
    private static final int DEFAULT_SNAPSHOT_HEIGHT = 600;

    /** Snapshots folder. */
    private static final String TEMP_DIR_NAME = "temp_snapshots";


    /** Zoom factor per scroll tick. */
    private static final double ZOOM_FACTOR = 1.1;

    /** Base scale value (1.0). */
    private static final double BASE_SCALE = 1.0;

    /** Maximum allowed zoom level. */
    private static final double MAX_ZOOM = 5.0;

    /** Minimum allowed zoom level. */
    private static final double MIN_ZOOM = 0.5;

    /** Button to select the selection tool. */
    @FXML private ToggleButton selectBtn;

    /** Button to select the freehand tool. */
    @FXML private ToggleButton freehandBtn;

    /** Button to select the rectangle tool. */
    @FXML private ToggleButton rectBtn;

    /** Button to select the ellipse tool. */
    @FXML private ToggleButton ellipseBtn;

    /** Button to select the line tool. */
    @FXML private ToggleButton lineBtn;

    /** Button to select the triangle tool. */
    @FXML private ToggleButton triangleBtn;

    /** Slider to adjust stroke width. */
    @FXML private Slider sizeSlider;

    /** Picker to select stroke color. */
    @FXML private ColorPicker colorPicker;

    /** Button to delete selected shapes. */
    @FXML private Button deleteBtn;

    /** Button to trigger shape regularization. */
    @FXML private Button regularizeBtn;

    /** Button to undo the last action. */
    @FXML private Button undoBtn;

    /** Button to redo the last undone action. */
    @FXML private Button redoBtn;

    /** Button to capture a screenshot of the canvas. */
    @FXML private Button captureBtn;

    /** Button to save the canvas state. */
    @FXML private Button saveBtn;

    /** Button to restore the canvas state. */
    @FXML private Button restoreBtn;

    /** Button to analyze the canvas content using AI. */
    @FXML private Button analyzeBtn;

    /** The actual canvas element for drawing. */
    @FXML private Canvas canvas;

    /** The container holding the canvas stack. */
    @FXML private StackPane canvasContainer;

    /** The pane holding the canvas (used for transforms). */
    @FXML private StackPane canvasHolder;

    /** Sidebar for AI analysis results. */
    @FXML private VBox aiSidebar;

    /** Text area for displaying AI descriptions. */
    @FXML private TextArea aiDescriptionArea;

    /** View model managing UI state. */
    private CanvasViewModel viewModel;

    /** Manager handling actions and network sync. */
    private ActionManager actionManager;

    /** Renderer responsible for drawing shapes. */
    private CanvasRenderer renderer;

    /** Flag to prevent cyclic updates during UI refresh. */
    private boolean isUpdatingUI = false;

    /** RPC interface for remote operations. */
    private AbstractRPC rpc;

    /** Library for interacting with cloud functions. */
    private final CloudFunctionLibrary cloudLib = new CloudFunctionLibrary();

    /** Translation transform for panning. */
    private Translate canvasTranslate;

    /** Scale transform for zooming. */
    private Scale canvasScale;

    /** Flag indicating if panning is in progress. */
    private boolean isPanning = false;

    /** X coordinate where panning started. */
    private double panStartX;

    /** Y coordinate where panning started. */
    private double panStartY;

    /**
     * Initializes the controller with the necessary backend models.
     *
     * @param manager     The ActionManager handling state.
     * @param rpcInstance The RPC instance for remote calls (AI/Regularization).
     */
    public void initModel(final ActionManager manager, final AbstractRPC rpcInstance) {
        this.actionManager = manager;
        this.rpc = rpcInstance;
        this.viewModel = new CanvasViewModel("user-"
                + System.nanoTime() % USER_ID_MODULO, manager);

        initializeControls();

        final boolean isHost = manager instanceof HostActionManager;
        if (saveBtn != null) {
            saveBtn.setVisible(isHost);
            saveBtn.setManaged(isHost);
        }
        if (restoreBtn != null) {
            restoreBtn.setVisible(isHost);
            restoreBtn.setManaged(isHost);
        }

        // --- Trigger initialization sequence (Handshake) ---
        this.actionManager.initialize();
    }

    private void initializeControls() {
        renderer = new CanvasRenderer(canvas);
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvas.widthProperty().addListener(o -> redraw());
        canvas.heightProperty().addListener(o -> redraw());

        canvasTranslate = GeometryFactory.createTranslate();
        canvasScale = GeometryFactory.createScale();
        canvasHolder.getTransforms().addAll(canvasTranslate, canvasScale);

        final Rectangle clip = GeometryFactory.createRectangle();
        clip.widthProperty().bind(canvasContainer.widthProperty());
        clip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(clip);

        sizeSlider.setValue(viewModel.activeStrokeWidth.get());
        colorPicker.setValue(viewModel.activeColor.get());

        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingUI) {
                return;
            }
            viewModel.activeStrokeWidth.set(newVal.doubleValue());
            if (viewModel.selectedShapeId.get() != null) {
                viewModel.updateSelectedShapeThickness(newVal.doubleValue());
            }
        });

        viewModel.selectedShapeId.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                final ShapeState state = viewModel.getCanvasState().getShapeState(newVal);
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

        this.actionManager.setOnUpdate(() -> Platform.runLater(() -> {
            if (viewModel != null) {
                viewModel.handleValidatedUpdate();
            }
            redraw();
        }));

        freehandBtn.setUserData(ToolType.FREEHAND);
        selectBtn.setUserData(ToolType.SELECT);
        rectBtn.setUserData(ToolType.RECTANGLE);
        ellipseBtn.setUserData(ToolType.ELLIPSE);
        lineBtn.setUserData(ToolType.LINE);
        triangleBtn.setUserData(ToolType.TRIANGLE);

        redraw();
    }

    @FXML
    private void onScroll(final ScrollEvent event) {
        event.consume();
        final double delta = event.getDeltaY();
        if (delta == 0) {
            return;
        }

        final double zoomFactor;
        if (delta > 0) {
            zoomFactor = ZOOM_FACTOR;
        } else {
            zoomFactor = BASE_SCALE / ZOOM_FACTOR;
        }

        double newScale = canvasScale.getX() * zoomFactor;
        newScale = Math.max(MIN_ZOOM, Math.min(newScale, MAX_ZOOM));
        final Point2D mouseLogicalCoords = canvas
                .sceneToLocal(event.getSceneX(), event.getSceneY());
        canvasScale.setPivotX(mouseLogicalCoords.getX());
        canvasScale.setPivotY(mouseLogicalCoords.getY());
        canvasScale.setX(newScale);
        canvasScale.setY(newScale);
    }

    @FXML
    private void onViewportMousePressed(final MouseEvent event) {
        if (event.isSecondaryButtonDown()) {
            isPanning = true;
            panStartX = event.getSceneX();
            panStartY = event.getSceneY();
            event.consume();
        }
    }

    @FXML
    private void onViewportMouseDragged(final MouseEvent event) {
        if (isPanning && event.isSecondaryButtonDown()) {
            final double dx = event.getSceneX() - panStartX;
            final double dy = event.getSceneY() - panStartY;
            canvasTranslate.setX(canvasTranslate.getX() + dx);
            canvasTranslate.setY(canvasTranslate.getY() + dy);
            panStartX = event.getSceneX();
            panStartY = event.getSceneY();
            event.consume();
        }
    }

    @FXML
    private void onViewportMouseReleased(final MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            isPanning = false;
            event.consume();
        }
    }

    @FXML
    private void onCanvasMousePressed(final MouseEvent e) {
        if (e.isPrimaryButtonDown()) {
            final Point2D p = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
            viewModel.onMousePressed(p.getX(), p.getY());
            redraw();
        }
    }

    @FXML
    private void onCanvasMouseDragged(final MouseEvent e) {
        if (e.isPrimaryButtonDown()) {
            final Point2D p = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
            viewModel.onMouseDragged(p.getX(), p.getY());
            redraw();
        }
    }

    @FXML
    private void onCanvasMouseReleased(final MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            final Point2D p = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
            viewModel.onMouseReleased(p.getX(), p.getY());
            redraw();
        }
    }

    @FXML
    private void onColorSelected(final ActionEvent event) {
        if (!isUpdatingUI) {
            viewModel.activeColor.set(colorPicker.getValue());
            if (viewModel.selectedShapeId.get() != null) {
                viewModel.updateSelectedShapeColor(colorPicker.getValue());
            }
            redraw();
        }
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
    private void onUndo() {
        viewModel.undo();
    }

    @FXML
    private void onRedo() {
        viewModel.redo();
    }

    @FXML
    private void onCapture() {
        final FileChooser fileChooser = UiFactory
                .createFileChooser("Save Canvas as PNG", "canvas-capture.png");
        final File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());

        if (file != null) {
            try {
                final WritableImage writableImage = GeometryFactory
                        .createWritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                canvas.snapshot(null, writableImage);
                final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(bufferedImage, "png", file);
                System.out.println("Canvas captured and saved to: " + file.getAbsolutePath());
            } catch (final Exception ex) {
                System.err.println("Error capturing or saving canvas: " + ex.getMessage());
            }
        }
    }

    private void redraw() {
        Platform.runLater(() -> {
            if (renderer != null && viewModel != null) {
                renderer.render(
                        viewModel.getCanvasState(),
                        viewModel.getTransientShape(),
                        viewModel.selectedShapeId.get(),
                        viewModel.isDraggingSelection
                );
            }
        });
    }

    @FXML
    private void onSave() {
        if (actionManager == null) {
            return;
        }

        final ChoiceDialog<String> dialog = UiFactory
                .createChoiceDialog("Cloud", "Cloud", "Local File");
        dialog.setTitle("Save Canvas");
        dialog.setHeaderText("Choose Storage Method");

        dialog.showAndWait().ifPresent(this::handleSaveSelection);
    }

    private void handleSaveSelection(final String result) {
        try {
            final String jsonState = actionManager.saveMap();
            if ("Local File".equals(result)) {
                final FileChooser fileChooser = UiFactory.createFileChooser(null, "canvas.json");
                final File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
                if (file != null) {
                    Files.writeString(file.toPath(), jsonState);
                }
            } else {
                saveToCloud(jsonState);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveToCloud(final String jsonState) {
        try {
            final String boardId = UUID.randomUUID().toString();
            final JsonNode dataNode = JsonFactory.readJsonTree(jsonState);

            final Entity req = new Entity(
                    "CANVAS", "saves", boardId, "state", CLOUD_ENTITY_ID, null, dataNode
            );

            cloudLib.cloudPost(req).thenAccept(res -> {
                Platform.runLater(() -> {
                    final TextInputDialog info = UiFactory.createTextInputDialog(boardId);
                    info.setTitle("Saved to Cloud");
                    info.setHeaderText("Canvas Saved Successfully!");
                    info.setContentText("Your Board ID (Copy this to restore):");
                    info.showAndWait();
                });
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRestore() {
        if (actionManager == null) {
            return;
        }

        final ChoiceDialog<String> dialog = UiFactory
                .createChoiceDialog("Cloud", "Cloud", "Local File");
        dialog.setTitle("Restore Canvas");
        dialog.setHeaderText("Choose Source");

        dialog.showAndWait().ifPresent(this::handleRestoreSelection);
    }

    private void handleRestoreSelection(final String result) {
        try {
            if ("Local File".equals(result)) {
                final FileChooser fileChooser = UiFactory.createFileChooser(null, null);
                final File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
                if (file != null) {
                    final String json = Files.readString(file.toPath());
                    actionManager.restoreMap(json);
                }
            } else {
                restoreFromCloudPrompt();
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private void restoreFromCloudPrompt() {
        final TextInputDialog idInput = UiFactory.createTextInputDialog("");
        idInput.setTitle("Cloud Restore");
        idInput.setHeaderText("Enter the Board ID");
        idInput.setContentText("Board UUID:");

        idInput.showAndWait().ifPresent(boardId -> {
            if (boardId.trim().isEmpty()) {
                return;
            }
            processCloudRestore(boardId.trim());
        });
    }

    private void processCloudRestore(final String boardId) {
        final Entity req = new Entity(
                "CANVAS", "saves", boardId, null, CLOUD_ENTITY_ID, null, null
        );

        cloudLib.cloudGet(req).thenAccept(res -> {
            if (res.data() != null) {
                String jsonState = null;
                if (res.data().isArray() && !res.data().isEmpty()) {
                    jsonState = res.data().get(0).get("data").toString();
                } else if (res.data().isObject() && res.data().has("data")) {
                    jsonState = res.data().get("data").toString();
                }

                if (jsonState != null) {
                    final String finalState = jsonState;
                    Platform.runLater(() -> {
                        actionManager.restoreMap(finalState);
                        UiFactory.createAlert(Alert.AlertType.INFORMATION,
                                "Restored successfully!").show();
                    });
                } else {
                    Platform.runLater(() -> UiFactory.createAlert(Alert.AlertType.ERROR,
                            "Invalid format.").show());
                }
            } else {
                Platform.runLater(() -> UiFactory.createAlert(Alert.AlertType.ERROR,
                        "ID not found.").show());
            }
        });
    }

    @FXML
    private void onRegularize() {
        if (viewModel.selectedShapeId.get() == null || rpc == null) {
            System.err.println("Regularize skipped: No selection or RPC missing.");
            return;
        }

        final ShapeState state = viewModel.getCanvasState()
                .getShapeState(viewModel.selectedShapeId.get());
        if (state == null || state.isDeleted()) {
            return;
        }

        final String shapeJson = ShapeSerializer.serializeShape(state);

        CompletableFuture.runAsync(() -> {
            try {
                final byte[] data = DataSerializer.serialize(shapeJson);
                final byte[] response = rpc.call("canvas:regularize", data).get();

                if (response != null && response.length > 0) {
                    final String updatedShapeJson = DataSerializer
                            .deserialize(response, String.class);
                    final ShapeState validatedState = ShapeSerializer
                            .deserializeShape(updatedShapeJson);

                    if (validatedState != null && validatedState.getShape() != null) {
                        Platform.runLater(() -> {
                            actionManager.requestModify(state, validatedState.getShape());
                            viewModel.selectedShapeId.set(state.getShapeId());
                        });
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void onDescribe() {
        if (rpc == null) {
            UiFactory.createAlert(Alert.AlertType.ERROR, "RPC not initialized.").show();
            return;
        }

        aiSidebar.setVisible(true);
        aiSidebar.setManaged(true);
        aiDescriptionArea.setText("Analyzing canvas... please wait.");

        performAnalysis();
    }

    private void performAnalysis() {
        try {
            final File relativeDir = new File(TEMP_DIR_NAME);
            if (!relativeDir.exists()) {
                if (!relativeDir.mkdirs()) {
                    throw new IOException("Could not create directory: " + relativeDir.getAbsolutePath());
                }
            }

            // final File tempFile = File.createTempFile("canvas_snapshot_", ".png");
            final String fileName = "canvas_snapshot_" + System.currentTimeMillis() + ".png";
            final File tempFile = new File(relativeDir, fileName);
            
            int width = (int) canvas.getWidth();
            int height = (int) canvas.getHeight();
            if (width <= 0) {
                width = DEFAULT_SNAPSHOT_WIDTH;
            }
            if (height <= 0) {
                height = DEFAULT_SNAPSHOT_HEIGHT;
            }

            final WritableImage writableImage = GeometryFactory.createWritableImage(width, height);
            canvas.snapshot(null, writableImage);

            final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
            ImageIO.write(bufferedImage, "png", tempFile);

            CompletableFuture.runAsync(() -> sendAnalyzeRequest(tempFile));

        } catch (final IOException e) {
            e.printStackTrace();
            aiDescriptionArea.setText("Failed to capture canvas: " + e.getMessage());
        }
    }

    private void sendAnalyzeRequest(final File tempFile) {
        try {
            final byte[] data = DataSerializer.serialize(tempFile.getAbsolutePath());
            final byte[] response = rpc.call("canvas:describe", data).get();
            final String finalDescription = DataSerializer.deserialize(response, String.class);

            Platform.runLater(() -> {
                aiDescriptionArea.setText(finalDescription);
                tempFile.deleteOnExit();
            });
        } catch (final Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> aiDescriptionArea
                    .setText("Error analyzing image: " + e.getMessage()));
        }
    }

    @FXML
    private void onCloseAiSidebar() {
        aiSidebar.setVisible(false);
        aiSidebar.setManaged(false);
        aiDescriptionArea.clear();
    }

    /**
     * Factory for geometry and image objects to reduce coupling.
     */
    private static class GeometryFactory {
        static Translate createTranslate() {
            return new Translate();
        }

        static Scale createScale() {
            return new Scale();
        }

        static Rectangle createRectangle() {
            return new Rectangle();
        }

        static WritableImage createWritableImage(final int width, final int height) {
            return new WritableImage(width, height);
        }
    }

    /**
     * Factory for UI dialogs to reduce coupling.
     */
    private static class UiFactory {
        static FileChooser createFileChooser(final String title, final String initialFileName) {
            final FileChooser fc = new FileChooser();
            if (title != null) {
                fc.setTitle(title);
            }
            if (initialFileName != null) {
                fc.setInitialFileName(initialFileName);
            }
            if (title != null && title.contains("PNG")) {
                fc.getExtensionFilters().add(new FileChooser
                        .ExtensionFilter("PNG files (*.png)", "*.png"));
            }
            return fc;
        }

        static ChoiceDialog<String> createChoiceDialog(final String defaultOption,
                                                       final String... choices) {
            return new ChoiceDialog<>(defaultOption, choices);
        }

        static TextInputDialog createTextInputDialog(final String defaultValue) {
            return new TextInputDialog(defaultValue);
        }

        static Alert createAlert(final Alert.AlertType type, final String content) {
            return new Alert(type, content);
        }
    }

    /**
     * Factory for JSON operations to reduce coupling.
     */
    private static class JsonFactory {
        static JsonNode readJsonTree(final String json) throws Exception {
            return new ObjectMapper().readTree(json);
        }
    }
}

