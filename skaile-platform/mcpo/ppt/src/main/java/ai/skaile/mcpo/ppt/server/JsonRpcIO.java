package ai.skaile.mcpo.ppt.server;

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
        byte[] lineBytes = readLineBytes(in);
        while (lineBytes != null && isBlankBytes(lineBytes)) {
            lineBytes = readLineBytes(in);
        }

        if (lineBytes == null) {
            return null;
        }

        int firstNonWs = 0;
        while (firstNonWs < lineBytes.length
                && Character.isWhitespace((char) (lineBytes[firstNonWs] & 0xFF))) {
            firstNonWs++;
        }
        if (firstNonWs < lineBytes.length
                && (lineBytes[firstNonWs] == '{' || lineBytes[firstNonWs] == '[')) {
            // Line-framed JSON: bytes are already the UTF-8 encoded body. Returning them as-is
            // preserves any non-ASCII payload (e.g. bullet_character "•" encoded as E2 80 A2).
            return lineBytes;
        }

        // Content-Length framing: the first line is an HTTP-style header (ASCII by spec).
        return readFramedBody(in, new String(lineBytes, StandardCharsets.US_ASCII));
    }

    private static boolean isBlankBytes(byte[] bytes) {
        for (byte b : bytes) {
            if (!Character.isWhitespace((char) (b & 0xFF))) {
                return false;
            }
        }
        return true;
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
        byte[] bytes = readLineBytes(in);
        return bytes == null ? null : new String(bytes, StandardCharsets.US_ASCII);
    }

    private static byte[] readLineBytes(InputStream in) throws IOException {
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
        return buffer.toByteArray();
    }
}
