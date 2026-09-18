package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code SISMEMBER key member} — replies {@code :1} if {@code member} is in the
 * set at {@code key}, otherwise {@code :0} (including when the key is absent).
 */
public final class SismemberCommand implements Command {

    @Override
    public String name() {
        return "SISMEMBER";
    }

    @Override
    public int arity() {
        return 3; // SISMEMBER key member
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        SetValue set = context.storage()
                .getTyped(command.argAsString(0), SetValue.class, DataType.SET);
        boolean present = set != null && set.contains(command.arg(1));
        return Reply.integer(present ? 1 : 0);
    }
}
