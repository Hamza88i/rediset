package io.rediset.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.command.CommandRegistry;
import io.rediset.command.CommandRegistryFactory;
import io.rediset.protocol.Reply;
import io.rediset.protocol.Reply.ArrayReply;
import io.rediset.protocol.Reply.BulkStringReply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.IntegerReply;
import io.rediset.protocol.Reply.NullReply;
import io.rediset.protocol.Reply.SimpleStringReply;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ListCommandsIntegrationTest {

    private RedisetServerFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RedisetServerFixture.start(CommandRegistryFactory.createDefault());
    }

    @AfterEach
    void tearDown() {
        if (fixture != null) {
            fixture.close();
        }
    }

    private static List<String> asStrings(Reply reply) {
        List<String> out = new ArrayList<>();
        for (Reply e : ((ArrayReply) reply).elements()) {
            out.add(((BulkStringReply) e).asString());
        }
        return out;
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldRpushAndLrange() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(3, ((IntegerReply) client.command("RPUSH", "l", "a", "b", "c")).value());
            assertEquals(List.of("a", "b", "c"), asStrings(client.command("LRANGE", "l", "0", "-1")));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void lpushShouldReverseInsertionOrder() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("LPUSH", "l", "a", "b", "c");
            assertEquals(List.of("c", "b", "a"), asStrings(client.command("LRANGE", "l", "0", "-1")));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldPopFromBothEnds() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("RPUSH", "l", "a", "b", "c");
            assertEquals("a", ((BulkStringReply) client.command("LPOP", "l")).asString());
            assertEquals("c", ((BulkStringReply) client.command("RPOP", "l")).asString());
            assertEquals(1, ((IntegerReply) client.command("LLEN", "l")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void keyShouldBeRemovedWhenListEmptied() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("RPUSH", "l", "only");
            client.command("LPOP", "l");
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "l")).value());
            assertInstanceOf(NullReply.class, client.command("LPOP", "l"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void lindexShouldSupportNegativeIndex() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("RPUSH", "l", "a", "b", "c");
            assertEquals("c", ((BulkStringReply) client.command("LINDEX", "l", "-1")).asString());
            assertInstanceOf(NullReply.class, client.command("LINDEX", "l", "99"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void llenOnMissingKeyReturnsZero() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(0, ((IntegerReply) client.command("LLEN", "missing")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void listCommandOnStringKeyShouldReturnWrongType() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "s", "value");
            Reply reply = client.command("RPUSH", "s", "x");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().startsWith("WRONGTYPE"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void stringCommandOnListKeyShouldReturnWrongType() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("RPUSH", "l", "a");
            Reply reply = client.command("GET", "l");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().startsWith("WRONGTYPE"));
            // TYPE should report list, confirming state is intact.
            assertEquals("list", ((SimpleStringReply) client.command("TYPE", "l")).value());
        }
    }
}
