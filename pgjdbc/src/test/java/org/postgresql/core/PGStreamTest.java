/*
 * Copyright (c) 2026, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.postgresql.util.HostSpec;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;

import javax.net.SocketFactory;

class PGStreamTest {
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  /**
   * Socket that is "connected" to a fixed sequence of input bytes. Reaching the end of those
   * bytes behaves like the peer closing the connection.
   */
  private static class FixedInputSocket extends Socket {
    private final InputStream in;
    private final OutputStream out = new ByteArrayOutputStream();

    FixedInputSocket(byte[] input) {
      this.in = new ByteArrayInputStream(input);
    }

    @Override
    public boolean isConnected() {
      return true;
    }

    @Override
    public InputStream getInputStream() {
      return in;
    }

    @Override
    public OutputStream getOutputStream() {
      return out;
    }

    @Override
    public void setTcpNoDelay(boolean on) {
    }

    @Override
    public int getSendBufferSize() {
      return 8192;
    }
  }

  private static class FixedSocketFactory extends SocketFactory {
    private final Socket socket;

    FixedSocketFactory(Socket socket) {
      this.socket = socket;
    }

    @Override
    public Socket createSocket() {
      return socket;
    }

    @Override
    public Socket createSocket(String host, int port) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(InetAddress host, int port) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
        int localPort) {
      throw new UnsupportedOperationException();
    }
  }

  private static PGStream newPGStream(byte[] input) throws IOException {
    return new PGStream(new FixedSocketFactory(new FixedInputSocket(input)),
        new HostSpec("localhost", 5432), 0, 8192);
  }

  @Test
  void skipFollowedByReceive() throws IOException {
    try (PGStream stream = newPGStream(new byte[]{1, 2, 3, 4, 5})) {
      stream.skip(3);
      assertArrayEquals(new byte[]{4, 5}, stream.receive(2));
    }
  }

  @Test
  void skipExactlyToEndOfStream() throws IOException {
    try (PGStream stream = newPGStream(new byte[]{1, 2, 3})) {
      stream.skip(3);
    }
  }

  @Test
  void skipPastEndOfStreamThrowsEOF() {
    // A broken implementation busy-loops forever when the stream ends before the
    // requested number of bytes has been skipped, hence the preemptive timeout
    assertTimeoutPreemptively(TIMEOUT, () -> {
      try (PGStream stream = newPGStream(new byte[]{1, 2, 3})) {
        assertThrows(EOFException.class, () -> stream.skip(10));
      }
    });
  }

  @Test
  void skipPastEndOfEmptyStreamThrowsEOF() {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      try (PGStream stream = newPGStream(new byte[0])) {
        assertThrows(EOFException.class, () -> stream.skip(1));
      }
    });
  }
}
