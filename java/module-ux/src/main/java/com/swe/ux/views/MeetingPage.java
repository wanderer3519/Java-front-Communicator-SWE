package com.swe.ux.views;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.collaboration.CanvasNetworkService;
import com.swe.canvas.datamodel.manager.ClientActionManager;
import com.swe.canvas.datamodel.manager.HostActionManager;
import com.swe.controller.Meeting.ParticipantRole;
import com.swe.controller.Meeting.UserProfile;
import com.swe.screenNVideo.Utils;
import com.swe.ux.App;
import com.swe.ux.binding.PropertyListeners;
import com.swe.ux.theme.ThemeManager;
import com.swe.ux.ui.FrostedBackgroundPanel;
import com.swe.ux.ui.FrostedBadgeLabel;
import com.swe.ux.ui.FrostedToolbarButton;
import com.swe.ux.ui.MeetingControlButton;
import com.swe.ux.ui.MeetingStageTabs;
import com.swe.ux.ui.ModernTabbedPaneUI;
import com.swe.ux.ui.QuickDoubtPopup;
import com.swe.ux.ui.SoftCardPanel;
import com.swe.ux.ui.ThemeToggleButton;
import com.swe.ux.ui.FontUtil;
import com.swe.ux.viewmodels.ChatViewModel;
import com.swe.ux.viewmodels.MeetingViewModel;
import com.swe.ux.viewmodels.ParticipantsViewModel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MeetingPage.
 */
public class MeetingPage extends FrostedBackgroundPanel {
    // ... (Keep all existing constants and fields) ...
    private static final int SIDEBAR_MIN_WIDTH = 280;
    private static final int SIDEBAR_MAX_WIDTH = 520;
    private static final int SIDEBAR_DEFAULT_WIDTH = 360;
    private static final Color ACCENT_BLUE = new Color(82, 140, 255);
    private static final int LAYOUT_GAP = 20;
    private static final int BORDER_PADDING = 24;
    private static final int HEADER_BLUR_RADIUS = 10;
    private static final int HEADER_CORNER_RADIUS = 18;
    private static final int HEADER_BORDER_PADDING = 6;
    private static final int FLOW_LAYOUT_GAP = 10;
    private static final float TITLE_FONT_SIZE = 20.0f;
    private static final float DEFAULT_FONT_SIZE = 12.0f;
    private static final int STAGE_BLUR_RADIUS = 18;
    private static final int STAGE_LAYOUT_GAP = 12;
    private static final int STAGE_CORNER_RADIUS = 26;
    private static final int STAGE_PREFERRED_HEIGHT = 680;
    private static final int STAGE_BORDER_PADDING = 6;
    private static final int STAGE_CONTENT_BORDER = 4;
    private static final int STAGE_MIN_WIDTH = 400;
    private static final int SIDEBAR_BLUR_RADIUS = 10;
    private static final int SIDEBAR_LAYOUT_GAP = 8;
    /** Sidebar header background color. */
    private static final Color SIDEBAR_HEADER_BACKGROUND = new Color(35, 37, 48, 210);
    /** Sidebar body background. */
    private static final Color SIDEBAR_BODY_BACKGROUND = new Color(255, 255, 255, 70);
    /** Sidebar body radius. */
    private static final int SIDEBAR_BODY_RADIUS = 26;
    /** Panel blur radius. */
    private static final int PANEL_BLUR_RADIUS = 12;
    private static final int CONTROLS_BLUR_RADIUS = 14;
    private static final int CONTROLS_CORNER_RADIUS = 36;
    private static final int CONTROLS_GAP_H = 12;
    private static final int CONTROLS_GAP_V = 10;
    /** Controls card fill color. */
    private static final Color CONTROLS_BAR_BACKGROUND = new Color(255, 255, 255, 90);
    private static final int LEAVE_RED = 229;
    private static final int LEAVE_GREEN = 57;
    private static final int LEAVE_BLUE = 53;
    private static final int LEAVE_ALPHA = 200;
    private static final int SPLIT_DIVIDER_SIZE = 10;
    private static final double SPLIT_RESIZE_WEIGHT = 0.8;
    private static final int TIMER_DELAY_MS = 1000;
    private static final int COPY_FEEDBACK_DELAY_MS = 3000;
    private static final int ICON_SIZE = 18;
    private static final int ICON_BAR_WIDTH = 3;
    private static final int ICON_SPACING = 4;
    private static final int ICON_VERTICAL_OFFSET = 3;
    private static final int ICON_HEIGHT_ADJUST = 6;
    private static final int ICON_ROUND_CORNER = 4;

