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

package org.lockss.util;

import java.util.*;
import org.springframework.http.HttpHeaders;

import org.lockss.test.*;

public class TestV2RepoUtil extends LockssTestCase {

  public void testHeadersFromProps() {
    CIProperties props = CIProperties.fromArgs("hhh1", "vvv1",
					       "mmm2", "mv1,mv2");
    HttpHeaders hdrs = V2RepoUtil.httpHeadersFromProps(props);
    assertEquals(MapUtil.map("hhh1", ListUtil.list("vvv1"),
			     "mmm2", ListUtil.list("mv1,mv2")),
		 hdrs);
  }

  public void testPropsFromHeaders() {
    HttpHeaders hdrs = new HttpHeaders();
    hdrs.add("hh", "vv");
    hdrs.add("mm", "mm1");
    hdrs.add("mm", "mm2");
    CIProperties props = V2RepoUtil.propsFromHttpHeaders(hdrs);
    assertEquals(CIProperties.fromArgs("hh", "vv", "mm", "mm1,mm2"),
 		 props);
  }

}
