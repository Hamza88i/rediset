package io.rediset.command;

import io.rediset.exception.CommandArityException;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * A single RediSet command (for example {@code GET} or {@code PING}).
 *
 * <p>Commands are registered in the {@link CommandRegistry} by name and are
 * stateless singletons: all per-request state travels through the
 * {@link CommandContext} and {@link ParsedCommand} arguments, so a single
 * instance safely serves many concurrent connections.
 */
public interface Command {

    /** The canonical upper-case command name used for registry lookup. */
    String name();

    /**
     * The command arity. A positive value {@code n} means exactly {@code n}
     * tokens (including the command name). A negative value {@code -n} means at
     * least {@code n} tokens. This mirrors a common convention for variadic
     * commands.
     */
    int arity();

    /**
     * Executes the command and returns the reply to send to the client.
     *
     * @param context the execution context
     * @param command the parsed request (name plus arguments)
     * @return the reply
     */
    Reply execute(CommandContext context, ParsedCommand command);

    /**
     * Validates the argument count against {@link #arity()}. Called by the
     * executor before {@link #execute}. Commands with more elaborate validation
     * may override this, but must still enforce arity.
     *
     * @throws CommandArityException if the token count does not satisfy the arity
     */
    default void validate(ParsedCommand command) {
        int arity = arity();
        int tokens = command.tokenCount();
        boolean ok = arity >= 0 ? tokens == arity : tokens >= -arity;
        if (!ok) {
            throw new CommandArityException(name());
        }
    }
}
