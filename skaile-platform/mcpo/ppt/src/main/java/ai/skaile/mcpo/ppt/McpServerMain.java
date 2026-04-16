package ai.skaile.mcpo.ppt;

public final class McpServerMain {
    private McpServerMain() {
    }

    public static void main(String[] args) {
        McpServer server = new McpServer();
        server.run();
    }
}
