package io.rediset.command;

import io.rediset.exception.CommandException;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Looks up, validates, and runs a command, translating command-level failures
 * into protocol error replies. Unexpected runtime failures are logged and turned
 * into a generic internal-error reply so a single bad command never crashes the
 * connection or the server.
 */
public final class CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutor.class);

    private final CommandRegistry registry;

    public CommandExecutor(CommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executes a parsed command in the given context and returns the reply.
     * This method never throws for client-caused problems; it returns an error
     * reply instead.
     */
    public Reply execute(CommandContext context, ParsedCommand parsed) {
        Command command;
        try {
            command = registry.get(parsed.name());
        } catch (CommandException e) {
            return Reply.error(e.toReplyMessage());
        }

        try {
            command.validate(parsed);
            return command.execute(context, parsed);
        } catch (CommandException e) {
            return Reply.error(e.toReplyMessage());
        } catch (RuntimeException e) {
            log.error("Internal error executing command {}", parsed.name(), e);
            return Reply.error("ERR internal error");
        }
    }
}
