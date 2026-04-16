package com.portfolex.excelmcp.log;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Locks down stdout. The MCP stdio transport reserves stdout for protocol frames; any rogue {@code
 * System.out.println} would corrupt the stream and drop the agent. We capture the real stdout at
 * startup, expose it for the transport to use directly, and replace {@code System.out} with stderr
 * so unexpected prints become harmless log noise instead of protocol corruption.
 */
public final class LoggingSetup {

  private static volatile OutputStream capturedStdout;

  private LoggingSetup() {}

  /** Call once, as early as possible in {@code main}. Idempotent. */
  public static synchronized void lockDownStdout() {
    if (capturedStdout == null) {
      capturedStdout = System.out;
    }
    PrintStream err = System.err;
    if (System.out != err) {
      System.setOut(err);
    }
  }

  /** The real stdout, captured before the redirect. Use this for MCP protocol output. */
  public static OutputStream realStdout() {
    if (capturedStdout == null) {
      throw new IllegalStateException("lockDownStdout() must be called before realStdout()");
    }
    return capturedStdout;
  }
}
