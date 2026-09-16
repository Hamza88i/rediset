package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.StringValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code SET key value} — stores {@code value} as a string under {@code key},
 * overwriting any existing value regardless of its previous type. Replies
 * {@code +OK}.
 */
public final class SetCommand implements Command {

    @Override
    public String name() {
        return "SET";
    }

    @Override
    public int arity() {
        return 3; // SET key value
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        StringValue value = new StringValue(command.arg(1));
        context.storage().put(key, value);
        return Reply.ok();
    }
}
