package dev.xDaoud.miniredis.command;

import dev.xDaoud.miniredis.resp.RespValue;

@FunctionalInterface
public interface Command {
    RespValue execute(CommandArgs args);
}