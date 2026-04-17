package ai.skaile.mcpo.ppt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class JsonRpcIO {
    private static final boolean LEGACY_FRAMED_STDIO = Boolean.parseBoolean(
            System.getenv().getOrDefault("MCPO_STDIO_FRAMED", "false"));

    private JsonRpcIO() {
    }

    public static byte[] readMessage(InputStream in) throws IOException {
        String line = readLine(in);
        while (line != null && line.isBlank()) {
            line = readLine(in);
        }

        if (line == null) {
            return null;
        }

        String trimmed = line.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return line.getBytes(StandardCharsets.UTF_8);
        }

        return readFramedBody(in, line);
    }

    public static void writeMessage(OutputStream out, byte[] body) throws IOException {
        if (LEGACY_FRAMED_STDIO) {
            String headers = "Content-Length: " + body.length + "\r\n"
                    + "Content-Type: application/json\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
            return;
        }

        out.write(body);
        out.write('\n');
        out.flush();
    }

    private static byte[] readFramedBody(InputStream in, String firstHeaderLine) throws IOException {
        String line = firstHeaderLine;
        int contentLength = -1;

        while (line != null && !line.isEmpty()) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                String name = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ("Content-Length".equalsIgnoreCase(name)) {
                    contentLength = Integer.parseInt(value);
                }
            }
            line = readLine(in);
        }

        if (contentLength < 0) {
            throw new IOException("Missing Content-Length header");
        }

        byte[] body = in.readNBytes(contentLength);
        if (body.length != contentLength) {
            throw new IOException("Unexpected EOF while reading message body");
        }
        return body;
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buffer.size() == 0) {
                    return null;
                }
                break;
            }
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buffer.write(b);
            }
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }
}
