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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * End-to-end tests for the MVP key/value command set over a real socket.
 */
class KeyValueCommandsIntegrationTest {

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
    void shouldSetAndGetValue() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals("OK", ((SimpleStringReply) client.command("SET", "name", "RediSet")).value());
            assertEquals("RediSet", ((BulkStringReply) client.command("GET", "name")).asString());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldReturnNullForMissingKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertInstanceOf(NullReply.class, client.command("GET", "nope"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldOverwriteOnSet() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "one");
            client.command("SET", "k", "two");
            assertEquals("two", ((BulkStringReply) client.command("GET", "k")).asString());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldCountExistsAndDelete() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "a", "1");
            client.command("SET", "b", "2");
            assertEquals(2, ((IntegerReply) client.command("EXISTS", "a", "b", "c")).value());
            assertEquals(2, ((IntegerReply) client.command("DEL", "a", "b", "missing")).value());
            assertEquals(0, ((IntegerReply) client.command("EXISTS", "a", "b")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldReportDbSize() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            assertEquals(0, ((IntegerReply) client.command("DBSIZE")).value());
            client.command("SET", "a", "1");
            client.command("SET", "b", "2");
            assertEquals(2, ((IntegerReply) client.command("DBSIZE")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldMatchKeysByPattern() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "user:1", "a");
            client.command("SET", "user:2", "b");
            client.command("SET", "admin:1", "c");

            ArrayReply matches = (ArrayReply) client.command("KEYS", "user:*");
            Set<String> keys = new HashSet<>();
            for (Reply element : matches.elements()) {
                keys.add(((BulkStringReply) element).asString());
            }
            assertEquals(Set.of("user:1", "user:2"), keys);

            ArrayReply all = (ArrayReply) client.command("KEYS", "*");
            assertEquals(3, all.elements().size());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldReportTypeOfKey() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            client.command("SET", "k", "v");
            assertEquals("string", ((SimpleStringReply) client.command("TYPE", "k")).value());
            assertEquals("none", ((SimpleStringReply) client.command("TYPE", "missing")).value());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldReturnArityErrorForBadArguments() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            Reply reply = client.command("SET", "onlykey");
            assertInstanceOf(ErrorReply.class, reply);
            assertTrue(((ErrorReply) reply).message().contains("wrong number of arguments"));
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldPreserveBinarySafeValues() throws Exception {
        try (RedisetTestClient client = fixture.newClient()) {
            // A value containing bytes that would break a line-based protocol.
            String tricky = "a\r\nb\u0000c";
            client.command("SET", "bin", tricky);
            assertEquals(tricky, ((BulkStringReply) client.command("GET", "bin")).asString());
        }
    }
}
