package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code DBSIZE} — returns the number of keys currently stored, as an integer.
 */
public final class DbSizeCommand implements Command {

    @Override
    public String name() {
        return "DBSIZE";
    }

    @Override
    public int arity() {
        return 1; // DBSIZE
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        return Reply.integer(context.storage().size());
    }
}
