/*
 * Copyright (c) 2019, PostgreSQL Global Development Group
 * See the LICENSE file in the project root for more information.
 */

package org.postgresql.test.jdbc2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.postgresql.test.TestUtil;
import org.postgresql.util.PSQLState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MaxRowsTest {
  private static final String ONE_HUNDRED_ROW_SQL = "SELECT x FROM generate_series(1,100) x";

  private Connection conn;
  private Statement stmt;

  @Before
  public void setUp() throws SQLException {
    this.conn = TestUtil.openDB();
    this.stmt = conn.createStatement();
  }

  @After
  public void tearDown() throws Exception {
    TestUtil.closeQuietly(stmt);
    stmt = null;
    TestUtil.closeQuietly(conn);
    conn = null;
  }

  @Test
  public void testWithDefault() throws SQLException {
    assertEquals("Default value for maxRows should be zero", 0, stmt.getMaxRows());
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch all the rows", 100, count);
  }

  @Test
  public void testWithExplicitZeroMaxRows() throws SQLException {
    stmt.setMaxRows(0);
    assertEquals("Value for maxRows should be zero", 0, stmt.getMaxRows());
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch all the rows", 100, count);
  }

  @Test
  public void testWithLessThanAllTheRows() throws SQLException {
    int maxRows = 25;
    stmt.setMaxRows(maxRows);
    assertEquals("Value for maxRows should be updated value", maxRows, stmt.getMaxRows());
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch only " + maxRows + " rows", maxRows, count);
  }

  @Test
  public void testWithMoreThanAllTheRows() throws SQLException {
    int maxRows = 200;
    stmt.setMaxRows(maxRows);
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch all the rows", 100, count);
  }

  @Test
  public void testWithBadNegativeValue() throws SQLException {
    try {
      stmt.setMaxRows(-123);
      fail("Expected an exception to be thrown for invalid maxRows value");
    } catch (SQLException e) {
      assertEquals("SQL State should indicate an invalid parameter value", e.getSQLState(),
          PSQLState.INVALID_PARAMETER_VALUE.getState());
    }
    assertEquals("Value for maxRows should be unchanged from default of zero", 0, stmt.getMaxRows());
  }

  @Test
  public void testLargeWithExplicitZeroMaxRows() throws SQLException {
    stmt.setLargeMaxRows(0);
    assertEquals("Value for maxRows should be zero", 0, stmt.getMaxRows());
    assertEquals("Value for largeMaxRows should be zero", 0, stmt.getLargeMaxRows());
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch all the rows", 100, count);
  }

  @Test
  public void testLargeWithLessThanAllTheRows() throws SQLException {
    int maxRows = 25;
    stmt.setLargeMaxRows(maxRows);
    assertEquals("Value for maxRows should be updated value", maxRows, stmt.getMaxRows());
    assertEquals("Value for largeMaxRows should be updated value", maxRows, stmt.getLargeMaxRows());
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch only " + maxRows + " rows", maxRows, count);
  }

  @Test
  public void testLargeWithMoreThanAllTheRows() throws SQLException {
    long maxRows = 200;
    stmt.setLargeMaxRows(maxRows);
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch all the rows", 100, count);
  }

  @Test
  public void testLargeWithBadNegativeValue() throws SQLException {
    try {
      stmt.setLargeMaxRows(-123);
      fail("Expected an exception to be thrown for invalid maxRows value");
    } catch (SQLException e) {
      assertEquals("SQL State should indicate an invalid parameter value", e.getSQLState(),
          PSQLState.INVALID_PARAMETER_VALUE.getState());
    }
    assertEquals("Value for maxRows should be unchanged from default of zero", 0, stmt.getMaxRows());
  }

  @Test
  public void testLargeWithValueBiggerThanInt() throws SQLException {
    long maxRows = ((long)Integer.MAX_VALUE) + 12345;
    stmt.setLargeMaxRows(maxRows);
    assertEquals("Value for maxRows should be zero as it exceeds int max value", 0, stmt.getMaxRows());
    assertEquals("Value for largeMaxRows should be updated", maxRows, stmt.getLargeMaxRows());
    int count = TestUtil.executeQueryAndCountRows(stmt, ONE_HUNDRED_ROW_SQL);
    assertEquals("Result should fetch all the rows", 100, count);
  }
}
