package com.swe.ux;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.swe.controller.ClientNode;
import com.swe.controller.RPC;
import com.swe.controller.Meeting.ParticipantRole;
import com.swe.controller.Meeting.UserProfile;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.controller.serialize.DataSerializer;
import com.swe.networking.AbstractController;
import com.swe.networking.NetworkFront;
import com.swe.ux.theme.ThemeManager;

import com.swe.ux.views.LoginPage;
import com.swe.ux.views.MainPage;
import com.swe.ux.views.MeetingPage;
import com.swe.ux.viewmodels.LoginViewModel;
import com.swe.ux.viewmodels.MainViewModel;
import com.swe.ux.viewmodels.MeetingViewModel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.io.IOException;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import com.swe.ux.binding.PropertyListeners;

/**
 * Main application class that initializes the UI and coordinates between
 * different views.
 */
public class App extends JFrame {
    /** Singleton instance of the application. */
    private static App instance;
    /** Card layout for switching between views. */
    private final CardLayout cardLayout;
    /** Main panel containing all views. */
    private final JPanel mainPanel;
    /** History stack for view navigation. */
    private final Stack<String> viewHistory = new Stack<>();

    /** Login view identifier. */
    public static final String LOGIN_VIEW = "LOGIN";
    /** Main view identifier. */
    public static final String MAIN_VIEW = "MAIN";
    /** Meeting view identifier. */
    public static final String MEETING_VIEW = "MEETING";

    /** Default window width. */
    private static final int DEFAULT_WIDTH = 1200;
    /** Default window height. */
    private static final int DEFAULT_HEIGHT = 700;
    /** Default port number for RPC connection. */
    private static final int DEFAULT_PORT = 6942;
    /** Sleep duration for JavaFX initialization. */
    private static final int JAVAFX_INIT_SLEEP_MS = 100;

    /** Currently logged in user profile. */
    private UserProfile currentUser;

    /** Login view model for managing login state. */
    private LoginViewModel loginViewModel;
    /** Main view model for managing main page state. */
    private MainViewModel mainViewModel;

    /** RPC instance for network communication. */
    private final AbstractRPC rpc;

    /**
     * Gets the singleton instance of the application.
     *
     * @param rpcInstance the RPC instance for network communication
     * @return the singleton App instance
     */
    public static App getInstance(final AbstractRPC rpcInstance) {
        if (instance == null) {
            instance = new App(rpcInstance);
        }
        return instance;
    }

    /**
     * Private constructor for singleton pattern.
     *
     * @param rpcInstance the RPC instance for network communication
     */
    private App(final AbstractRPC rpcInstance) {
        // Initialize services
        this.rpc = rpcInstance;
        // Set up the main panel with card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Initialize views
        initViews();

    }

    /**
     * Starts the application by initializing the window and showing the login view.
     */
    public void start() {

        // Set up the main window
        setTitle("Comm-Uni-Cate");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);

        // Add main panel to the frame
        add(mainPanel, BorderLayout.CENTER);

        // Apply theme
        final ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.setMainFrame(this);
        themeManager.setApp(this);

        // Show login view by default
        showView(LOGIN_VIEW);

