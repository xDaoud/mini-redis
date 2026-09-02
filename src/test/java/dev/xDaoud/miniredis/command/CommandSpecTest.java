package dev.xDaoud.miniredis.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandSpecTest {

    private static CommandSpec spec(int arity) {
        return new CommandSpec("X", arity, args -> null);
    }

    @Test void exactArityAcceptsOnlyThatCount() {
        assertTrue(spec(2).arityMatches(2));
        assertFalse(spec(2).arityMatches(1));
        assertFalse(spec(2).arityMatches(3));
    }

    @Test void negativeArityIsAMinimum() {
        assertFalse(spec(-2).arityMatches(1));
        assertTrue(spec(-2).arityMatches(2));
        assertTrue(spec(-2).arityMatches(99));
    }
}