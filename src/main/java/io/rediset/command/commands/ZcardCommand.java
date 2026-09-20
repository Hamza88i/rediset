package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SortedSetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code ZCARD key} — returns the number of members in the sorted set at
 * {@code key}, or 0 if the key does not exist.
 */
public final class ZcardCommand implements Command {

    @Override
    public String name() {
        return "ZCARD";
    }

    @Override
    public int arity() {
        return 2; // ZCARD key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        SortedSetValue zset = context.storage()
                .getTyped(command.argAsString(0), SortedSetValue.class, DataType.SORTED_SET);
        return Reply.integer(zset == null ? 0 : zset.size());
    }
}
