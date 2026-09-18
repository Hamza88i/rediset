package io.rediset.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.command.CommandRegistryFactory;
import io.rediset.protocol.Reply;
import io.rediset.protocol.Reply.ArrayReply;
import io.rediset.protocol.Reply.BulkStringReply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.IntegerReply;
import io.rediset.protocol.Reply.NullReply;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class HashCommandsIntegrationTest {

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

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hsetShouldCountNewFieldsOnly() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(2, ((IntegerReply) client.command("HSET", "h", "a", "1", "b", "2")).value());
            assertEquals(0, ((IntegerReply) client.command("HSET", "h", "a", "10")).value());
            assertEquals("10", ((BulkStringReply) client.command("HGET", "h", "a")).asString());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hgetOnMissingReturnsNull() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertInstanceOf(NullReply.class, client.command("HGET", "h", "x"));
            client.command("HSET", "h", "a", "1");
            assertInstanceOf(NullReply.class, client.command("HGET", "h", "missing"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hgetallShouldReturnAllPairs() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("HSET", "h", "a", "1", "b", "2");
            List<Reply> flat = ((ArrayReply) client.command("HGETALL", "h")).elements();
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < flat.size(); i += 2) {
                map.put(((BulkStringReply) flat.get(i)).asString(),
                        ((BulkStringReply) flat.get(i + 1)).asString());
            }
            assertEquals(Map.of("a", "1", "b", "2"), map);
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hkeysAndHvalsShouldReturnFieldsAndValues() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("HSET", "h", "a", "1", "b", "2");
            Set<String> keys = new HashSet<>();
            for (Reply e : ((ArrayReply) client.command("HKEYS", "h")).elements()) {
                keys.add(((BulkStringReply) e).asString());
            }
            Set<String> vals = new HashSet<>();
            for (Reply e : ((ArrayReply) client.command("HVALS", "h")).elements()) {
                vals.add(((BulkStringReply) e).asString());
            }
            assertEquals(Set.of("a", "b"), keys);
            assertEquals(Set.of("1", "2"), vals);
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hdelShouldRemoveAndDeleteEmptyKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("HSET", "h", "a", "1", "b", "2");
            assertEquals(1, ((IntegerReply) client.command("HDEL", "h", "a", "zzz")).value());
            assertEquals(1, ((IntegerReply) client.command("HLEN", "h")).value());
            assertEquals(1, ((IntegerReply) client.command("HDEL", "h", "b")).value());
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "h")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hsetWithOddArgsShouldReturnArityError() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.command("HSET", "h", "a", "1", "b"); // missing value for b
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().contains("wrong number of arguments"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void hashCommandOnStringKeyShouldReturnWrongType() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            Reply reply = client.command("HSET", "k", "f", "1");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().startsWith("WRONGTYPE"));
        }
    }
}
