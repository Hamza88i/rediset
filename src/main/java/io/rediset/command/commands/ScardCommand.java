package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code SCARD key} — returns the number of members in the set at {@code key}, or
 * 0 if the key does not exist.
 */
public final class ScardCommand implements Command {

    @Override
    public String name() {
        return "SCARD";
    }

    @Override
    public int arity() {
        return 2; // SCARD key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        SetValue set = context.storage()
                .getTyped(command.argAsString(0), SetValue.class, DataType.SET);
        return Reply.integer(set == null ? 0 : set.size());
    }
}
