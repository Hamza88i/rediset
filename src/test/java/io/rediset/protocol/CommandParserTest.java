package io.rediset.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandParserTest {

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldUpperCaseCommandName() {
        ParsedCommand cmd = CommandParser.parse(List.of(b("get"), b("key")));
        assertEquals("GET", cmd.name());
    }

    @Test
    void shouldSeparateArgumentsFromName() {
        ParsedCommand cmd = CommandParser.parse(List.of(b("SET"), b("k"), b("v")));
        assertEquals(2, cmd.arguments().size());
        assertEquals("k", cmd.argAsString(0));
        assertEquals("v", cmd.argAsString(1));
        assertEquals(3, cmd.tokenCount());
    }

    @Test
    void shouldHandleCommandWithNoArguments() {
        ParsedCommand cmd = CommandParser.parse(List.of(b("PING")));
        assertEquals("PING", cmd.name());
        assertEquals(0, cmd.arguments().size());
        assertEquals(1, cmd.tokenCount());
    }

    @Test
    void shouldRejectEmptyTokens() {
        assertThrows(ProtocolException.class, () -> CommandParser.parse(List.of()));
    }
}
