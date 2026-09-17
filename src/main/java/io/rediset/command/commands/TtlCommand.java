package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code TTL key} — returns the remaining time-to-live in whole seconds, or
 * {@code -2} if the key does not exist, {@code -1} if it exists but has no expiry.
 */
public final class TtlCommand implements Command {

    private static final long MILLIS_PER_SECOND = 1000L;

    @Override
    public String name() {
        return "TTL";
    }

    @Override
    public int arity() {
        return 2; // TTL key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        long millis = context.storage().ttlMillis(command.argAsString(0));
        if (millis < 0) {
            return Reply.integer(millis); // -1 or -2 sentinel passed through
        }
        // Round up so a sub-second remainder still reports at least 1 second.
        long seconds = (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND;
        return Reply.integer(seconds);
    }
}
