package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code PERSIST key} — removes the expiry from {@code key}, making it persistent.
 * Replies {@code :1} if an expiry was removed, {@code :0} if the key does not exist
 * or had no expiry.
 */
public final class PersistCommand implements Command {

    @Override
    public String name() {
        return "PERSIST";
    }

    @Override
    public int arity() {
        return 2; // PERSIST key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        boolean removed = context.storage().persist(command.argAsString(0));
        return Reply.integer(removed ? 1 : 0);
    }
}
