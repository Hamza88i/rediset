package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.ListValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code LPUSH key element [element ...]} — prepends one or more elements to the
 * list at {@code key}, creating the list if needed. Replies with the new length.
 * Elements are inserted so that the last argument ends up at the head.
 */
public final class LpushCommand implements Command {

    @Override
    public String name() {
        return "LPUSH";
    }

    @Override
    public int arity() {
        return -3; // LPUSH key element [element ...]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        ListValue list = context.storage()
                .getOrCreateTyped(key, DataType.LIST, ListValue.class, ListValue::new);
        int length = 0;
        for (int i = 1; i < command.arguments().size(); i++) {
            length = list.pushLeft(command.arg(i));
        }
        return Reply.integer(length);
    }
}
