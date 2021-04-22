package org.postgresql.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongFunction;

public class VirtualInputStream extends InputStream {
  private @Nullable LongFunction<byte[]> recordSource;
  private @Nullable byte[] record = null;
  private long recordIndex = 0;
  private int pos = 0;

  public VirtualInputStream(LongFunction<byte[]> recordSource) {
    this.recordSource = recordSource;
  }

  @Override
  public int read() throws IOException {
    if (record != null && pos < record.length) {
      return record[pos++];
    }

    if (recordSource != null) {
      record = recordSource.apply(recordIndex++);
      pos = 0;
    }

    if (record != null && pos < record.length) {
      return record[pos++];
    }

    recordSource = null;
    return -1;
  }

  @Override
  public void close() {
    recordSource = null;
    record = null;
  }
}
