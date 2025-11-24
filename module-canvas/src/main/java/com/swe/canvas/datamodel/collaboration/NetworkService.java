/*
 * -----------------------------------------------------------------------------
 * File: NetworkService.java
 * Owner: B S S Krishna
 * Roll Number: 112201013
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.collaboration;

import com.swe.canvas.datamodel.manager.ActionManager;

/**
 * Abstract interface for the network layer.
 *
 * <p>
 * This interface defines the contract for sending messages between the
 * Client and the Host. It abstracts the underlying transport mechanism
 * (e.g., sockets, WebSockets, or in-memory simulation).
 * </p>
 */
public interface NetworkService {

    /**
     * Sends a message from a Client to the Host.
     *
     * @param message The message to send.
     */
    void sendMessageToHost(NetworkMessage message);

    /**
     * Broadcasts a message from the Host to all connected Clients.
     *
     * @param message The message to broadcast.
     */
    void broadcastMessage(NetworkMessage message);

    /**
     * Registers the Host's ActionManager to handle incoming messages.
     *
     * @param host The HostActionManager instance.
     */
    void registerHost(ActionManager host);

    /**
     * Registers a Client's ActionManager to handle incoming messages.
     *
     * @param client The ClientActionManager instance.
     */
    void registerClient(ActionManager client);
}