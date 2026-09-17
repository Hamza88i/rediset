package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code PTTL key} — like {@code TTL} but returns the remaining time-to-live in
 * milliseconds. Returns {@code -2} if the key does not exist, {@code -1} if it has
 * no expiry.
 */
public final class PttlCommand implements Command {

    @Override
    public String name() {
        return "PTTL";
    }

    @Override
    public int arity() {
        return 2; // PTTL key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        return Reply.integer(context.storage().ttlMillis(command.argAsString(0)));
    }
}
