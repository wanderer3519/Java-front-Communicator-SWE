package com.swe.module_canvas.canvas.tool;

import java.net.http.WebSocket.Listener;

/**
 * Tool interface.
 */
public interface Tool {
    void onPointerDown(Listener e);

    void onPointerUp(Listener e);

    void onPointerMove(Listener e);
}
