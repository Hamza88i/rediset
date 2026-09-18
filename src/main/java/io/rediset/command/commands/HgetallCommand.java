package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.HashValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code HGETALL key} — returns all field/value pairs of the hash at {@code key} as
 * a flat array [field1, value1, field2, value2, ...]. Empty array if absent.
 */
public final class HgetallCommand implements Command {

    @Override
    public String name() {
        return "HGETALL";
    }

    @Override
    public int arity() {
        return 2; // HGETALL key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        HashValue hash = context.storage()
                .getTyped(command.argAsString(0), HashValue.class, DataType.HASH);
        if (hash == null) {
            return Reply.array(List.of());
        }
        List<Reply> replies = new ArrayList<>();
        for (byte[] element : hash.flattenedEntries()) {
            replies.add(Reply.bulk(element));
        }
        return Reply.array(replies);
    }
}
