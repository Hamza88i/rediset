package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.ListValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code LINDEX key index} — returns the element at {@code index} in the list at
 * {@code key}. Negative indices count from the end. Returns null if the key does
 * not exist or the index is out of range.
 */
public final class LindexCommand implements Command {

    @Override
    public String name() {
        return "LINDEX";
    }

    @Override
    public int arity() {
        return 3; // LINDEX key index
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        int index = (int) Arguments.parseLong(command.argAsString(1));
        ListValue list = context.storage().getTyped(key, ListValue.class, DataType.LIST);
        if (list == null) {
            return Reply.nil();
        }
        byte[] element = list.get(index);
        return element == null ? Reply.nil() : Reply.bulk(element);
    }
}