        // Center the window
        setLocationRelativeTo(null);
    }

    /**
     * Refreshes the theme for the entire application.
     */
    public void refreshTheme() {
        final ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.applyThemeRecursively(mainPanel);
        SwingUtilities.updateComponentTreeUI(this);
        revalidate();
        repaint();
    }

    private String getIPFromClientNodeString(final String val) {
        if (!val.startsWith("ClientNode")) {
            return val;
        }
        final String ipVal = val.substring(val.indexOf("hostName=") + 9);
        return ipVal.substring(0, ipVal.indexOf(","));
    }

    /**
     * Initializes all the views and adds them to the card layout.
     */
    private void initViews() {

        // Initialize ViewModels
        loginViewModel = new LoginViewModel(rpc);
        mainViewModel = new MainViewModel(rpc);
        System.out.println("Meeting");

        final UserProfile newUser = new UserProfile("", "You", ParticipantRole.STUDENT);
        // Will be set when user joins a meeting
        final MeetingViewModel meetingViewModel = new MeetingViewModel(newUser, rpc);

        // Initialize Views with their respective ViewModels
        final LoginPage loginView = new LoginPage(loginViewModel);
        final MainPage mainView = new MainPage(mainViewModel);
        final MeetingPage meetingView = new MeetingPage(meetingViewModel);

        // Add views to card layout
        mainPanel.add(loginView, LOGIN_VIEW);
        mainPanel.add(mainView, MAIN_VIEW);
        mainPanel.add(meetingView, MEETING_VIEW);

        meetingViewModel.startMeeting();
        // Set up navigation listeners
        setupLoginListener();
        setupLogoutListener();

        // Use an array to hold the meeting view reference for use in lambda
        final MeetingPage[] meetingViewRef = new MeetingPage[] {meetingView};

        // Use an array to hold the current active meeting view model reference
        final MeetingViewModel[] activeMeetingViewModelRef = new MeetingViewModel[] {meetingViewModel};

        setupParticipantUpdateListener(activeMeetingViewModelRef);
        setupStartMeetingListener(meetingViewRef, activeMeetingViewModelRef);
        setupJoinMeetingListener(meetingViewRef, activeMeetingViewModelRef);

        // When meeting ends (for initial meeting view model), go back to main view
        meetingViewModel.getIsMeetingActive().addListener(PropertyListeners.onBooleanChanged(isActive -> {
            if (!Boolean.TRUE.equals(isActive)) {
                showView(MAIN_VIEW);
            }
        }));
    }

    /**
     * Sets up the login listener for navigation.
     */
    private void setupLoginListener() {
        loginViewModel.getCurrentUser().addListener(PropertyListeners.onUserProfileChanged(user -> {
            if (user != null) {
                this.currentUser = user;
                mainViewModel.setCurrentUser(currentUser);
                
                // Load theme from cloud after user is logged in
                ThemeManager.getInstance().loadThemeFromCloud();
                System.out.println("App: Theme loaded from cloud");
                System.out.println("App: Theme: " + ThemeManager.getInstance().getCurrentTheme());
                showView(MAIN_VIEW);
            }
        }));
    }

    /**
     * Sets up the logout listener.
     */
    private void setupLogoutListener() {
        mainViewModel.getLogoutRequested().addListener(PropertyListeners.onBooleanChanged(logoutRequested -> {
            if (logoutRequested) {
                logout();
                // Reset the flag
                mainViewModel.getLogoutRequested().set(false);
            }
        }));
    }

    /**
     * Sets up the participant update listener from RPC.
     *
     * @param activeMeetingViewModelRef reference to the active meeting view model
     */
    private void setupParticipantUpdateListener(final MeetingViewModel[] activeMeetingViewModelRef) {
        rpc.subscribe("core/updateParticipants", data -> {
            try {
                final Map<ClientNode, UserProfile> participantsMap = DataSerializer.deserialize(data,
                        new TypeReference<Map<ClientNode, UserProfile>>() { });
                System.out.println("App: participantsMap: " + participantsMap);
                participantsMap.forEach((clientNode, userProfile) -> {
                    System.out.println("App: clientNode: " + clientNode + " userProfile: " + userProfile);
                    activeMeetingViewModelRef[0].getIpToMail().put(userProfile.getEmail(), clientNode.hostName());
                    // Use the currently active meeting view model
                    activeMeetingViewModelRef[0].addParticipant(userProfile);
                    System.out.println("App: participants: " + activeMeetingViewModelRef[0].getParticipants().get());
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return new byte[0];
        });
    }

    /**
     * Sets up the start meeting listener.
     *
     * @param meetingViewRef reference to the meeting view
     * @param activeMeetingViewModelRef reference to the active meeting view model
     */
    private void setupStartMeetingListener(final MeetingPage[] meetingViewRef,
                                           final MeetingViewModel[] activeMeetingViewModelRef) {
        mainViewModel.getStartMeetingRequested().addListener(PropertyListeners.onBooleanChanged(startMeeting -> {
            if (startMeeting && currentUser != null) {
                // First, get the meeting ID from MainViewModel by creating the meeting
                final String meetingId = mainViewModel.startMeeting();

                // Only proceed if we successfully got a meeting ID
                if (meetingId == null || meetingId.trim().isEmpty()) {
                    System.err.println("App: Failed to create meeting - no meeting ID received from RPC");
                    mainViewModel.getStartMeetingRequested().set(false);
                    return;
                }

                // Ensure the local profile reflects instructor role
                currentUser.setRole(ParticipantRole.INSTRUCTOR);
                // Create a new meeting view model for this meeting with Instructor role
                final MeetingViewModel newMeetingViewModel = new MeetingViewModel(currentUser, "Instructor", rpc);

                // Update the active meeting view model reference
                activeMeetingViewModelRef[0] = newMeetingViewModel;

                // Explicitly pass the meeting ID from MainViewModel to MeetingViewModel
                newMeetingViewModel.setMeetingId(meetingId);
                System.out.println("App: Passing meeting ID from MainViewModel to MeetingViewModel: " + meetingId);

                // Create a new MeetingPage with the new view model
                meetingViewRef[0] = new MeetingPage(newMeetingViewModel);

                // Try to start the meeting
                newMeetingViewModel.startMeeting();

                // Only change view if meeting was successfully started
                if (!Boolean.TRUE.equals(newMeetingViewModel.getIsMeetingActive().get())) {
                    System.err.println("App: Failed to start meeting - meeting not active");
                    mainViewModel.getStartMeetingRequested().set(false);
                    return;
                }

                // Set up listener for when meeting ends - navigate back to main view
                newMeetingViewModel.getIsMeetingActive().addListener(PropertyListeners.onBooleanChanged(isActive -> {
                    if (!Boolean.TRUE.equals(isActive)) {
                        showView(MAIN_VIEW);
                        // Reset the flag so the button can be clicked again
                        mainViewModel.getStartMeetingRequested().set(false);
                    }
                }));

                mainPanel.add(meetingViewRef[0], MEETING_VIEW);
                showView(MEETING_VIEW);

                // Reset the flag
                mainViewModel.getStartMeetingRequested().set(false);
            }
        }));
    }

    /**
     * Sets up the join meeting listener.
     *
     * @param meetingViewRef reference to the meeting view
     * @param activeMeetingViewModelRef reference to the active meeting view model
     */
    private void setupJoinMeetingListener(final MeetingPage[] meetingViewRef,
                                          final MeetingViewModel[] activeMeetingViewModelRef) {
        mainViewModel.getJoinMeetingRequested().addListener(PropertyListeners.onBooleanChanged(joinMeeting -> {
            if (joinMeeting && currentUser != null) {
                // Get the meeting code from MainViewModel
                final String meetingCode = mainViewModel.getMeetingCode().get();

                // Only proceed if we have a valid meeting code
                if (meetingCode == null || meetingCode.trim().isEmpty()) {
                    System.err.println("App: Failed to join meeting - no meeting code provided");
                    mainViewModel.getJoinMeetingRequested().set(false);
                    return;
                }

                mainViewModel.joinMeeting(meetingCode);

                // Create a new meeting view model for joining meeting with Student role
                currentUser.setRole(ParticipantRole.STUDENT);
                final MeetingViewModel newMeetingViewModel = new MeetingViewModel(currentUser, "Student", rpc);

                // Create a new MeetingPage with the new view model
                meetingViewRef[0] = new MeetingPage(newMeetingViewModel);

                // Update the active meeting view model reference
                activeMeetingViewModelRef[0] = newMeetingViewModel;

                // Explicitly pass the meeting ID from MainViewModel to MeetingViewModel
                newMeetingViewModel.setMeetingId(meetingCode);
                System.out.println("App: Passing meeting code from MainViewModel to MeetingViewModel: " + meetingCode);

                // Try to start the meeting with the provided meeting ID
                newMeetingViewModel.startMeeting();

                // Only change view if meeting was successfully started
                if (!Boolean.TRUE.equals(newMeetingViewModel.getIsMeetingActive().get())) {
                    System.err.println("App: Failed to join meeting - meeting not active");
                    mainViewModel.getJoinMeetingRequested().set(false);
                    mainViewModel.getMeetingCode().set("");
                    return;
                }

                // Set up listener for when meeting ends - navigate back to main view
                newMeetingViewModel.getIsMeetingActive().addListener(PropertyListeners.onBooleanChanged(isActive -> {
                    if (!Boolean.TRUE.equals(isActive)) {
                        showView(MAIN_VIEW);
                        // Reset the flag and meeting code so the button can be clicked again
                        mainViewModel.getJoinMeetingRequested().set(false);
                        mainViewModel.getMeetingCode().set("");

                    }
                }));

                mainPanel.add(meetingViewRef[0], MEETING_VIEW);
                showView(MEETING_VIEW);

                // Reset the flag and meeting code
                mainViewModel.getJoinMeetingRequested().set(false);
                mainViewModel.getMeetingCode().set("");
            }
        }));
    }

    /**
     * Shows the specified view.
     * 
     * @param viewName The name of the view to show
     */
    public void showView(final String viewName) {
        cardLayout.show(mainPanel, viewName);

        if (MEETING_VIEW.equals(viewName)) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setResizable(true);
        } else {
            setExtendedState(JFrame.NORMAL);
            setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            setLocationRelativeTo(null);
        }

        // Add to history if it's different from the current view
        if (viewHistory.isEmpty() || !viewHistory.peek().equals(viewName)) {
            viewHistory.push(viewName);
        }
    }

    /**
     * Navigates back to the previous view.
     */
    public void navigateBack() {
        if (viewHistory.size() > 1) {
            viewHistory.pop(); // Remove current view
            final String previousView = viewHistory.pop(); // Get previous view
            showView(previousView); // Show previous view (will be added back to history)
        }
    }

    /**
     * Main entry point of the application.
     *
     * @param args command line arguments, first argument can be port number
     */
    public static void main(final String[] args) {
        int portNumber = DEFAULT_PORT;

        if (args.length > 0) {
            final String port = args[0];
            portNumber = Integer.parseInt(port);
        }

        final AbstractRPC rpc = new RPC();
        final App app = App.getInstance(rpc);

        // Create and show the application window

        final Thread handler;
        try {
            handler = rpc.connect(portNumber);
            final AbstractController nController = NetworkFront.getInstance();
            nController.consumeRPC(rpc);
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // try {
        // byte[] data = rpc.call("core/register", new byte[0]).get();
        // System.out.println("Data: " + data.length);
        // } catch (InterruptedException | ExecutionException e) {
        // throw new RuntimeException(e);
        // }

        // Initialize JavaFX toolkit early in a separate thread to avoid macOS conflicts
        // This prevents macOS window management conflicts
        final Thread javaFXInitThread = new Thread(() -> {
            try {
                com.swe.ux.integration.JavaFXSwingBridge.initializeJavaFX();
            } catch (Exception e) {
                System.err.println("Warning: Could not initialize JavaFX early: " + e.getMessage());
            }
        });
        javaFXInitThread.setDaemon(true);
        javaFXInitThread.start();

        // Give JavaFX a moment to initialize
        try {
            Thread.sleep(JAVAFX_INIT_SLEEP_MS);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Run on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            app.start();
            app.setVisible(true);
        });

        try {
            handler.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Getters
    public UserProfile getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the current user and updates the UI accordingly.
     * 
     * @param user The user to set as current, or null to log out
     */
    public void setCurrentUser(final UserProfile user) {
        this.currentUser = user;
        // Update UI based on authentication state
        if (user != null) {
            showView(MAIN_VIEW);
        } else {
            showView(LOGIN_VIEW);
        }
    }

    /**
     * Logs out the current user completely.
     * Clears all user state and returns to login view.
     */
    public void logout() {
        // Clear current user
        this.currentUser = null;

        // Reset MainViewModel's current user
        if (mainViewModel != null) {
            mainViewModel.setCurrentUser(null);
            // Reset all flags and meeting code
            mainViewModel.getLogoutRequested().set(false);
            mainViewModel.getStartMeetingRequested().set(false);
            mainViewModel.getJoinMeetingRequested().set(false);
            mainViewModel.getMeetingCode().set("");
        }

        // Reset LoginViewModel to ensure clean state
        if (loginViewModel != null) {
            loginViewModel.reset();
        }

        // Logout from auth service (this will clear all user data)
        // TODO USE RPC TO LOGOUT

        // Clear view history
        viewHistory.clear();

        // Navigate to login view - this will trigger reset on LoginPage
        showView(LOGIN_VIEW);
    }
}