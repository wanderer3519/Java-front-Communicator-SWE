package com.swe.module_canvas.canvas.command;

/**
 * Central interface for a command.
 */
public interface Command {
    void execute();

    void unExecute();
}
