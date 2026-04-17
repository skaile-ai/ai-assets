package ai.skaile.mcpo.ppt;

import ai.skaile.mcpo.ppt.server.McpServer;

public final class McpServerMain {
    private McpServerMain() {
    }

    public static void main(String[] args) {
        McpServer server = new McpServer();
        server.run();
    }
}
