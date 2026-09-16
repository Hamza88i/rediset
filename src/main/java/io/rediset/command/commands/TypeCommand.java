package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.RedisetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code TYPE key} — returns the string name of the type stored at {@code key}
 * ({@code string}, {@code list}, {@code set}, {@code hash}, {@code zset}), or
 * {@code none} if the key does not exist.
 */
public final class TypeCommand implements Command {

    @Override
    public String name() {
        return "TYPE";
    }

    @Override
    public int arity() {
        return 2; // TYPE key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        RedisetValue value = context.storage().get(command.argAsString(0));
        String type = value == null ? "none" : value.type().displayName();
        return Reply.simple(type);
    }
}
