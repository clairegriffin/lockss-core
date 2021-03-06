/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.Arrays;
import java.util.TimeZone;

import org.lockss.test.LockssTestCase;

/**
 * @deprecated {@link org.lockss.util.TimeZoneUtil} is deprecated (but is used
 *             in plugins); use {@link org.lockss.util.time.TimeZoneUtil} in
 *             lockss-util instead.
 */
@Deprecated
public class TestTimeZoneUtil extends LockssTestCase {

  /**
   * @deprecated {@link org.lockss.util.TimeZoneUtil} is deprecated (but is used
   *             in plugins); use {@link org.lockss.util.time.TimeZoneUtil} in
   *             lockss-util instead.
   */
  @Deprecated
  public void testIsBasicTimeZoneDataAvailable() {
    assertTrue(TimeZoneUtil.isBasicTimeZoneDataAvailable());
  }
  
  /**
   * @deprecated {@link org.lockss.util.TimeZoneUtil} is deprecated (but is used
   *             in plugins); use {@link org.lockss.util.time.TimeZoneUtil} in
   *             lockss-util instead.
   */
  @Deprecated
  public void testGoodTimeZones() throws Exception {
    for (String id : TimeZoneUtil.BASIC_TIME_ZONES) {
      TimeZone tz = TimeZoneUtil.getExactTimeZone(id);
      assertEquals(id, tz.getID());
      assertEquals("GMT".equals(id), "GMT".equals(tz.getID()));
    }
  }
  
  /**
   * @deprecated {@link org.lockss.util.TimeZoneUtil} is deprecated (but is used
   *             in plugins); use {@link org.lockss.util.time.TimeZoneUtil} in
   *             lockss-util instead.
   */
  @Deprecated
  public void testBadTimeZones() throws Exception {
    for (String id : Arrays.asList(null,
                                   "Foo",
                                   "America/Copenhagen",
                                   "Europe/Tokyo")) {
      try {
        TimeZone tz = TimeZoneUtil.getExactTimeZone(id);
        fail("Should have thrown IllegalArgumentException: " + id);
      }
      catch (IllegalArgumentException iae) {
        if (id == null) {
          assertEquals("Time zone identifier cannot be null", iae.getMessage());
        }
        else {
          assertEquals("Unknown time zone identifier: " + id, iae.getMessage());
        }
      }
    }
  }
  
}
