package com.swe.controller;

import com.socketry.SocketryServer;
import com.swe.controller.RPCinterface.AbstractRPC;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class RPC implements AbstractRPC {

    HashMap<String, Function<byte[], byte[]>> methods;

    private SocketryServer socketryServer;

    private static AbstractRPC s_instance = null;

    private RPC() {
        methods = new HashMap<>();
    }

    
    /**
     * Get the singleton instance of the RPC manager.
     *
     * @return The singleton RPC instance.
     */
    public static AbstractRPC getInstance() {
        if (s_instance == null) {
            s_instance = new RPC();
        }

        return s_instance;

    }

    public void subscribe(final String methodName, final Function<byte[], byte[]> method) {
        methods.put(methodName, method);
    }

    public Thread connect(final int portNumber) throws IOException, InterruptedException, ExecutionException {
        System.out.println("Connecting to port: " + portNumber + " " + methods.keySet());
        socketryServer = new SocketryServer(portNumber, methods);
        final Thread rpcThread = new Thread(socketryServer::listenLoop);
        rpcThread.start();
        return rpcThread;
    }

    public CompletableFuture<byte[]> call(final String methodName, final byte[] data) {
        System.out.println("Calling method: " + methodName);
        final byte methodId = socketryServer.getRemoteProcedureId(methodName);
        System.out.println("Method ID: " + methodId);
        try {
            return socketryServer.makeRemoteCall(methodId, data, 0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
