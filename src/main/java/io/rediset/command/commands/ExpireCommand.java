package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code EXPIRE key seconds} — sets a time-to-live, in seconds, on {@code key}.
 * A zero or negative TTL deletes the key immediately. Replies {@code :1} if the
 * timeout was set (or the key deleted), {@code :0} if the key does not exist.
 */
public final class ExpireCommand implements Command {

    private static final long MILLIS_PER_SECOND = 1000L;

    @Override
    public String name() {
        return "EXPIRE";
    }

    @Override
    public int arity() {
        return 3; // EXPIRE key seconds
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        long seconds = Arguments.parseLong(command.argAsString(1));
        boolean applied = context.storage().setExpiryAfterMillis(key, seconds * MILLIS_PER_SECOND);
        return Reply.integer(applied ? 1 : 0);
    }
}
