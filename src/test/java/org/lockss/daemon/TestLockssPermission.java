/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.daemon;

import java.io.*;
import java.util.*;

import org.lockss.state.*;
import org.lockss.clockss.*;
import org.lockss.daemon.CreativeCommonsPermissionChecker;
import org.lockss.daemon.CreativeCommonsRdfPermissionChecker;
import org.lockss.daemon.LockssPermission;
import org.lockss.daemon.StringPermissionChecker;
import org.lockss.test.*;

public class TestLockssPermission extends LockssPermissionCheckerTestCase {

  private String PERM_STRING = "LOCKSS system has permission to collect, " +
    "preserve, and serve this Archival Unit";

  private String OPEN_ACCESS_STRING =
    "LOCKSS system has permission to collect, preserve, " +
    "and serve this open access Archival Unit";

  public void setUp() throws Exception {
    super.setUp();
  }

  public void testStrings() {
    assertEquals(PERM_STRING, LockssPermission.LOCKSS_PERMISSION_STRING);
    assertEquals(OPEN_ACCESS_STRING,
		 LockssPermission.LOCKSS_OPEN_ACCESS_PERMISSION_STRING);
  }

  private boolean hasPermission(String page) throws IOException {
    return MiscTestUtil.hasPermission(new LockssPermission().getCheckers(),
				      page, mcf);
  }

  public void testNoPermission() throws IOException {
    assertFalse(hasPermission("LOCKSS system does not have permission to collect, preserve, and serve this Archival Unit"));
    assertFalse(hasPermission("LOCKSS system does not have permission to collect, preserve, and serve this open access Archival Unit"));
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testLockssPermission() throws IOException {
    String padding = org.apache.commons.lang3.StringUtils.repeat("Blah ", 50);
    assertTrue(hasPermission("LOCKSS system has permission to collect, preserve, and serve this Archival Unit"));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(padding + "LOCKSS system has permission to collect, preserve, and serve this Archival Unit"));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission("LOCKSS system has permission to collect, preserve, and serve this Archival Unit" + padding));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(padding + "LOCKSS system has permission to collect, preserve, and serve this Archival Unit" + padding));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
  }

  public void testLockssOpenAccessPermission() throws IOException {
    String padding = org.apache.commons.lang3.StringUtils.repeat("Blah ", 50);
    assertTrue(hasPermission("LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit"));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(hasPermission(padding + "LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit"));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(hasPermission("LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit" + padding));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(hasPermission(padding + "LOCKSS system has permission to collect, preserve, and serve this open access Archival Unit" + padding));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testLockssOjsPermission() throws IOException {
    String padding = org.apache.commons.lang3.StringUtils.repeat("Blah ", 50);
    String perm = "This journal utilizes the LOCKSS system to create a distributed archiving system among participating libraries and permits those libraries to create permanent archives of the journal for purposes of preservation and restoration";
    assertTrue(hasPermission(perm));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(padding + perm));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(perm + padding));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(padding + perm + padding));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
  }

  public void testNoMatchClockssPermission() throws IOException {
    assertFalse(hasPermission(ClockssPermission.CLOCKSS_PERMISSION_STRING));
  }

  /**
   * To catch changes to the permission checker list
   * Currently, this should have four checkers:
   * 1) A StringPermissionChecker with the LOCKSS Permission Statement
   * 2) A StringPermissionChecker with the LOCKSS open access Permission Statement
   * 3) A CreativeCommonsPermissionChecker
   * 4) A CreativeCommonsV3PermissionChecker
   */
  public void testGetCheckersHasProperCheckers() {
    List checkers = new LockssPermission().getCheckers();
    assertEquals("Expected four Permission checkers, but found ",
		 5, checkers.size());
    assertTrue("First checker wasn't a StringPermission Checker",
	       checkers.get(0) instanceof StringPermissionChecker);
    assertTrue("Second checker wasn't a StringPermission Checker",
	       checkers.get(1) instanceof StringPermissionChecker);
    assertTrue("Third checker wasn't a StringPermission Checker",
	       checkers.get(2) instanceof StringPermissionChecker);
    assertTrue("Fourth checker wasn't a CreativeCommonsRdfPermissionChecker",
	       checkers.get(3) instanceof CreativeCommonsRdfPermissionChecker);
    assertTrue("Fifth checker wasn't a CreativeCommonsV3PermissionChecker",
	       checkers.get(4) instanceof CreativeCommonsPermissionChecker);
  }

  public void testGetCheckersNotModifiable() {
    List checkers = new LockssPermission().getCheckers();
    try {
      checkers.set(0, new StringPermissionChecker("anything",
						  new StringPermissionChecker.StringFilterRule()));
      fail("Shouldn't be able to modify permission checker list");
    } catch (UnsupportedOperationException e) {
    }
  }
}
