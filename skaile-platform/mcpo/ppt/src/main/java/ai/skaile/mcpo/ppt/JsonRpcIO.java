package ai.skaile.mcpo.ppt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class JsonRpcIO {
    private JsonRpcIO() {
    }

    public static byte[] readMessage(InputStream in) throws IOException {
        String line;
        int contentLength = -1;

        while (true) {
            line = readLine(in);
            if (line == null) {
                return null;
            }
            if (!line.isBlank()) {
                break;
            }
        }

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

    public static void writeMessage(OutputStream out, byte[] body) throws IOException {
        String headers = "Content-Length: " + body.length + "\r\n"
                + "Content-Type: application/json\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
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
