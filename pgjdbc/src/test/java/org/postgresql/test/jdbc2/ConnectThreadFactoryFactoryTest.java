/*
 * Copyright (c) 2026, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.test.jdbc2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.postgresql.PGProperty;
import org.postgresql.test.TestUtil;
import org.postgresql.util.PGThreadFactoryFactory;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectThreadFactoryFactoryTest {
  @Test
  void customFactoryIsInvoked() throws Exception {
    AtomicInteger threadsCreated = new AtomicInteger();
    ThreadFactory threadFactory = r -> {
      threadsCreated.incrementAndGet();
      Thread t = new Thread(r, "ConnectThreadFactoryFactoryTest worker");
      t.setDaemon(true);
      return t;
    };

    ThreadLocalFactoryFactory.DELEGATE.set(threadFactory);
    try {
      Properties props = new Properties();
      PGProperty.LOGIN_TIMEOUT.set(props, "10");
      PGProperty.CONNECT_THREAD_FACTORY_FACTORY.set(props,
          ThreadLocalFactoryFactory.class.getName());

      try (Connection conn = TestUtil.openDB(props)) {
        assertNotNull(conn);
        assertTrue(conn.isValid(1));
      }

      assertEquals(1, threadsCreated.get(),
          "Configured ThreadFactory should produce exactly one thread for a connect attempt "
              + "with loginTimeout > 0");
    } finally {
      ThreadLocalFactoryFactory.DELEGATE.remove();
    }
  }

  @Test
  void factoryArgMatching() throws Exception {
    Properties props = new Properties();
    PGProperty.LOGIN_TIMEOUT.set(props, "10");
    PGProperty.CONNECT_THREAD_FACTORY_FACTORY.set(props,
        ArgValidatingFactoryFactory.class.getName());
    PGProperty.CONNECT_THREAD_FACTORY_FACTORY_ARG.set(props,
        ArgValidatingFactoryFactory.EXPECTED_ARG);

    try (Connection conn = TestUtil.openDB(props)) {
      assertNotNull(conn);
      assertTrue(conn.isValid(1));
    }
  }

  @Test
  void factoryArgMismatchFailsConnect() {
    Properties props = new Properties();
    PGProperty.LOGIN_TIMEOUT.set(props, "10");
    PGProperty.CONNECT_THREAD_FACTORY_FACTORY.set(props,
        ArgValidatingFactoryFactory.class.getName());
    PGProperty.CONNECT_THREAD_FACTORY_FACTORY_ARG.set(props, "wrong-arg");

    assertThrows(SQLException.class, () -> TestUtil.openDB(props),
        "Connect should fail when the worker thread's run() rejects the configured arg");
  }

  /**
   * Test-only {@link PGThreadFactoryFactory} that returns whatever {@link ThreadFactory} the
   * current thread has stashed in {@link #DELEGATE}. Lets each test install its own factory
   * without sharing static state across tests.
   */
  public static class ThreadLocalFactoryFactory implements PGThreadFactoryFactory {
    static final ThreadLocal<ThreadFactory> DELEGATE = new ThreadLocal<>();

    @Override
    public ThreadFactory newThreadFactory() {
      ThreadFactory tf = DELEGATE.get();
      if (tf == null) {
        throw new IllegalStateException(
            "No ThreadFactory configured in ThreadLocalFactoryFactory.DELEGATE");
      }
      return tf;
    }
  }

  /**
   * Test-only {@link PGThreadFactoryFactory} whose constructor captures the
   * {@code connectThreadFactoryFactoryArg} String. The produced ThreadFactory wraps the
   * connection task in a Runnable that throws if the captured arg does not match the expected
   * value so the failure happens inside the worker thread's run() rather than in the
   * factory's constructor.
   */
  public static class ArgValidatingFactoryFactory implements PGThreadFactoryFactory {
    static final String EXPECTED_ARG = "expected-arg-value";

    private final String arg;

    public ArgValidatingFactoryFactory(String arg) {
      this.arg = arg;
    }

    @Override
    public ThreadFactory newThreadFactory() {
      String capturedArg = this.arg;
      return r -> {
        Thread t = new Thread(() -> {
          if (!EXPECTED_ARG.equals(capturedArg)) {
            throw new IllegalArgumentException(
                "Unexpected connectThreadFactoryFactoryArg: " + capturedArg);
          }
          r.run();
        }, "ArgValidatingFactoryFactory worker");
        t.setDaemon(true);
        return t;
      };
    }
  }
}
