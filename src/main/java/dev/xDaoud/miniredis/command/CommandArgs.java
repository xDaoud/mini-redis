package dev.xDaoud.miniredis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * A parsed command. argv[0] is the command name, matching the Redis convention.
 * Arguments stay as byte[] so binary values survive intact.
 */
public record CommandArgs(List<byte[]> argv) {

    public int size() {
        return argv.size();
    }

    /** Raw bytes of argument i. Use this for values. */
    public byte[] raw(int i) {
        return argv.get(i);
    }

    /** Argument i decoded as UTF-8. Use this for names and keys only. */
    public String str(int i) {
        return new String(argv.get(i), StandardCharsets.UTF_8);
    }

    /** The command name, uppercased for case-insensitive lookup. */
    public String name() {
        return str(0).toUpperCase(Locale.ROOT);
    }
}