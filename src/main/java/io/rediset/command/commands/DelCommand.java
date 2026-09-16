package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code DEL key [key ...]} — deletes the given keys, ignoring keys that do not
 * exist. Replies with the number of keys actually removed.
 */
public final class DelCommand implements Command {

    @Override
    public String name() {
        return "DEL";
    }

    @Override
    public int arity() {
        return -2; // DEL + at least one key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        long removed = 0;
        for (int i = 0; i < command.arguments().size(); i++) {
            if (context.storage().delete(command.argAsString(i))) {
                removed++;
            }
        }
        return Reply.integer(removed);
    }
}
