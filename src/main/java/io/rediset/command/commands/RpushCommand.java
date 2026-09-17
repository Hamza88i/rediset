package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.ListValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code RPUSH key element [element ...]} — appends one or more elements to the
 * list at {@code key}, creating the list if needed. Replies with the new length.
 */
public final class RpushCommand implements Command {

    @Override
    public String name() {
        return "RPUSH";
    }

    @Override
    public int arity() {
        return -3; // RPUSH key element [element ...]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        ListValue list = context.storage()
                .getOrCreateTyped(key, DataType.LIST, ListValue.class, ListValue::new);
        int length = 0;
        for (int i = 1; i < command.arguments().size(); i++) {
            length = list.pushRight(command.arg(i));
        }
        return Reply.integer(length);
    }
}
