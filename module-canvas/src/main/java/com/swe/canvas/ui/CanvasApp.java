/*
 * -----------------------------------------------------------------------------
 *  File: CanvasApp.java
 *  Owner: Darla Manohar
 *  Roll Number: 112201034
 *  Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.ui;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.collaboration.NetworkSimulator;
import com.swe.canvas.datamodel.manager.ActionManager;
import com.swe.canvas.datamodel.manager.ClientActionManager;
import com.swe.canvas.datamodel.manager.HostActionManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application entry point.
 * Sets up the Host/Client simulation and launches two windows.
 *
 * THIS IS THE NEW MAIN CLASS TO RUN.
 */
public class CanvasApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Create the core components
        NetworkSimulator network = new NetworkSimulator();
        CanvasState hostCanvasState = new CanvasState(); // The single source of truth

        // 2. Create the Host
        ActionManager hostManager = new HostActionManager("HOST-USER", hostCanvasState, network);

        // 3. Create a Client
        ActionManager clientManager = new ClientActionManager("CLIENT-USER-1", new CanvasState(), network);

        // 4. Launch Host Window
        launchWindow("Host View (Authoritative)", hostManager);
        
        // 5. Launch Client Window
        launchWindow("Client View (Mirror)", clientManager);
    }

    /**
     * Helper to launch a new canvas window with its own ActionManager.
     */
    private void launchWindow(String title, ActionManager actionManager) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../fxml/canvas-view.fxml"));
        Parent root = loader.load();

        // Get the controller and inject the ActionManager
        CanvasController controller = loader.getController();
        controller.initModel(actionManager);
        
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}