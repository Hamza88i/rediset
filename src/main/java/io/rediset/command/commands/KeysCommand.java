package io.rediset.command.commands;

import io.rediset.command.Command;
import io.rediset.command.CommandContext;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import io.rediset.util.GlobPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code KEYS pattern} — returns all keys matching the glob-style {@code pattern}
 * as an array of bulk strings. {@code KEYS *} returns every key.
 *
 * <p>Like the systems that inspired it, this is an O(n) scan of the key space and
 * is intended for debugging and small data sets rather than hot paths.
 */
public final class KeysCommand implements Command {

    @Override
    public String name() {
        return "KEYS";
    }

    @Override
    public int arity() {
        return 2; // KEYS pattern
    }

    @Override
    public Reply execute(CommandContext context, ParsedCommand command) {
        String pattern = command.argAsString(0);
        List<Reply> matches = new ArrayList<>();
        for (String key : context.storage().keys()) {
            if (GlobPattern.matches(pattern, key)) {
                matches.add(Reply.bulk(key));
            }
        }
        return Reply.array(matches);
    }
}
