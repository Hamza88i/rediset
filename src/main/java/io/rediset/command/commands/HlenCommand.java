package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.HashValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code HLEN key} — returns the number of fields in the hash at {@code key}, or 0
 * if the key does not exist.
 */
public final class HlenCommand implements Command {

    @Override
    public String name() {
        return "HLEN";
    }

    @Override
    public int arity() {
        return 2; // HLEN key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        HashValue hash = context.storage()
                .getTyped(command.argAsString(0), HashValue.class, DataType.HASH);
        return Reply.integer(hash == null ? 0 : hash.size());
    }
}
