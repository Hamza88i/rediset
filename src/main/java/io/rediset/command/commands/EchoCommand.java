package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code ECHO message} — replies with {@code message} as a bulk string.
 */
public final class EchoCommand implements Command {

    @Override
    public String name() {
        return "ECHO";
    }

    @Override
    public int arity() {
        return 2; // ECHO + message
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        return Reply.bulk(command.arg(0));
    }
}
