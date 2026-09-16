package io.rediset.command;

import io.rediset.command.commands.DbSizeCommand;
import io.rediset.command.commands.DelCommand;
import io.rediset.command.commands.EchoCommand;
import io.rediset.command.commands.ExistsCommand;
import io.rediset.command.commands.GetCommand;
import io.rediset.command.commands.KeysCommand;
import io.rediset.command.commands.PingCommand;
import io.rediset.command.commands.SetCommand;
import io.rediset.command.commands.TypeCommand;

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
        registerKeyCommands(registry);
        return registry;
    }

    private static void registerConnectionCommands(CommandRegistry registry) {
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
    }

    private static void registerKeyCommands(CommandRegistry registry) {
        registry.register(new SetCommand());
        registry.register(new GetCommand());
        registry.register(new DelCommand());
        registry.register(new ExistsCommand());
        registry.register(new KeysCommand());
        registry.register(new DbSizeCommand());
        registry.register(new TypeCommand());
    }
}
