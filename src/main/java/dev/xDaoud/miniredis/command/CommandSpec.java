package dev.xDaoud.miniredis.command;


/**
 * @param arity positive = exactly this many arguments (including the name);
 *              negative = at least this many. Mirrors the Redis command table.
 */
public record CommandSpec(String name, int arity, Command handler) {

    public boolean arityMatches(int argc) {
        return arity >= 0 ? argc == arity : argc >= -arity;
    }
}