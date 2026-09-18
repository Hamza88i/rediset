package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.HashValue;
import io.rediset.exception.CommandArityException;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code HSET key field value [field value ...]} — sets one or more fields in the
 * hash at {@code key}, creating it if needed. Replies with the number of fields
 * that were newly created (not counting updates to existing fields).
 */
public final class HsetCommand implements Command {

    @Override
    public String name() {
        return "HSET";
    }

    @Override
    public int arity() {
        return -4; // HSET key field value [field value ...]
    }

    @Override
    public void validate(ParsedCommand command) {
        Command.super.validate(command);
        // Arguments after the key must come in field/value pairs.
        int argsAfterKey = command.arguments().size() - 1;
        if (argsAfterKey % 2 != 0) {
            throw new CommandArityException(name());
        }
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        HashValue hash = context.storage()
                .getOrCreateTyped(key, DataType.HASH, HashValue.class, HashValue::new);
        long created = 0;
        for (int i = 1; i < command.arguments().size(); i += 2) {
            if (hash.set(command.arg(i), command.arg(i + 1))) {
                created++;
            }
        }
        return Reply.integer(created);
    }
}
