package io.rediset.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.command.commands.PingCommand;
import io.rediset.exception.UnknownCommandException;
import org.junit.jupiter.api.Test;

class CommandRegistryTest {

    @Test
    void shouldFindRegisteredCommandCaseInsensitively() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new PingCommand());
        assertTrue(registry.find("ping").isPresent());
        assertTrue(registry.find("PING").isPresent());
        assertTrue(registry.contains("PiNg"));
    }

    @Test
    void shouldReturnEmptyForUnknownCommand() {
        CommandRegistry registry = new CommandRegistry();
        assertFalse(registry.find("nope").isPresent());
    }

    @Test
    void shouldThrowOnGetForUnknownCommand() {
        CommandRegistry registry = new CommandRegistry();
        assertThrows(UnknownCommandException.class, () -> registry.get("nope"));
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new PingCommand());
        assertThrows(IllegalStateException.class, () -> registry.register(new PingCommand()));
    }

    @Test
    void factoryShouldRegisterBuiltInCommands() {
        CommandRegistry registry = CommandRegistryFactory.createDefault();
        for (String name : new String[] {
                "PING", "ECHO", "SET", "GET", "DEL", "EXISTS", "KEYS", "DBSIZE", "TYPE",
                "EXPIRE", "PEXPIRE", "TTL", "PTTL", "PERSIST"}) {
            assertTrue(registry.contains(name), () -> "missing command: " + name);
        }
        assertTrue(registry.size() >= 14,
                () -> "expected at least 14 commands, got " + registry.size());
    }
}
