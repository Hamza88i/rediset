package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code PING [message]} — replies {@code PONG}, or echoes {@code message} back
 * as a bulk string when one is supplied.
 */
public final class PingCommand implements Command {

    @Override
    public String name() {
        return "PING";
    }

    @Override
    public int arity() {
        // At least the name; optionally one message argument.
        return -1;
    }

    @Override
    public void validate(ParsedCommand command) {
        if (command.tokenCount() > 2) {
            throw new io.rediset.exception.CommandArityException(name());
        }
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        if (command.arguments().isEmpty()) {
            return Reply.simple("PONG");
        }
        return Reply.bulk(command.arg(0));
    }
}
