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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SetCommandsIntegrationTest {

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
    void saddShouldCountOnlyNewMembers() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(3, ((IntegerReply) client.command("SADD", "s", "a", "b", "c")).value());
            assertEquals(1, ((IntegerReply) client.command("SADD", "s", "a", "d")).value());
            assertEquals(4, ((IntegerReply) client.command("SCARD", "s")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void smembersShouldReturnAllMembers() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SADD", "s", "a", "b", "c");
            Set<String> members = new HashSet<>();
            for (Reply e : ((ArrayReply) client.command("SMEMBERS", "s")).elements()) {
                members.add(((BulkStringReply) e).asString());
            }
            assertEquals(Set.of("a", "b", "c"), members);
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void sismemberShouldReflectMembership() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SADD", "s", "a");
            assertEquals(1, ((IntegerReply) client.command("SISMEMBER", "s", "a")).value());
            assertEquals(0, ((IntegerReply) client.command("SISMEMBER", "s", "z")).value());
            assertEquals(0, ((IntegerReply) client.command("SISMEMBER", "missing", "a")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void sremShouldRemoveAndDeleteEmptyKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SADD", "s", "a", "b");
            assertEquals(1, ((IntegerReply) client.command("SREM", "s", "a", "zzz")).value());
            assertEquals(1, ((IntegerReply) client.command("SREM", "s", "b")).value());
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "s")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void scardOnMissingKeyReturnsZero() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(0, ((IntegerReply) client.command("SCARD", "missing")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void setCommandOnStringKeyShouldReturnWrongType() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            Reply reply = client.command("SADD", "k", "x");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().startsWith("WRONGTYPE"));
        }
    }
}
