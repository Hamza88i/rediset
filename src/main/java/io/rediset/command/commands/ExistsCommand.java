package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code EXISTS key [key ...]} — returns the number of the given keys that exist.
 * A key listed multiple times is counted multiple times.
 */
public final class ExistsCommand implements Command {

    @Override
    public String name() {
        return "EXISTS";
    }

    @Override
    public int arity() {
        return -2; // EXISTS + at least one key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        long count = 0;
        for (int i = 0; i < command.arguments().size(); i++) {
            if (context.storage().exists(command.argAsString(i))) {
                count++;
            }
        }
        return Reply.integer(count);
    }
}
