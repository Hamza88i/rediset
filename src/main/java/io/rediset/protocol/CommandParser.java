package io.rediset.protocol;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Turns raw request tokens (as produced by {@link ProtocolDecoder#readRequest()})
 * into a {@link ParsedCommand}. This is the boundary between the byte-level
 * protocol and the higher-level command layer.
 */
public final class CommandParser {

    private CommandParser() {
    }

    /**
     * Parses request tokens into a command.
     *
     * @param tokens the wire tokens; the first is the command name
     * @return the parsed command
     * @throws ProtocolException if the token list is empty
     */
    public static ParsedCommand parse(List<byte[]> tokens) {
        if (tokens.isEmpty()) {
            throw new ProtocolException("empty command");
        }
        String name = new String(tokens.get(0), StandardCharsets.UTF_8)
                .toUpperCase(Locale.ROOT);
        List<byte[]> args = tokens.subList(1, tokens.size());
        return new ParsedCommand(name, args);
    }
}