    private final MeetingViewModel meetingViewModel;
    private SoftCardPanel headerCard;
    private SoftCardPanel controlsBar;
    private JButton sidebarToggleBtn;
    private FrostedToolbarButton btnCopyLink;
    private SoftCardPanel stageCard;
    private MeetingStageTabs stageTabs;
    private CardLayout stageContentLayout;
    private JPanel stageContentPanel;
    private JSplitPane centerSplit;
    private SoftCardPanel sidebarCard;
    private JTabbedPane sidebarTabs;
    private JLabel sidebarHeaderLabel;
    private JPanel chatPanel;
    private JPanel participantsPanel;
    private boolean sidebarVisible = false;
    private int lastSidebarWidth = SIDEBAR_DEFAULT_WIDTH;
    private MeetingControlButton btnCamera;
    private MeetingControlButton btnShare;
    private MeetingControlButton btnLeave;
    private MeetingControlButton btnMute;
    private MeetingControlButton btnRaiseHand;
    private MeetingControlButton btnChat;
    private MeetingControlButton btnPeople;
    private FrostedToolbarButton meetingControlsButton;
    private FrostedBadgeLabel meetingIdBadge;
    /** Live clock label. */
    private JLabel liveClockLabel;
    private JLabel roleLabel;
    private Timer liveTimer;
    private final QuickDoubtPopup quickDoubtPopup;
    private boolean isHandRaised = false;
    private boolean allowParticipantChat = true;
    private boolean allowParticipantUnmute = true;
    private boolean allowParticipantShare = true;
    
    // Add reference to CanvasPage to pass participant updates
    private CanvasPage canvasPageReference;

