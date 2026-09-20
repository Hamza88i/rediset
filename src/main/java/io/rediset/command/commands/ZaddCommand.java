package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SortedSetValue;
import io.rediset.exception.CommandArityException;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code ZADD key score member [score member ...]} — adds members with scores to
 * the sorted set at {@code key}, creating it if needed, or updates the scores of
 * existing members. Replies with the number of members newly added (not counting
 * score updates).
 */
public final class ZaddCommand implements Command {

    @Override
    public String name() {
        return "ZADD";
    }

    @Override
    public int arity() {
        return -4; // ZADD key score member [score member ...]
    }

    @Override
    public void validate(ParsedCommand command) {
        Command.super.validate(command);
        int argsAfterKey = command.arguments().size() - 1;
        if (argsAfterKey % 2 != 0) {
            throw new CommandArityException(name());
        }
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        // Parse all scores first so a malformed score does not partially apply.
        int pairs = (command.arguments().size() - 1) / 2;
        double[] parsedScores = new double[pairs];
        for (int p = 0; p < pairs; p++) {
            parsedScores[p] = Arguments.parseDouble(command.argAsString(1 + p * 2));
        }

        SortedSetValue zset = context.storage()
                .getOrCreateTyped(key, DataType.SORTED_SET, SortedSetValue.class, SortedSetValue::new);
        long added = 0;
        for (int p = 0; p < pairs; p++) {
            byte[] member = command.arg(2 + p * 2);
            if (zset.add(member, parsedScores[p])) {
                added++;
            }
        }
        return Reply.integer(added);
    }
}
