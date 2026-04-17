package ai.skaile.mcpo.ppt.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JsonRpcIOTest {
    @Test
    void readsLineDelimitedJsonMessage() throws Exception {
        byte[] input = "{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}\n"
                .getBytes(StandardCharsets.UTF_8);

        byte[] message = JsonRpcIO.readMessage(new ByteArrayInputStream(input));

        assertArrayEquals(
                "{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}".getBytes(StandardCharsets.UTF_8),
                message);
    }

    @Test
    void readsFramedJsonMessage() throws Exception {
        byte[] body = "{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}".getBytes(StandardCharsets.UTF_8);
        String headers = "Content-Length: " + body.length + "\r\n"
                + "Content-Type: application/json\r\n\r\n";
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        framed.write(headers.getBytes(StandardCharsets.US_ASCII));
        framed.write(body);

        byte[] message = JsonRpcIO.readMessage(new ByteArrayInputStream(framed.toByteArray()));

        assertArrayEquals(body, message);
    }

    @Test
    void writesLineDelimitedJsonMessageByDefault() throws Exception {
        byte[] body = "{\"jsonrpc\":\"2.0\",\"result\":{}}".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JsonRpcIO.writeMessage(out, body);

        byte[] expected = "{\"jsonrpc\":\"2.0\",\"result\":{}}\n".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, out.toByteArray());
    }
}
