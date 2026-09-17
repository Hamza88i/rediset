package io.rediset.command;

import io.rediset.command.commands.DbSizeCommand;
import io.rediset.command.commands.DelCommand;
import io.rediset.command.commands.EchoCommand;
import io.rediset.command.commands.ExistsCommand;
import io.rediset.command.commands.ExpireCommand;
import io.rediset.command.commands.GetCommand;
import io.rediset.command.commands.KeysCommand;
import io.rediset.command.commands.LindexCommand;
import io.rediset.command.commands.LlenCommand;
import io.rediset.command.commands.LpopCommand;
import io.rediset.command.commands.LpushCommand;
import io.rediset.command.commands.LrangeCommand;
import io.rediset.command.commands.PersistCommand;
import io.rediset.command.commands.PexpireCommand;
import io.rediset.command.commands.PingCommand;
import io.rediset.command.commands.PttlCommand;
import io.rediset.command.commands.RpopCommand;
import io.rediset.command.commands.RpushCommand;
import io.rediset.command.commands.SetCommand;
import io.rediset.command.commands.TtlCommand;
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
        registerExpirationCommands(registry);
        registerListCommands(registry);
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

    private static void registerExpirationCommands(CommandRegistry registry) {
        registry.register(new ExpireCommand());
        registry.register(new PexpireCommand());
        registry.register(new TtlCommand());
        registry.register(new PttlCommand());
        registry.register(new PersistCommand());
    }

    private static void registerListCommands(CommandRegistry registry) {
        registry.register(new LpushCommand());
        registry.register(new RpushCommand());
        registry.register(new LpopCommand());
        registry.register(new RpopCommand());
        registry.register(new LrangeCommand());
        registry.register(new LlenCommand());
        registry.register(new LindexCommand());
    }
}
