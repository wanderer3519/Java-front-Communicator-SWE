/*
 * -----------------------------------------------------------------------------
 * File: NetworkSimulatorTest.java
 * Owner: B S S Krishna
 * Roll Number: 112201013
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.collaboration;

import com.swe.canvas.datamodel.manager.ActionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the NetworkSimulator.
 */
class NetworkSimulatorTest {

    private NetworkSimulator simulator;
    private ActionManager mockHost;
    private ActionManager mockClient1;
    private ActionManager mockClient2;

    @BeforeEach
    void setUp() {
        simulator = new NetworkSimulator();
        mockHost = mock(ActionManager.class);
        mockClient1 = mock(ActionManager.class);
        mockClient2 = mock(ActionManager.class);

        simulator.registerHost(mockHost);
        simulator.registerClient(mockClient1);
        simulator.registerClient(mockClient2);
    }

    @Test
    void testSendMessageToHost() {
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, new byte[]{1});
        
        // Act
        simulator.sendMessageToHost(msg);
        
        // Assert
        verify(mockHost, times(1)).processIncomingMessage(msg);
        // Clients should NOT receive direct messages sent to host
        verify(mockClient1, never()).processIncomingMessage(any());
    }

    @Test
    void testSendMessageToHost_NoHostRegistered() {
        NetworkSimulator emptySim = new NetworkSimulator();
        NetworkMessage msg = new NetworkMessage(MessageType.NORMAL, new byte[]{1});
        
        // Should not crash
        emptySim.sendMessageToHost(msg);
    }

    @Test
    void testBroadcastMessage() {
        NetworkMessage msg = new NetworkMessage(MessageType.REDO, new byte[]{2});
        
        // Act
        simulator.broadcastMessage(msg);
        
        // Assert
        verify(mockClient1, times(1)).processIncomingMessage(msg);
        verify(mockClient2, times(1)).processIncomingMessage(msg);
        
        // Host doesn't receive its own broadcast in this implementation (commented out in source)
        verify(mockHost, never()).processIncomingMessage(msg);
    }
}