    public MeetingPage(final MeetingViewModel meetingViewModelParam) {
        this.meetingViewModel = meetingViewModelParam;
        initializeUI();
        quickDoubtPopup = new QuickDoubtPopup();
        quickDoubtPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) { handleQuickDoubtClosed(); }
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) { handleQuickDoubtClosed(); }
        });
        registerThemeListener();
        setupBindings();
        startLiveClock();
        applyTheme();
    }

    // ... (Existing methods: initializeUI, buildHeader) ...
    private void initializeUI() {
        setLayout(new BorderLayout(LAYOUT_GAP, LAYOUT_GAP));
        setBorder(new EmptyBorder(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING));
        headerCard = buildHeader();
        add(headerCard, BorderLayout.NORTH);
        centerSplit = buildCenterSplit();
        add(centerSplit, BorderLayout.CENTER);
        controlsBar = buildControlsBar();
        final JPanel controlsWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        controlsWrapper.setOpaque(false);
        controlsWrapper.add(controlsBar);
        add(controlsWrapper, BorderLayout.SOUTH);
    }

    private SoftCardPanel buildHeader() {
        final SoftCardPanel card = new SoftCardPanel(HEADER_BLUR_RADIUS);
        card.setCornerRadius(HEADER_CORNER_RADIUS);
        final JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBorder(new EmptyBorder(0, HEADER_BORDER_PADDING, 0, HEADER_BORDER_PADDING));

        final JPanel leftCluster = new JPanel(new FlowLayout(FlowLayout.LEFT, FLOW_LAYOUT_GAP, 0));
        leftCluster.setOpaque(false);
        final JLabel title = new JLabel("Live Meeting");
        title.setFont(FontUtil.getJetBrainsMono(TITLE_FONT_SIZE, Font.BOLD));
        leftCluster.add(title);
        final ThemeToggleButton toggle = new ThemeToggleButton();
        leftCluster.add(toggle);
        meetingIdBadge = new FrostedBadgeLabel("Meeting: --");
        leftCluster.add(meetingIdBadge);
        final FrostedBadgeLabel ipBadge = new FrostedBadgeLabel("IP: " + Utils.getSelfIP());
        leftCluster.add(ipBadge);

        roleLabel = new JLabel(buildRoleLabelText());
        roleLabel.setFont(FontUtil.getJetBrainsMono(DEFAULT_FONT_SIZE, Font.PLAIN));
        leftCluster.add(roleLabel);
        row.add(leftCluster);
        row.add(Box.createHorizontalGlue());

        final JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, FLOW_LAYOUT_GAP, 0));
        rightCluster.setOpaque(false);
        liveClockLabel = new JLabel("Live: --:--");
        liveClockLabel.setFont(FontUtil.getJetBrainsMono(DEFAULT_FONT_SIZE, Font.PLAIN));
        rightCluster.add(liveClockLabel);
        btnCopyLink = new FrostedToolbarButton("Copy Link");
        btnCopyLink.addActionListener(e -> copyMeetingId());
        rightCluster.add(btnCopyLink);
        meetingControlsButton = new FrostedToolbarButton("Meeting Controls");
        meetingControlsButton.addActionListener(e -> openMeetingControlsDialog());
        rightCluster.add(meetingControlsButton);
        updateMeetingControlAvailability();
        sidebarToggleBtn = new JButton(new SidebarToggleIcon());
        sidebarToggleBtn.setToolTipText("Toggle right panel");
        sidebarToggleBtn.setBorderPainted(false);
        sidebarToggleBtn.setContentAreaFilled(false);
        sidebarToggleBtn.setFocusPainted(false);
        sidebarToggleBtn.addActionListener(e -> toggleSidebarVisibility());
        rightCluster.add(sidebarToggleBtn);
        row.add(rightCluster);
        card.setLayout(new BorderLayout());
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JSplitPane buildCenterSplit() {
        stageCard = buildStageCard();
        sidebarCard = buildSidebarCard();
        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stageCard, sidebarCard);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(SPLIT_DIVIDER_SIZE);
        split.setResizeWeight(SPLIT_RESIZE_WEIGHT);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(false);
        attachSplitListeners(split);
        stageCard.setMinimumSize(new Dimension(STAGE_MIN_WIDTH, 0));
        sidebarCard.setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, 0));
        sidebarCard.setVisible(false);
        split.setDividerLocation(1.0);
        return split;
    }

    private SoftCardPanel buildStageCard() {
        final SoftCardPanel card = new SoftCardPanel(STAGE_BLUR_RADIUS);
        card.setLayout(new BorderLayout(STAGE_LAYOUT_GAP, STAGE_LAYOUT_GAP));
        card.setCornerRadius(STAGE_CORNER_RADIUS);
        card.setPreferredSize(new Dimension(0, STAGE_PREFERRED_HEIGHT));
        final Map<String, String> tabs = new LinkedHashMap<>();
        tabs.put("MEETING", "Meeting");
        tabs.put("CANVAS", "Canvas");
        stageTabs = new MeetingStageTabs(tabs, this::switchStageView);
        stageTabs.setAccentColor(ACCENT_BLUE);
        stageContentLayout = new CardLayout();
        stageContentPanel = new JPanel(stageContentLayout);
        stageContentPanel.setOpaque(false);
        stageContentPanel.setBorder(new EmptyBorder(STAGE_CONTENT_BORDER, STAGE_CONTENT_BORDER,
                STAGE_CONTENT_BORDER, STAGE_BORDER_PADDING));

        final ScreenNVideo screenNVideo = new ScreenNVideo(meetingViewModel);
        final CanvasPage canvasPage;
        final String userId;
        if (meetingViewModel.getCurrentUser() != null) {
            userId = meetingViewModel.getCurrentUser().getEmail();
        } else {
            userId = "user-" + System.nanoTime();
        }

        if (meetingViewModel.getCurrentUser().getRole() == ParticipantRole.INSTRUCTOR) {
            final CanvasState hostCanvasState = new CanvasState();
            final CanvasNetworkService networkService = new CanvasNetworkService(meetingViewModel.getRpc());
            final HostActionManager hostManager = new HostActionManager(userId, hostCanvasState,
                    networkService, meetingViewModel.getRpc());
            canvasPage = new CanvasPage(hostManager, userId, meetingViewModel.getRpc());
        } else {
            final CanvasState clientCanvasState = new CanvasState();
            final CanvasNetworkService networkService = new CanvasNetworkService(meetingViewModel.getRpc());
            final ClientActionManager clientManager = new ClientActionManager(userId, clientCanvasState,
                    networkService, meetingViewModel.getRpc());
            canvasPage = new CanvasPage(clientManager, userId, meetingViewModel.getRpc());
        }
        
        this.canvasPageReference = canvasPage; // Keep reference for updates

        stageContentPanel.add(wrap(screenNVideo), "MEETING");
        stageContentPanel.add(wrap(canvasPage), "CANVAS");
        card.add(stageTabs, BorderLayout.NORTH);
        card.add(stageContentPanel, BorderLayout.CENTER);
        stageTabs.setSelectedTab("MEETING");
        stageContentLayout.show(stageContentPanel, "MEETING");
        return card;
    }

    private JPanel wrap(final JPanel p) {
        final JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(p, BorderLayout.CENTER);
        w.setMinimumSize(new Dimension(0, 0));
        w.setBorder(new EmptyBorder(STAGE_CONTENT_BORDER, STAGE_CONTENT_BORDER,
                STAGE_CONTENT_BORDER, STAGE_CONTENT_BORDER));
        return w;
    }

    private void switchStageView(final String tabKey) {
        if (stageContentLayout == null || stageContentPanel == null) return;
        stageContentLayout.show(stageContentPanel, tabKey);
        if (stageTabs != null) stageTabs.setSelectedTab(tabKey);
    }

    private SoftCardPanel buildSidebarCard() {
        final SoftCardPanel sb = new SoftCardPanel(SIDEBAR_BLUR_RADIUS);
        sb.setLayout(new BorderLayout());
        sb.setPreferredSize(new Dimension(SIDEBAR_DEFAULT_WIDTH, 0));
        sb.setVisible(false);
        sb.setBorder(new EmptyBorder(18, 18, 18, 18));

        final JPanel chrome = new JPanel(new BorderLayout(SIDEBAR_LAYOUT_GAP, SIDEBAR_LAYOUT_GAP));
        chrome.setOpaque(false);

        final JPanel sidebarHeader = createSidebarHeader();

        // Internal tabs: Chat | Participants
        sidebarTabs = new JTabbedPane(SwingConstants.TOP);
        sidebarTabs.setOpaque(false);
        sidebarTabs.setUI(new ModernTabbedPaneUI());
        sidebarTabs.setBorder(new EmptyBorder(4, 4, 4, 4));

        chatPanel = createChatPanel();
        participantsPanel = createParticipantsPanel();
        sidebarTabs.addTab("Chat", chatPanel);
        sidebarTabs.addTab("Participants", participantsPanel);

        final JPanel bodyWrapper = new SidebarBodyPanel();
        bodyWrapper.setLayout(new BorderLayout());
        bodyWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        bodyWrapper.add(sidebarTabs, BorderLayout.CENTER);

        chrome.add(sidebarHeader, BorderLayout.NORTH);
        chrome.add(bodyWrapper, BorderLayout.CENTER);

        sb.add(chrome, BorderLayout.CENTER);
        return sb;
    }

    private JPanel createSidebarHeader() {
        final SoftCardPanel header = new SoftCardPanel(PANEL_BLUR_RADIUS);
        header.setCornerRadius(18);
        header.setLayout(new BorderLayout(12, 0));
        header.setBackground(SIDEBAR_HEADER_BACKGROUND);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        header.setOpaque(false);

        sidebarHeaderLabel = new JLabel("Collaboration Hub");
        sidebarHeaderLabel.setFont(FontUtil.getJetBrainsMono(TITLE_FONT_SIZE, Font.BOLD));
        sidebarHeaderLabel.setForeground(Color.WHITE);
        header.add(sidebarHeaderLabel, BorderLayout.WEST);
        header.add(createSidebarCloseButton(), BorderLayout.EAST);
        return header;
    }

    private JButton createSidebarCloseButton() {
        final JButton closeSidebarBtn = new JButton("✕");
        closeSidebarBtn.setToolTipText("Hide panel");
        closeSidebarBtn.setFocusPainted(false);
        closeSidebarBtn.setBorderPainted(false);
        closeSidebarBtn.setContentAreaFilled(false);
        closeSidebarBtn.setFont(FontUtil.getJetBrainsMono(DEFAULT_FONT_SIZE, Font.BOLD));
        closeSidebarBtn.setForeground(Color.WHITE);
        closeSidebarBtn.addActionListener(e -> toggleSidebarVisibility());
        return closeSidebarBtn;
    }

    private JPanel createParticipantsPanel() {
        final SoftCardPanel panel = new SoftCardPanel(PANEL_BLUR_RADIUS);
        panel.setLayout(new BorderLayout());
        final ParticipantsViewModel pvm = new ParticipantsViewModel(meetingViewModel);
        panel.add(new ParticipantsView(pvm), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChatPanel() {
        final SoftCardPanel panel = new SoftCardPanel(PANEL_BLUR_RADIUS);
        panel.setLayout(new BorderLayout());
        if (meetingViewModel == null || meetingViewModel.getRpc() == null) {
            final JLabel fallback = new JLabel("<html><center>Chat unavailable<br>(no RPC connection)</center></html>",
                    SwingConstants.CENTER);
            fallback.setFont(FontUtil.getJetBrainsMono(14.0f, Font.PLAIN));
            panel.add(fallback, BorderLayout.CENTER);
            return panel;
        }
        final ChatViewModel chatViewModel = new ChatViewModel(meetingViewModel.getRpc(),
                meetingViewModel.getCurrentUser());
        final ChatView chatView = new ChatView(chatViewModel);
        panel.add(chatView, BorderLayout.CENTER);
        return panel;
    }

    // ... (Sidebar logic: openSidebarToTab, toggleSidebarVisibility, etc. - Keep existing) ...
    private void openSidebarToTab(final String tabName) {
        final int widthHint = sidebarCard.isVisible() ? getCurrentSidebarWidth() : lastSidebarWidth;
        ensureSidebarShowing(widthHint > 0 ? widthHint : SIDEBAR_DEFAULT_WIDTH);
        for (int i = 0; i < sidebarTabs.getTabCount(); i++) {
            if (sidebarTabs.getTitleAt(i).equalsIgnoreCase(tabName)) {
                sidebarTabs.setSelectedIndex(i);
                break;
            }
        }
        if (sidebarHeaderLabel != null) sidebarHeaderLabel.setText(tabName);
        if (btnChat != null) btnChat.setActive("Chat".equalsIgnoreCase(tabName));
        if (btnPeople != null) btnPeople.setActive("Participants".equalsIgnoreCase(tabName));
        refreshLayoutContainers();
    }

    private void handleControlTabButton(final String tabName) {
        final String current = getCurrentSidebarTab();
        if (sidebarCard.isVisible() && current != null && tabName.equalsIgnoreCase(current)) {
            toggleSidebarVisibility();
        } else {
            openSidebarToTab(tabName);
        }
    }

    private String getCurrentSidebarTab() {
        if (sidebarTabs == null) return null;
        final int idx = sidebarTabs.getSelectedIndex();
        return (idx >= 0 && idx < sidebarTabs.getTabCount()) ? sidebarTabs.getTitleAt(idx) : null;
    }

    private void toggleSidebarVisibility() {
        if (sidebarCard.isVisible()) {
            captureSidebarWidth();
            sidebarCard.setVisible(false);
            sidebarVisible = false;
            if (btnChat != null) btnChat.setActive(false);
            if (btnPeople != null) btnPeople.setActive(false);
            collapseSidebarSpace();
        } else {
            ensureSidebarShowing(lastSidebarWidth > 0 ? lastSidebarWidth : SIDEBAR_DEFAULT_WIDTH);
        }
        refreshLayoutContainers();
    }

    private void ensureSidebarShowing(final int desiredWidth) {
        if (sidebarCard == null) return;
        sidebarCard.setVisible(true);
        sidebarVisible = true;
        setSidebarWidth(desiredWidth);
    }

    private void setSidebarWidth(final int desiredWidth) {
        if (centerSplit == null) return;
        if (centerSplit.getWidth() <= 0) {
            SwingUtilities.invokeLater(() -> setSidebarWidth(desiredWidth));
            return;
        }
        final int clamped = clampSidebarWidth(desiredWidth > 0 ? desiredWidth : SIDEBAR_DEFAULT_WIDTH, centerSplit.getWidth());
        setSidebarWidthInternal(clamped, centerSplit.getWidth());
    }

    private void setSidebarWidthInternal(final int sidebarWidth, final int totalWidth) {
        if (centerSplit == null) return;
        final int dividerSize = centerSplit.getDividerSize();
        int dividerLocation = Math.max(0, totalWidth - sidebarWidth - dividerSize);
        dividerLocation = Math.min(dividerLocation, centerSplit.getMaximumDividerLocation());
        centerSplit.setDividerLocation(dividerLocation);
        lastSidebarWidth = sidebarWidth;
    }

    private int clampSidebarWidth(final int desiredWidth, final int totalWidth) {
        final int dividerSize = centerSplit != null ? centerSplit.getDividerSize() : 0;
        final int availableForSidebar = Math.max(0, totalWidth - dividerSize);
        int stageMin = (stageCard != null && stageCard.getMinimumSize() != null) ? stageCard.getMinimumSize().width : 0;
        final int maxAllowable = Math.max(0, availableForSidebar - stageMin);
        final int minWidth = Math.min(SIDEBAR_MIN_WIDTH, maxAllowable);
        int maxWidth = Math.min(SIDEBAR_MAX_WIDTH, maxAllowable);
        if (maxWidth < minWidth) maxWidth = minWidth;
        int width = desiredWidth;
        if (width < minWidth) width = minWidth;
        if (width > maxWidth) width = maxWidth;
        return width;
    }

    private void captureSidebarWidth() {
        if (sidebarCard == null) return;
        final int width = getCurrentSidebarWidth();
        if (width > 0) lastSidebarWidth = width;
    }

    private int getCurrentSidebarWidth() {
        if (centerSplit == null) return lastSidebarWidth;
        final int totalWidth = centerSplit.getWidth();
        if (totalWidth <= 0) return lastSidebarWidth;
        return Math.max(0, totalWidth - centerSplit.getDividerLocation() - centerSplit.getDividerSize());
    }

    private void collapseSidebarSpace() {
        if (centerSplit != null) centerSplit.setDividerLocation(1.0);
    }

    private void refreshLayoutContainers() {
        if (sidebarCard != null && sidebarCard.getParent() != null) {
            sidebarCard.getParent().revalidate();
            sidebarCard.getParent().repaint();
        }
        if (stageCard != null) {
            stageCard.revalidate();
            stageCard.repaint();
        }
        SwingUtilities.invokeLater(() -> { revalidate(); repaint(); });
    }

    private void attachSplitListeners(final JSplitPane split) {
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> enforceSidebarBounds());
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) { enforceSidebarBounds(); }
        });
    }

    private void enforceSidebarBounds() {
        if (centerSplit == null || !sidebarCard.isVisible() || centerSplit.getWidth() <= 0) return;
        final int dividerLocation = centerSplit.getDividerLocation();
        if (dividerLocation < 0) return;
        final int currentWidth = centerSplit.getWidth() - dividerLocation - centerSplit.getDividerSize();
        final int clamped = clampSidebarWidth(currentWidth, centerSplit.getWidth());
        if (clamped != currentWidth) setSidebarWidthInternal(clamped, centerSplit.getWidth());
        else lastSidebarWidth = clamped;
    }

    private SoftCardPanel buildControlsBar() {
        final SoftCardPanel bar = new SoftCardPanel(CONTROLS_BLUR_RADIUS);
        bar.setCornerRadius(CONTROLS_CORNER_RADIUS);
        bar.setLayout(new BorderLayout());
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));
        bar.setBackground(CONTROLS_BAR_BACKGROUND);
        bar.setOpaque(false);

        // Create buttons
        btnMute = new MeetingControlButton("Mic", MeetingControlButton.ControlIcon.MIC);
        btnMute.setShowIcon(false);
        btnMute.setAccentColor(ACCENT_BLUE);
        btnMute.addActionListener(evt -> meetingViewModel.toggleAudio());
        btnCamera = new MeetingControlButton("Video", MeetingControlButton.ControlIcon.VIDEO);
        btnCamera.setAccentColor(ACCENT_BLUE);
        btnCamera.addActionListener(evt -> meetingViewModel.toggleVideo());
        btnShare = new MeetingControlButton("Share", MeetingControlButton.ControlIcon.SHARE);
        btnShare.setAccentColor(ACCENT_BLUE);
        btnShare.addActionListener(evt -> meetingViewModel.toggleScreenSharing());
        btnRaiseHand = new MeetingControlButton("Raise", MeetingControlButton.ControlIcon.HAND);
        btnRaiseHand.setAccentColor(ACCENT_BLUE);
        btnRaiseHand.addActionListener(evt -> toggleQuickDoubt());
        final boolean isInstructor = isCurrentUserInstructor();
        btnLeave = new MeetingControlButton(isInstructor ? "End" : "Leave", MeetingControlButton.ControlIcon.LEAVE);
        final Color leaveAccent = new Color(LEAVE_RED, LEAVE_GREEN, LEAVE_BLUE);
        btnLeave.setActiveColorOverride(new Color(LEAVE_RED, LEAVE_GREEN, LEAVE_BLUE, LEAVE_ALPHA));
        btnLeave.setAccentColor(new Color(220, 68, 55));
        btnLeave.addActionListener(e -> {
            meetingViewModel.endMeeting();
            App.getInstance(null).showView(App.MAIN_VIEW);
        });

        btnChat = new MeetingControlButton("Chat", MeetingControlButton.ControlIcon.CHAT);
        btnChat.setAccentColor(ACCENT_BLUE);
        btnChat.addActionListener(evt -> handleControlTabButton("Chat"));
        btnPeople = new MeetingControlButton("People", MeetingControlButton.ControlIcon.PEOPLE);
        btnPeople.setAccentColor(ACCENT_BLUE);
        btnPeople.addActionListener(evt -> handleControlTabButton("Participants"));
        final JPanel controlsContent = new JPanel(new BorderLayout());
        controlsContent.setOpaque(false);
        final JPanel primaryRow = createControlsRow(FlowLayout.LEFT);
        primaryRow.add(btnMute); primaryRow.add(btnCamera); primaryRow.add(btnShare); primaryRow.add(btnRaiseHand); primaryRow.add(btnLeave);
        final JPanel secondaryRow = createControlsRow(FlowLayout.RIGHT);
        secondaryRow.add(btnChat); secondaryRow.add(btnPeople);
        final JLabel controlsLabel = new JLabel("Meeting Controls");
        controlsLabel.setFont(FontUtil.getJetBrainsMono(DEFAULT_FONT_SIZE + 1, Font.BOLD));
        controlsLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        controlsContent.add(controlsLabel, BorderLayout.NORTH);
        controlsContent.add(primaryRow, BorderLayout.CENTER);
        controlsContent.add(secondaryRow, BorderLayout.EAST);
        bar.add(controlsContent, BorderLayout.CENTER);
        final Dimension controlsPref = controlsContent.getPreferredSize();
        final Dimension cardPref = new Dimension(controlsPref.width + 24, controlsPref.height + 16);
        bar.setPreferredSize(cardPref);
        bar.setMaximumSize(cardPref);

        return bar;
    }

    private JPanel createControlsRow(final int alignment) {
        final FlowLayout layout = new FlowLayout(alignment, CONTROLS_GAP_H, CONTROLS_GAP_V);
        final JPanel row = new JPanel(layout);
        row.setOpaque(false);
        return row;
    }

    private void setupBindings() {
        meetingViewModel.getIsVideoEnabled().addListener(PropertyListeners.onBooleanChanged(v -> SwingUtilities.invokeLater(() -> btnCamera.setActive(Boolean.TRUE.equals(v)))));
        meetingViewModel.getIsScreenShareEnabled().addListener(PropertyListeners.onBooleanChanged(v -> SwingUtilities.invokeLater(() -> btnShare.setActive(Boolean.TRUE.equals(v)))));
        meetingViewModel.getIsAudioEnabled().addListener(PropertyListeners.onBooleanChanged(v -> SwingUtilities.invokeLater(() -> btnMute.setActive(Boolean.TRUE.equals(v)))));
        meetingViewModel.getMeetingId().addListener(evt -> SwingUtilities.invokeLater(() -> {
            final String meetingId = meetingViewModel.getMeetingId().get();
            meetingIdBadge.setText((meetingId == null || meetingId.isEmpty()) ? "Meeting: --" : "Meeting: " + meetingId);
        }));
        meetingViewModel.getRole().addListener(evt -> SwingUtilities.invokeLater(() -> {
            roleLabel.setText(buildRoleLabelText());
            updateMeetingControlAvailability();
            updateLeaveButtonLabel();
        }));
        
        // NEW: Bind participant changes to canvas user updates
        meetingViewModel.getParticipants().addListener(PropertyListeners.onListChanged((List<UserProfile> participants) -> {
            // Iterate all participants and notify canvas
            // HostActionManager filters duplicates internally
            if (canvasPageReference != null) {
                for (UserProfile p : participants) {
                    canvasPageReference.onUserJoined(p.getEmail());
                }
            }
        }));
    }

    private void registerThemeListener() {
        try {
            if (ThemeManager.getInstance() != null) {
                ThemeManager.getInstance().addThemeChangeListener(() -> SwingUtilities.invokeLater(() -> {
                    try {
                        if (sidebarTabs != null) { sidebarTabs.setUI(new ModernTabbedPaneUI()); sidebarTabs.revalidate(); sidebarTabs.repaint(); }
                        applyTheme();
                    } catch (Throwable ignored) {}
                }));
            }
        } catch (Throwable ignored) {}
    }

    private void startLiveClock() {
        if (liveTimer != null) liveTimer.stop();
        liveTimer = new Timer(TIMER_DELAY_MS, e -> liveClockLabel.setText("Live: " + new SimpleDateFormat("hh:mm:ss a").format(new Date())));
        liveTimer.setInitialDelay(0);
        liveTimer.start();
    }

    private void updateLeaveButtonLabel() {
        if (btnLeave != null) {
            btnLeave.setText(isCurrentUserInstructor() ? "End" : "Leave");
        }
    }

    private boolean isCurrentUserInstructor() {
        return meetingViewModel != null && meetingViewModel.getCurrentUser() != null && meetingViewModel.getCurrentUser().getRole() == ParticipantRole.INSTRUCTOR;
    }

    private void updateMeetingControlAvailability() {
        if (meetingControlsButton == null) return;
        final boolean instructor = isCurrentUserInstructor();
        meetingControlsButton.setEnabled(instructor);
        meetingControlsButton.setToolTipText(instructor ? "Open advanced meeting controls" : "Available only for instructors");
    }

    private void openMeetingControlsDialog() {
        if (!isCurrentUserInstructor()) {
            JOptionPane.showMessageDialog(this, "Only instructors can access meeting controls.", "Access Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final JPopupMenu menu = new JPopupMenu();
        menu.add(createPermissionToggle("Allow participants to chat", allowParticipantChat, selected -> allowParticipantChat = selected));
        menu.add(createPermissionToggle("Allow participants to unmute", allowParticipantUnmute, selected -> allowParticipantUnmute = selected));
        menu.add(createPermissionToggle("Allow participants to share screen", allowParticipantShare, selected -> allowParticipantShare = selected));
        menu.show(meetingControlsButton, 0, meetingControlsButton.getHeight());
    }

    private JCheckBoxMenuItem createPermissionToggle(final String label, final boolean initial, final Consumer<Boolean> onToggle) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, initial);
        item.addActionListener(e -> onToggle.accept(item.isSelected()));
        return item;
    }

    private void copyMeetingId() {
        final String id = meetingViewModel.getMeetingId().get();
        if (id != null && !id.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(id), null);
            btnCopyLink.setText("Copied!");
            btnCopyLink.setEnabled(false);
            final Timer resetTimer = new Timer(COPY_FEEDBACK_DELAY_MS, e -> { btnCopyLink.setText("Copy Link"); btnCopyLink.setEnabled(true); });
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
    }

    private void toggleQuickDoubt() {
        isHandRaised = !isHandRaised;
        btnRaiseHand.setActive(isHandRaised);
        if (isHandRaised) quickDoubtPopup.showAbove(btnRaiseHand);
        else { quickDoubtPopup.setVisible(false); quickDoubtPopup.reset(); }
    }

    private void handleQuickDoubtClosed() {
        if (isHandRaised) {
            isHandRaised = false;
            if (btnRaiseHand != null) {
                btnRaiseHand.setActive(false);
            }
        }
    }

    private void handleQuickDoubtSent(final String message) {
        if (meetingViewModel != null) {
            meetingViewModel.submitQuickDoubt(message);
        }
        SwingUtilities.invokeLater(() -> {
            quickDoubtPopup.setVisible(false);
            quickDoubtPopup.reset();
            handleQuickDoubtClosed();
        });
    }

    private static final class SidebarBodyPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        SidebarBodyPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SIDEBAR_BODY_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                    SIDEBAR_BODY_RADIUS, SIDEBAR_BODY_RADIUS);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void applyTheme() {
        try {
            if (ThemeManager.getInstance() != null) {
                final com.swe.ux.theme.Theme theme = ThemeManager.getInstance().getCurrentTheme();
                if (theme != null) {
                    setBackground(theme.getBackgroundColor());
                    ThemeManager.getInstance().applyThemeRecursively(headerCard);
                    ThemeManager.getInstance().applyThemeRecursively(stageCard);
                    ThemeManager.getInstance().applyThemeRecursively(sidebarCard);
                    ThemeManager.getInstance().applyThemeRecursively(controlsBar);
                    if (centerSplit != null) centerSplit.setBackground(theme.getBackgroundColor());
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void updateUI() {
        super.updateUI();
        SwingUtilities.invokeLater(() -> {
            try {
                if (sidebarTabs != null) { sidebarTabs.setUI(new ModernTabbedPaneUI()); sidebarTabs.revalidate(); sidebarTabs.repaint(); }
                applyTheme();
            } catch (Throwable ignored) {}
        });
    }

    private static class SidebarToggleIcon implements Icon {
        private static final int SIZE = ICON_SIZE;
        @Override public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ThemeManager.getInstance().getCurrentTheme().getTextColor());
            for (int i = 0; i < 3; i++) {
                g2.fillRoundRect(x + i * (ICON_BAR_WIDTH + ICON_SPACING), y + ICON_VERTICAL_OFFSET, ICON_BAR_WIDTH, SIZE - ICON_HEIGHT_ADJUST, ICON_ROUND_CORNER, ICON_ROUND_CORNER);
            }
            g2.dispose();
        }
        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }

    private String buildRoleLabelText() {
        String role = meetingViewModel.getRole().get();
        if (role == null || role.isEmpty()) {
            if (meetingViewModel.getCurrentUser() != null && meetingViewModel.getCurrentUser().getRole() != null) {
                role = meetingViewModel.getCurrentUser().getRole().name();
            }
        }
        return "Role: " + ((role == null || role.isEmpty()) ? "Guest" : role);
    }
}