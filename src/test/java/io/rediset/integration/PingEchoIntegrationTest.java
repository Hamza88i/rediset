package io.rediset.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.command.CommandRegistry;
import io.rediset.command.commands.EchoCommand;
import io.rediset.command.commands.PingCommand;
import io.rediset.protocol.Reply;
import io.rediset.protocol.Reply.BulkStringReply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.SimpleStringReply;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * End-to-end tests: real client → TCP → protocol → command → reply, exercising
 * the thinnest vertical slice (PING/ECHO).
 */
class PingEchoIntegrationTest {

    private RedisetServerFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new PingCommand());
        registry.register(new EchoCommand());
        fixture = RedisetServerFixture.start(registry);
    }

    @AfterEach
    void tearDown() {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldRespondPongToPing() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.command("PING");
            assertInstanceOf(SimpleStringReply.class, reply);
            assertEquals("PONG", ((SimpleStringReply) reply).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldEchoMessageViaEcho() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.command("ECHO", "RediSet");
            assertEquals("RediSet", ((BulkStringReply) reply).asString());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleMultipleCommandsOnSameConnection() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals("PONG", ((SimpleStringReply) client.command("PING")).value());
            assertEquals("a", ((BulkStringReply) client.command("ECHO", "a")).asString());
            assertEquals("PONG", ((SimpleStringReply) client.command("PING")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldSupportInlineCommandForm() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.inline("PING");
            assertEquals("PONG", ((SimpleStringReply) reply).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldReturnErrorForUnknownCommand() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.command("NOPE");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().contains("unknown command"));
        }
    }
}
