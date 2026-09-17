package io.rediset.command.commands;

import io.rediset.command.Arguments;
import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.ListValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code LRANGE key start stop} — returns the elements of the list at {@code key}
 * in the inclusive index range {@code [start, stop]}. Negative indices count from
 * the end. Returns an empty array if the key does not exist.
 */
public final class LrangeCommand implements Command {

    @Override
    public String name() {
        return "LRANGE";
    }

    @Override
    public int arity() {
        return 4; // LRANGE key start stop
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String key = command.argAsString(0);
        int start = (int) Arguments.parseLong(command.argAsString(1));
        int stop = (int) Arguments.parseLong(command.argAsString(2));

        ListValue list = context.storage().getTyped(key, ListValue.class, DataType.LIST);
        if (list == null) {
            return Reply.array(List.of());
        }
        List<Reply> replies = new ArrayList<>();
        for (byte[] element : list.range(start, stop)) {
            replies.add(Reply.bulk(element));
        }
        return Reply.array(replies);
    }
}
