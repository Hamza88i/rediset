package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.HashValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code HGET key field} — returns the value of {@code field} in the hash at
 * {@code key}, or null if the key or field does not exist.
 */
public final class HgetCommand implements Command {

    @Override
    public String name() {
        return "HGET";
    }

    @Override
    public int arity() {
        return 3; // HGET key field
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        HashValue hash = context.storage()
                .getTyped(command.argAsString(0), HashValue.class, DataType.HASH);
        if (hash == null) {
            return Reply.nil();
        }
        byte[] value = hash.get(command.arg(1));
        return value == null ? Reply.nil() : Reply.bulk(value);
    }
}
