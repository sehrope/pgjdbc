/*
 * Copyright (c) 2019, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */


package org.postgresql.util;

public class CopyNotSupportedException extends PSQLException {
  public CopyNotSupportedException(String msg, PSQLState state) {
    super(msg, state);
  }

  public CopyNotSupportedException(ServerErrorMessage serverError) {
    super(serverError);
  }
}
