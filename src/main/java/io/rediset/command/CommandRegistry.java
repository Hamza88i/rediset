package io.rediset.command;

import io.rediset.exception.UnknownCommandException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A name-to-{@link Command} lookup table. Using a map keyed by the upper-cased
 * command name avoids a giant {@code if/else} or {@code switch} in the executor
 * and lets new commands be registered without touching dispatch code.
 *
 * <p>The registry is populated at startup and then read concurrently by many
 * connection threads; a {@link ConcurrentHashMap} backs it for safe concurrent
 * reads.
 */
public final class CommandRegistry {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    /** Registers a command under its {@link Command#name()}. */
    public CommandRegistry register(Command command) {
        String key = command.name().toUpperCase(Locale.ROOT);
        if (commands.putIfAbsent(key, command) != null) {
            throw new IllegalStateException("duplicate command registration: " + key);
        }
        return this;
    }

    /** Looks up a command by name (case-insensitive), if present. */
    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(name.toUpperCase(Locale.ROOT)));
    }

    /**
     * Looks up a command by name, throwing {@link UnknownCommandException} if it
     * is not registered.
     */
    public Command get(String name) {
        Command command = commands.get(name.toUpperCase(Locale.ROOT));
        if (command == null) {
            throw new UnknownCommandException(name);
        }
        return command;
    }

    public boolean contains(String name) {
        return commands.containsKey(name.toUpperCase(Locale.ROOT));
    }

    public int size() {
        return commands.size();
    }

    /** Returns all registered commands (unspecified order). */
    public Collection<Command> all() {
        return commands.values();
    }
}
