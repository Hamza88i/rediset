package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code SREM key member [member ...]} — removes members from the set at
 * {@code key}. Replies with the number removed. The key is deleted once the set
 * becomes empty.
 */
public final class SremCommand implements Command {

    @Override
    public String name() {
        return "SREM";
    }

    @Override
    public int arity() {
        return -3; // SREM key member [member ...]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        SetValue set = context.storage().getTyped(key, SetValue.class, DataType.SET);
        if (set == null) {
            return Reply.integer(0);
        }
        long removed = 0;
        for (int i = 1; i < command.arguments().size(); i++) {
            if (set.remove(command.arg(i))) {
                removed++;
            }
        }
        context.storage().removeIfEmpty(key, v -> ((SetValue) v).isEmpty());
        return Reply.integer(removed);
    }
}
