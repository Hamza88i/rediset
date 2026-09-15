package io.rediset.command;

import io.rediset.command.commands.EchoCommand;
import io.rediset.command.commands.PingCommand;

/**
 * Builds a fully-populated {@link CommandRegistry}. Centralizing registration
 * here keeps command wiring in one place and out of the server startup code.
 */
public final class CommandRegistryFactory {

    private CommandRegistryFactory() {
    }

    /** Creates a registry containing all built-in commands. */
    public static CommandRegistry createDefault() {
        CommandRegistry registry = new CommandRegistry();
        registerConnectionCommands(registry);
        return registry;
    }

    private static void registerConnectionCommands(CommandRegistry registry) {
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
    }
}
