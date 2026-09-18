package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code SADD key member [member ...]} — adds members to the set at {@code key},
 * creating it if needed. Replies with the number of members newly added (already
 * present members are not counted).
 */
public final class SaddCommand implements Command {

    @Override
    public String name() {
        return "SADD";
    }

    @Override
    public int arity() {
        return -3; // SADD key member [member ...]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        SetValue set = context.storage()
                .getOrCreateTyped(key, DataType.SET, SetValue.class, SetValue::new);
        long added = 0;
        for (int i = 1; i < command.arguments().size(); i++) {
            if (set.add(command.arg(i))) {
                added++;
            }
        }
        return Reply.integer(added);
    }
}
