package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.ListValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code LPOP key} — removes and returns the first element of the list at
 * {@code key}, or null if the key does not exist or the list is empty. The key is
 * removed once the list becomes empty.
 */
public final class LpopCommand implements Command {

    @Override
    public String name() {
        return "LPOP";
    }

    @Override
    public int arity() {
        return 2; // LPOP key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        ListValue list = context.storage().getTyped(key, ListValue.class, DataType.LIST);
        if (list == null) {
            return Reply.nil();
        }
        byte[] element = list.popLeft();
        context.storage().removeIfEmpty(key, v -> ((ListValue) v).isEmpty());
        return element == null ? Reply.nil() : Reply.bulk(element);
    }
}
