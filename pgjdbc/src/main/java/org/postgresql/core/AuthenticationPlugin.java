/*
 * Copyright (c) 2021, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.core;

import org.postgresql.util.PSQLException;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface AuthenticationPlugin {
  enum AuthenticationRequestType {
    MD5, SCRAM, PLAIN, GSS,
  }

  @Nullable
  String getPassword(AuthenticationRequestType type) throws PSQLException;

}
