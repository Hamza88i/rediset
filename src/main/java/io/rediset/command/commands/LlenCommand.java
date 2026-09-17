package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.ListValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code LLEN key} — returns the length of the list at {@code key}, or 0 if the
 * key does not exist.
 */
public final class LlenCommand implements Command {

    @Override
    public String name() {
        return "LLEN";
    }

    @Override
    public int arity() {
        return 2; // LLEN key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        ListValue list = context.storage()
                .getTyped(command.argAsString(0), ListValue.class, DataType.LIST);
        return Reply.integer(list == null ? 0 : list.size());
    }
}
