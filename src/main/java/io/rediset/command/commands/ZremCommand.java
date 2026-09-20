package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SortedSetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code ZREM key member [member ...]} — removes members from the sorted set at
 * {@code key}. Replies with the number removed. The key is deleted once empty.
 */
public final class ZremCommand implements Command {

    @Override
    public String name() {
        return "ZREM";
    }

    @Override
    public int arity() {
        return -3; // ZREM key member [member ...]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        SortedSetValue zset = context.storage()
                .getTyped(key, SortedSetValue.class, DataType.SORTED_SET);
        if (zset == null) {
            return Reply.integer(0);
        }
        long removed = 0;
        for (int i = 1; i < command.arguments().size(); i++) {
            if (zset.remove(command.arg(i))) {
                removed++;
            }
        }
        context.storage().removeIfEmpty(key, v -> ((SortedSetValue) v).isEmpty());
        return Reply.integer(removed);
    }
}
