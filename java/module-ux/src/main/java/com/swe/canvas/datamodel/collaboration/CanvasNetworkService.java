/*
 * -----------------------------------------------------------------------------
 * File: CanvasNetworkService.java
 * Owner: Bhogaraju Shanmukha Sri Krishna
 * Roll Number: 112201013
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.collaboration;

import com.swe.canvas.datamodel.serialization.JsonUtils;
import com.swe.controller.RPC;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.networking.ClientNode;
import com.swe.networking.NetworkFront;
import java.nio.charset.StandardCharsets;

/**
 * NetworkService implementation that relies on the networking module + RPC bridge.
 */
public class CanvasNetworkService implements NetworkService {

    /**
     * Expected number of parts when splitting client node data.
     */
    private static final int ADDR_PARTS = 2;

    /**
     * Index for the IP address in the split array.
     */
    private static final int IP_INDEX = 0;

    /**
     * Index for the Port in the split array.
     */
    private static final int PORT_INDEX = 1;

    /**
     * The network front instance used for network operations.
     */
    private final NetworkFront network;

    /**
     * The abstract RPC instance used for remote procedure calls.
     */
    private final AbstractRPC rpc;

    /**
     * Constructs a new CanvasNetworkService with the default NetworkFront instance.
     *
     * @param rpcBridge The AbstractRPC instance to use for communication.
     */
    public CanvasNetworkService(final AbstractRPC rpcBridge) {
        this(rpcBridge, NetworkFront.getInstance());
    }

    /**
     * Constructs a new CanvasNetworkService with a specific NetworkFront instance.
     *
     * @param rpcBridge    The AbstractRPC instance to use.
     * @param networkFront The NetworkFront instance to use.
     */
    public CanvasNetworkService(final AbstractRPC rpcBridge, final NetworkFront networkFront) {
        if (rpcBridge != null) {
            this.rpc = rpcBridge;
        } else {
            this.rpc = RPC.getInstance();
        }

        this.network = networkFront;
    }

    ClientNode deserializeClientNodee(final byte[] data) {
        final String dataStr = new String(data);
        final String[] parts = dataStr.split(":");
        if (parts.length != ADDR_PARTS) {
            throw new IllegalArgumentException("Invalid ClientNode data: " + dataStr);
        }
        final String ip = parts[IP_INDEX];
        final int port = Integer.parseInt(parts[PORT_INDEX]);
        return new ClientNode(ip, port);
    }

    @Override
    public void sendMessageToHost(final NetworkMessage message) {
        final String serializedMessage = message.serialize();
        // System.out.println("[CanvasNetworkService] sendMessageToHost called. Type="
        //        + message.getMessageType() + ", bytes=" + serializedMessage.length());

        if (this.rpc != null) {
            this.rpc.call("canvas:sendToHost", serializedMessage.getBytes(StandardCharsets.UTF_8))
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            System.err.println("[CanvasNetworkService] sendMessageToHost failed: " + err.getMessage());
                        }
                    });
        } else {
            System.err.println("[CanvasNetworkService] RPC instance is null; cannot send to host.");
        }
    }

    @Override
    public void broadcastMessage(final NetworkMessage message) {
        final String serializedMessage = message.serialize();
        // System.out.println("[CanvasNetworkService] broadcastMessage called. Type="
        //        + message.getMessageType() + ", bytes=" + serializedMessage.length());

        if (this.rpc != null) {
            this.rpc.call("canvas:broadcast", serializedMessage.getBytes())
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            System.err.println("[CanvasNetworkService] broadcastMessage failed: " + err.getMessage());
                        }
                    });
        } else {
            System.err.println("[CanvasNetworkService] RPC instance is null; cannot broadcast.");
        }
    }

    @Override
    public void sendToClient(final NetworkMessage message, final String targetClientId) {
        if (targetClientId == null || targetClientId.isEmpty()) {
            return;
        }

        final String serializedMessage = message.serialize();

        // Construct JSON payload: { "target": "email", "data": "serialized_msg" }
        // We use manual string concatenation to match JsonUtils style/avoid extra dependencies here
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(JsonUtils.jsonEscape("target")).append(":")
            .append(JsonUtils.jsonEscape(targetClientId));
        sb.append(",");
        sb.append(JsonUtils.jsonEscape("data")).append(":")
            .append(JsonUtils.jsonEscape(serializedMessage));
        sb.append("}");

        final String payload = sb.toString();

        System.out.println("[CanvasNetworkService] sendToClient called for " + targetClientId);

        if (this.rpc != null) {
            this.rpc.call("canvas:sendToClient", payload.getBytes(StandardCharsets.UTF_8))
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            System.err.println("[CanvasNetworkService] sendToClient failed: " + err.getMessage());
                        } else {
                            System.out.println("[CanvasNetworkService] sendToClient delivered to " + targetClientId);
                        }
                    });
        } else {
            System.err.println("[CanvasNetworkService] RPC instance is null; cannot send to client.");
        }
    }
}