package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SortedSetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;

/**
 * {@code ZSCORE key member} — returns the score of {@code member} in the sorted set
 * at {@code key} as a bulk string, or null if the key or member does not exist.
 */
public final class ZscoreCommand implements Command {

    @Override
    public String name() {
        return "ZSCORE";
    }

    @Override
    public int arity() {
        return 3; // ZSCORE key member
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        SortedSetValue zset = context.storage()
                .getTyped(command.argAsString(0), SortedSetValue.class, DataType.SORTED_SET);
        if (zset == null) {
            return Reply.nil();
        }
        Double score = zset.score(command.arg(1));
        return score == null ? Reply.nil() : Reply.bulk(Arguments.formatScore(score));
    }
}
