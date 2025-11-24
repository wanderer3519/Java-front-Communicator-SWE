/*
 * -----------------------------------------------------------------------------
 * File: NetworkSimulator.java
 * Owner: B S S Krishna
 * Roll Number: 112201013
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.collaboration;

import java.util.ArrayList;
import java.util.List;

import com.swe.canvas.datamodel.manager.ActionManager;

/**
 * A network stub that simulates the Host-Client connection in memory.
 *
 * <p>This class implements {@link NetworkService} by holding direct references
 * to the Host and Client managers. Calls to send/broadcast directly invoke
 * the `processIncomingMessage` methods on the recipients, simulating instantaneous
 * network transmission.</p>
 */
public class NetworkSimulator implements NetworkService {

    /** Reference to the Host manager. */
    private ActionManager hostManager;

    /** List of registered Client managers. */
    private final List<ActionManager> clientManagers = new ArrayList<>();

    @Override
    public void registerHost(final ActionManager host) {
        this.hostManager = host;
    }

    @Override
    public void registerClient(final ActionManager client) {
        this.clientManagers.add(client);
    }

    @Override
    public void sendMessageToHost(final NetworkMessage message) {
        // System.out.println("[NETWORK] Client -> Host: " + message.getMessageType());
        if (hostManager != null) {
            hostManager.processIncomingMessage(message);
        }
    }

    @Override
    public void broadcastMessage(final NetworkMessage message) {
        // System.out.println("[NETWORK] Host -> ALL Clients: " + message.getMessageType());
        for (ActionManager client : clientManagers) {
            client.processIncomingMessage(message);
        }
    }
}