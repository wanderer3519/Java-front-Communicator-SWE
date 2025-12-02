/*
 * -----------------------------------------------------------------------------
 * File: CanvasApp.java
 * Owner: 
 * Roll Number: 112201014
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.ux.canvas;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.collaboration.CanvasNetworkService;
import com.swe.canvas.datamodel.collaboration.NetworkService;
import com.swe.canvas.datamodel.manager.ActionManager;
import com.swe.canvas.datamodel.manager.ClientActionManager;
import com.swe.canvas.datamodel.manager.HostActionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application entry point. Sets up the Host/Client simulation and launches
 * two windows.
 *
 * <p>THIS IS THE NEW MAIN CLASS TO RUN.</p>
 *
 *
 */
public class CanvasApp extends Application {

    /**
     * Default width for the application window.
     */
    private static final int WINDOW_WIDTH = 1000;

    /**
     * Default height for the application window.
     */
    private static final int WINDOW_HEIGHT = 700;

    /**
     * Starts the JavaFX application.
     *
     * <p>This method initializes the simulated network, creates the Host and Client
     * action managers, and launches a window for each to demonstrate real-time
     * collaboration on a local machine.</p>
     *
     * @param primaryStage The primary stage for this application (not used directly).
     * @throws Exception if there is an issue loading the FXML or resources.
     */
    @Override
    public void start(final Stage primaryStage) throws Exception {
        // 1. Create the core components
        final NetworkService network = new CanvasNetworkService(null);
        final CanvasState hostCanvasState = new CanvasState();

        // The single source of truth
        // 2. Create the Host
        final ActionManager hostManager = new HostActionManager("HOST-USER", hostCanvasState, network);

        // 3. Create a Client
        final ActionManager clientManager = new ClientActionManager(
                "CLIENT-USER-1",
                new CanvasState(),
                network
        );

        // 4. Launch Host Window
        launchWindow("Host View (Authoritative)", hostManager);

        // 5. Launch Client Window
        launchWindow("Client View (Mirror)", clientManager);
    }

    /**
     * Helper to launch a new canvas window with its own ActionManager.
     *
     * <p>Loads the FXML layout, injects the specific ActionManager (Host or Client)
     * into the controller, and displays the stage.</p>
     *
     * @param title         The title to display on the window.
     * @param actionManager The manager instance to control this window's logic.
     * @throws Exception if FXML loading fails.
     */
    private void launchWindow(final String title, final ActionManager actionManager) throws Exception {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/canvas-view.fxml"));
        final Parent root = loader.load();

        // Get the controller and inject the ActionManager
        final CanvasController controller = loader.getController();
        controller.initModel(actionManager, null);

        final Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        stage.show();
    }

    /**
     * The main entry point for the Java application.
     *
     * @param args Command line arguments.
     */
    public static void main(final String[] args) {
        launch(args);
    }
}