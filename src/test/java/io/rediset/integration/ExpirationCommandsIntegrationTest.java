package io.rediset.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.command.CommandRegistry;
import io.rediset.command.CommandRegistryFactory;
import io.rediset.protocol.Reply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.IntegerReply;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * End-to-end tests for TTL commands over a real socket. Time-boundary behavior is
 * unit-tested deterministically with a fake clock; here we verify the wire-level
 * contract of each command.
 */
class ExpirationCommandsIntegrationTest {

    private RedisetServerFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        CommandRegistry registry = CommandRegistryFactory.createDefault();
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
    void ttlShouldReportNegativeTwoForMissingKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(-2, ((IntegerReply) client.command("TTL", "nope")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void ttlShouldReportNegativeOneWhenNoExpiry() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            assertEquals(-1, ((IntegerReply) client.command("TTL", "k")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void expireShouldSetTtl() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            assertEquals(1, ((IntegerReply) client.command("EXPIRE", "k", "100")).value());
            long ttl = ((IntegerReply) client.command("TTL", "k")).value();
            assertTrue(ttl > 0 && ttl <= 100, "ttl should be within (0,100], got " + ttl);
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void expireOnMissingKeyReturnsZero() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(0, ((IntegerReply) client.command("EXPIRE", "nope", "100")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void expireWithNonPositiveTtlDeletesKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            assertEquals(1, ((IntegerReply) client.command("EXPIRE", "k", "0")).value());
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "k")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void pexpireAndPttlWorkInMilliseconds() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            assertEquals(1, ((IntegerReply) client.command("PEXPIRE", "k", "100000")).value());
            long pttl = ((IntegerReply) client.command("PTTL", "k")).value();
            assertTrue(pttl > 0 && pttl <= 100_000, "pttl out of range: " + pttl);
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void persistShouldRemoveTtl() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            client.command("EXPIRE", "k", "100");
            assertEquals(1, ((IntegerReply) client.command("PERSIST", "k")).value());
            assertEquals(-1, ((IntegerReply) client.command("TTL", "k")).value());
            assertEquals(0, ((IntegerReply) client.command("PERSIST", "k")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void expireWithNonIntegerReturnsError() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            Reply reply = client.command("EXPIRE", "k", "abc");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().contains("not an integer"));
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void keyShouldDisappearAfterRealShortTtl() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            client.command("PEXPIRE", "k", "150");
            // Wait past the real TTL and verify lazy expiry on access.
            Thread.sleep(300);
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "k")).value());
        }
    }
}
