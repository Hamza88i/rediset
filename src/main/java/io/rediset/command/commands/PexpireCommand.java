package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code PEXPIRE key milliseconds} — like {@code EXPIRE} but the TTL is given in
 * milliseconds. A zero or negative TTL deletes the key immediately.
 */
public final class PexpireCommand implements Command {

    @Override
    public String name() {
        return "PEXPIRE";
    }

    @Override
    public int arity() {
        return 3; // PEXPIRE key milliseconds
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        long millis = Arguments.parseLong(command.argAsString(1));
        boolean applied = context.storage().setExpiryAfterMillis(key, millis);
        return Reply.integer(applied ? 1 : 0);
    }
}
