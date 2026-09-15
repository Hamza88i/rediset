package io.rediset.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.command.commands.EchoCommand;
import io.rediset.command.commands.PingCommand;
import io.rediset.protocol.CommandParser;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.Reply;
import io.rediset.protocol.Reply.BulkStringReply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.SimpleStringReply;
import io.rediset.server.ClientSession;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandExecutorTest {

    private CommandExecutor executor;
    private CommandContext context;

    @BeforeEach
    void setUp() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        executor = new CommandExecutor(registry);
        context = new CommandContext(new ClientSession(1L, null));
    }

    private static ParsedCommand parse(String... tokens) {
        List<byte[]> raw = java.util.Arrays.stream(tokens)
                .map(t -> t.getBytes(StandardCharsets.UTF_8))
                .toList();
        return CommandParser.parse(raw);
    }

    @Test
    void shouldReturnPongForBarePing() {
        Reply reply = executor.execute(context, parse("PING"));
        assertInstanceOf(SimpleStringReply.class, reply);
        assertEquals("PONG", ((SimpleStringReply) reply).value());
    }

    @Test
    void shouldEchoPingArgument() {
        Reply reply = executor.execute(context, parse("PING", "hi"));
        assertInstanceOf(BulkStringReply.class, reply);
        assertEquals("hi", ((BulkStringReply) reply).asString());
    }

    @Test
    void shouldReturnErrorForUnknownCommand() {
        Reply reply = executor.execute(context, parse("BOGUS"));
        assertInstanceOf(ErrorReply.class, reply);
        assertTrue(((ErrorReply) reply).message().contains("unknown command"));
    }

    @Test
    void shouldReturnArityErrorForEchoWithoutArgument() {
        Reply reply = executor.execute(context, parse("ECHO"));
        assertInstanceOf(ErrorReply.class, reply);
        assertTrue(((ErrorReply) reply).message().contains("wrong number of arguments"));
    }

    @Test
    void shouldReturnArityErrorForPingWithTooManyArguments() {
        Reply reply = executor.execute(context, parse("PING", "a", "b"));
        assertInstanceOf(ErrorReply.class, reply);
    }

    @Test
    void shouldEchoArgument() {
        Reply reply = executor.execute(context, parse("ECHO", "world"));
        assertEquals("world", ((BulkStringReply) reply).asString());
    }
}
