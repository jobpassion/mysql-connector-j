/*
  Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FLOSS License Exception
  <http://www.mysql.com/about/legal/licensing/foss-exception.html>.

  This program is free software; you can redistribute it and/or modify it under the terms
  of the GNU General Public License as published by the Free Software Foundation; version 2
  of the License.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this
  program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
  Floor, Boston, MA 02110-1301  USA

 */

package testsuite.fabric.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import testsuite.fabric.BaseFabricTestCase;

import com.mysql.fabric.jdbc.FabricMySQLConnection;
import com.mysql.fabric.jdbc.FabricMySQLDataSource;

/**
 * Testsuite for C/J Fabric regression tests.
 */
public class TestRegressions extends BaseFabricTestCase {
    private FabricMySQLConnection conn;

    public TestRegressions() throws Exception {
        super();
    }

    /**
     * Test for Bug#73070 - prepareCall() throws NPE
     * 
     * To test this, we create a basic stored procedure with a
     * parameter, call it and check the result.
     */
    public void testBug73070() throws Exception {
        if (!this.isSetForFabricTest) {
            return;
        }
        this.conn = (FabricMySQLConnection) getNewDefaultDataSource().getConnection(this.username, this.password);
        this.conn.setServerGroupName("fabric_test1_global");

        this.conn.createStatement().executeUpdate("drop procedure if exists bug73070");
        this.conn.createStatement().executeUpdate("create procedure bug73070(in x integer) select x");
        CallableStatement stmt = this.conn.prepareCall("{call bug73070(?)}");
        stmt.setInt(1, 42);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        assertEquals(42, rs.getInt(1));
        rs.close();
        stmt.close();
        this.conn.createStatement().executeUpdate("drop procedure bug73070");

        this.conn.close();
    }

    /**
     * Test Bug#75080 - NPE when setting a timestamp on a Fabric connection
     */
    public void testBug75080() throws Exception {
        if (!this.isSetForFabricTest) {
            return;
        }

        class TestBugInternal {
            @SuppressWarnings("synthetic-access")
            void test(FabricMySQLDataSource ds) throws Exception {
                TestRegressions.this.conn = (FabricMySQLConnection) ds.getConnection(TestRegressions.this.username, TestRegressions.this.password);
                TestRegressions.this.conn.setServerGroupName("fabric_test1_global");

                PreparedStatement ps = TestRegressions.this.conn.prepareStatement("select ?");
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                ps.setTimestamp(1, ts);
                ResultSet rs = ps.executeQuery();
                rs.next();
                Timestamp tsResult = rs.getTimestamp(1);
                assertEquals(ts, tsResult);
                rs.close();
                ps.close();
                TestRegressions.this.conn.close();
            }
        }

        FabricMySQLDataSource ds = getNewDefaultDataSource();

        // TODO get rid of inner class, not necessary if testing without legacy datetime
        new TestBugInternal().test(ds);
    }
}
