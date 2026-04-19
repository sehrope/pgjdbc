/*
 * Copyright (c) 2026, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.util;

import java.util.concurrent.ThreadFactory;

public class DefaultPGThreadFactoryFactory implements PGThreadFactoryFactory {
  public static final DefaultPGThreadFactoryFactory INSTANCE = new DefaultPGThreadFactoryFactory();

  public DefaultPGThreadFactoryFactory() {
  }

  @Override
  public ThreadFactory newThreadFactory() {
    return r -> {
      Thread thread = new Thread(r, "PostgreSQL JDBC driver connection thread");
      thread.setDaemon(true); // Don't prevent the VM from shutting down
      return thread;
    };
  }
}
