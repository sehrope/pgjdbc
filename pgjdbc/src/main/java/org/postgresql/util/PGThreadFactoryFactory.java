/*
 * Copyright (c) 2026, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.util;

import java.util.concurrent.ThreadFactory;

/**
 * Factory of {@link ThreadFactory} instances used by the driver when spawning helper threads for
 * connection establishment (for example, enforcing {@code loginTimeout}).
 */
public interface PGThreadFactoryFactory {
  ThreadFactory newThreadFactory();
}
