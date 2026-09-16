package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.StringValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code GET key} — returns the string value of {@code key} as a bulk string, or
 * null if the key does not exist. Raises {@code WRONGTYPE} if the key holds a
 * non-string value.
 */
public final class GetCommand implements Command {

    @Override
    public String name() {
        return "GET";
    }

    @Override
    public int arity() {
        return 2; // GET key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        StringValue value = context.storage().getTyped(key, StringValue.class, DataType.STRING);
        if (value == null) {
            return Reply.nil();
        }
        return Reply.bulk(value.bytes());
    }
}
