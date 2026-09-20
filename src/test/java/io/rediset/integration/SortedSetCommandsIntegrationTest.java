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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SortedSetCommandsIntegrationTest {

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
    void zaddAndZrangeShouldOrderByScore() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(3, ((IntegerReply) client.command("ZADD", "z", "3", "c", "1", "a", "2", "b")).value());
            assertEquals(List.of("a", "b", "c"), asStrings(client.command("ZRANGE", "z", "0", "-1")));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void zaddShouldCountOnlyNewMembers() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("ZADD", "z", "1", "a");
            assertEquals(0, ((IntegerReply) client.command("ZADD", "z", "5", "a")).value());
            assertEquals("5", ((BulkStringReply) client.command("ZSCORE", "z", "a")).asString());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void zrangeWithScoresShouldInterleaveMembersAndScores() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("ZADD", "z", "1", "a", "2", "b");
            assertEquals(List.of("a", "1", "b", "2"),
                    asStrings(client.command("ZRANGE", "z", "0", "-1", "WITHSCORES")));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void zscoreOnMissingReturnsNull() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertInstanceOf(NullReply.class, client.command("ZSCORE", "z", "x"));
            client.command("ZADD", "z", "1", "a");
            assertInstanceOf(NullReply.class, client.command("ZSCORE", "z", "missing"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void zremShouldRemoveAndDeleteEmptyKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("ZADD", "z", "1", "a", "2", "b");
            assertEquals(1, ((IntegerReply) client.command("ZREM", "z", "a", "zzz")).value());
            assertEquals(1, ((IntegerReply) client.command("ZCARD", "z")).value());
            assertEquals(1, ((IntegerReply) client.command("ZREM", "z", "b")).value());
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "z")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void zaddWithNonFloatScoreReturnsError() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.command("ZADD", "z", "notanumber", "a");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().contains("not a valid float"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void zsetCommandOnStringKeyShouldReturnWrongType() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            Reply reply = client.command("ZADD", "k", "1", "a");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().startsWith("WRONGTYPE"));
        }
    }
}
