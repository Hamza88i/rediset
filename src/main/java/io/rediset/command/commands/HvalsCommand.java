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
 * {@code HVALS key} — returns all values of the hash at {@code key}. Empty array if
 * absent.
 */
public final class HvalsCommand implements Command {

    @Override
    public String name() {
        return "HVALS";
    }

    @Override
    public int arity() {
        return 2; // HVALS key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        HashValue hash = context.storage()
                .getTyped(command.argAsString(0), HashValue.class, DataType.HASH);
        if (hash == null) {
            return Reply.array(List.of());
        }
        List<Reply> replies = new ArrayList<>();
        for (byte[] value : hash.values()) {
            replies.add(Reply.bulk(value));
        }
        return Reply.array(replies);
    }
}
