package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.datatype.DataType;
import io.rediset.datatype.SetValue;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code SMEMBERS key} — returns all members of the set at {@code key} as an array
 * of bulk strings, in unspecified order. Returns an empty array if the key is
 * absent.
 */
public final class SmembersCommand implements Command {

    @Override
    public String name() {
        return "SMEMBERS";
    }

    @Override
    public int arity() {
        return 2; // SMEMBERS key
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        SetValue set = context.storage()
                .getTyped(command.argAsString(0), SetValue.class, DataType.SET);
        if (set == null) {
            return Reply.array(List.of());
        }
        List<Reply> replies = new ArrayList<>();
        for (byte[] member : set.members()) {
            replies.add(Reply.bulk(member));
        }
        return Reply.array(replies);
    }
}
