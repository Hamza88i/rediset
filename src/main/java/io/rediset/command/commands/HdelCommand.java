package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.HashValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code HDEL key field [field ...]} — removes fields from the hash at {@code key}.
 * Replies with the number removed. The key is deleted once the hash is empty.
 */
public final class HdelCommand implements Command {

    @Override
    public String name() {
        return "HDEL";
    }

    @Override
    public int arity() {
        return -3; // HDEL key field [field ...]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        HashValue hash = context.storage().getTyped(key, HashValue.class, DataType.HASH);
        if (hash == null) {
            return Reply.integer(0);
        }
        long removed = 0;
        for (int i = 1; i < command.arguments().size(); i++) {
            if (hash.remove(command.arg(i))) {
                removed++;
            }
        }
        context.storage().removeIfEmpty(key, v -> ((HashValue) v).isEmpty());
        return Reply.integer(removed);
    }
}
