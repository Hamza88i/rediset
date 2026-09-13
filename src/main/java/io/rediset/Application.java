package io.rediset;

import io.rediset.config.ServerConfig;
import io.rediset.server.ClientSession;
import io.rediset.server.ConnectionHandler;
import io.rediset.server.NetworkConfig;
import io.rediset.server.RedisetServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RediSet entry point.
 *
 * <p>At this stage the server is wired to a placeholder line-echo handler so the
 * networking layer can be exercised end-to-end. Subsequent steps replace this
 * handler with the RediSet protocol decoder and command engine.
 */
public final class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        NetworkConfig networkConfig = NetworkConfig.from(config);

        RedisetServer server = new RedisetServer(networkConfig, lineEchoHandler());
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "rediset-shutdown"));

        server.start();
        server.awaitTermination();
    }

    /**
     * A temporary handler that echoes each received line back to the client. It
     * exists only so the networking layer is runnable before the protocol layer
     * is implemented.
     */
    private static ConnectionHandler lineEchoHandler() {
        return (ClientSession session, InputStream in, OutputStream out) -> {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while (!session.isClosing() && (line = reader.readLine()) != null) {
                session.touch();
                writeLine(out, line);
            }
        };
    }

    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.write('\n');
        out.flush();
    }
}
