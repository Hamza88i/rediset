package io.rediset.protocol;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A decoded command request: the (upper-cased) command name and its raw
 * binary-safe arguments. Produced by {@link CommandParser} from the tokens read
 * off the wire.
 */
public final class ParsedCommand {

    private final String name;
    private final List<byte[]> arguments;

    ParsedCommand(String name, List<byte[]> arguments) {
        this.name = Objects.requireNonNull(name, "name");
        this.arguments = List.copyOf(arguments);
    }

    /** The command name, normalized to upper case for case-insensitive lookup. */
    public String name() {
        return name;
    }

    /** The arguments following the command name (never includes the name itself). */
    public List<byte[]> arguments() {
        return arguments;
    }

    /** Total number of tokens including the command name (the command "arity"). */
    public int tokenCount() {
        return arguments.size() + 1;
    }

    /** Returns argument {@code index} decoded as a UTF-8 string. */
    public String argAsString(int index) {
        return new String(arguments.get(index), StandardCharsets.UTF_8);
    }

    /** Returns argument {@code index} as raw bytes. */
    public byte[] arg(int index) {
        return arguments.get(index);
    }

    @Override
    public String toString() {
        return "ParsedCommand[" + name.toLowerCase(Locale.ROOT) + ", args=" + arguments.size() + "]";
    }
}
