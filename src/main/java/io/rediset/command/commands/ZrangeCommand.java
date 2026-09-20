package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SortedSetValue;
import io.rediset.datatype.SortedSetValue.Entry;
import io.rediset.exception.InvalidArgumentException;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code ZRANGE key start stop [WITHSCORES]} — returns the members of the sorted
 * set at {@code key} in the inclusive rank range {@code [start, stop]} ordered by
 * ascending score (ties broken lexicographically). Negative indices count from the
 * end. With {@code WITHSCORES}, each member is followed by its score in the reply.
 */
public final class ZrangeCommand implements Command {

    private static final String WITHSCORES = "WITHSCORES";

    @Override
    public String name() {
        return "ZRANGE";
    }

    @Override
    public int arity() {
        return -4; // ZRANGE key start stop [WITHSCORES]
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        int start = (int) Arguments.parseLong(command.argAsString(1));
        int stop = (int) Arguments.parseLong(command.argAsString(2));
        boolean withScores = parseWithScores(command);

        SortedSetValue zset = context.storage()
                .getTyped(key, SortedSetValue.class, DataType.SORTED_SET);
        if (zset == null) {
            return Reply.array(List.of());
        }

        List<Reply> replies = new ArrayList<>();
        for (Entry entry : zset.rangeByRank(start, stop)) {
            replies.add(Reply.bulk(entry.member()));
            if (withScores) {
                replies.add(Reply.bulk(Arguments.formatScore(entry.score())));
            }
        }
        return Reply.array(replies);
    }

    private boolean parseWithScores(ParsedCommand command) {
        if (command.tokenCount() == 4) {
            return false;
        }
        if (command.tokenCount() == 5
                && command.argAsString(3).toUpperCase(Locale.ROOT).equals(WITHSCORES)) {
            return true;
        }
        throw new InvalidArgumentException("syntax error");
    }
}
