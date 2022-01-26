/*
 * Copyright (c) 2021, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.core;

import org.postgresql.PGProperty;
import org.postgresql.plugin.AuthenticationPlugin;
import org.postgresql.plugin.AuthenticationRequestType;
import org.postgresql.util.GT;
import org.postgresql.util.ObjectFactory;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthenticationPluginManager {
  private static final Logger LOGGER = Logger.getLogger(AuthenticationPluginManager.class.getName());

  private AuthenticationPluginManager() {
  }

  public interface PasswordCallback {
    void withPassword(char @Nullable [] password) throws PSQLException, IOException;
  }

  public interface EndodedPasswordCallback {
    void withEncodedPassword(byte[] encodedPassword) throws PSQLException, IOException;
  }

  /**
   * If a password is requested by the server during connection initiation, this
   * method will be invoked to supply the password. This method will only be
   * invoked if the server actually requests a password, e.g. trust authentication
   * will skip it entirely.
   *
   * <p>The caller provides a callback method that will be invoked with the char[]
   * password. After completion, for security reasons the char[] array will be
   * wiped by filling it with zeroes. Callers must not rely on being able to read
   * the password char[] after the callback has completed.</p>
   *
   * @param type The authentication type that is being requested
   * @param info The connection properties for the connection
   * @param callback The callback to invoke with the password
   * @throws PSQLException Throws a PSQLException if the plugin class cannot be instantiated
   * @throws IOException Bubbles up any thrown IOException from the provided callback
   */
  public static void withPassword(AuthenticationRequestType type, Properties info, PasswordCallback callback) throws PSQLException, IOException {
    char[] password = null;

    String authPluginClassName = PGProperty.AUTHENTICATION_PLUGIN_CLASS_NAME.get(info);

    if (authPluginClassName == null || authPluginClassName.equals("")) {
      // Default auth plugin simply pulls password directly from connection properties
      String passwordText = PGProperty.PASSWORD.get(info);
      if (passwordText != null) {
        password = passwordText.toCharArray();
      }
    } else {
      AuthenticationPlugin authPlugin;
      try {
        authPlugin = (AuthenticationPlugin) ObjectFactory.instantiate(authPluginClassName, info,
            false, null);
      } catch (Exception ex) {
        LOGGER.log(Level.FINE, "Unable to load Authentication Plugin " + ex.toString());
        throw new PSQLException(ex.getMessage(), PSQLState.UNEXPECTED_ERROR);
      }

      password = authPlugin.getPassword(type);
    }

    try {
      callback.withPassword(password);
    } finally {
      if (password != null) {
        java.util.Arrays.fill(password, (char) 0);
      }
    }
  }

  /**
   * Helper that wraps withPassword(...), checks that it is not-null, and encodes
   * it as a byte array. Used by internal code paths that require an encoded password
   * that may be an empty string, but not null.
   *
   * <p>The caller provides a callback method that will be invoked with the byte[]
   * encoded password. After completion, for security reasons the byte[] array will be
   * wiped by filling it with zeroes. Callers must not rely on being able to read
   * the password byte[] after the callback has completed.</p>

   * @param type The authentication type that is being requested
   * @param info The connection properties for the connection
   * @param callback The callback to invoke with the encoded password
   * @throws PSQLException Throws a PSQLException if the plugin class cannot be instantiated or if the retrieved password is null.
   * @throws IOException Bubbles up any thrown IOException from the provided callback
   */
  public static void withEncodedPassword(AuthenticationRequestType type, Properties info, EndodedPasswordCallback callback) throws PSQLException, IOException {
    withPassword(type, info, password -> {
      if (password == null) {
        throw new PSQLException(
            GT.tr("The server requested password-based authentication, but no password was provided."),
            PSQLState.CONNECTION_REJECTED);
      }

      ByteBuffer buf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
      byte[] encodedPassword = new byte[buf.limit()];
      buf.get(encodedPassword);

      try {
        callback.withEncodedPassword(encodedPassword);
      } finally {
        java.util.Arrays.fill(encodedPassword, (byte) 0);
      }
    });
  }
}
