/*

Copyright (c) 2000, Board of Trustees of Leland Stanford Jr. University.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.util.Iterator;

import org.lockss.test.LockssTestCase;

/**
 * This is the test class for org.lockss.daemon.UniqueRefLruCache
 */
public class TestUniqueRefLruCache extends LockssTestCase {
  private UniqueRefLruCache cache;

  public void setUp() throws Exception {
    super.setUp();
    cache = new UniqueRefLruCache(10);
  }

  public void testMaxSize() throws Exception {
    assertEquals(10, cache.getMaxSize());

    for (int ii=0; ii<11; ii++) {
      cache.put("test"+ii, new Object());
    }
    assertEquals(10, cache.lruMap.size());

    cache.setMaxSize(20);
    assertEquals(20, cache.getMaxSize());

    for (int ii=0; ii<21; ii++) {
      cache.put("test"+ii, new Object());
    }
    assertEquals(20, cache.lruMap.size());

    cache.setMaxSize(10);
    assertEquals(10, cache.getMaxSize());
    assertEquals(10, cache.lruMap.size());

    try {
      cache.setMaxSize(0);
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }

    try {
      cache = new UniqueRefLruCache(0);
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }
  }

  public void testCaching() throws Exception {
    Object obj = cache.get("foo");
    assertEquals(0, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
    assertNull(obj);

    obj = new Object();
    assertFalse(cache.containsKey("foo"));
    cache.put("foo", obj);
    Object obj2 = cache.get("foo");
    assertTrue(cache.containsKey("foo"));
    assertSame(obj, obj2);
    assertEquals(1, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    Object obj = cache.get("bar");
    assertEquals(1, cache.getCacheMisses());
    assertEquals(1, cache.getRefMisses());
    assertNull(obj);

    obj = new Object();
    cache.put("bar", obj);
    obj = cache.get("bar");
    assertEquals(1, cache.getCacheHits());

    Object obj2 = null;
    int loopSize = 1;
    int refHits = 0;
    // create objs in a loop until fetching the original creates a cache miss
    while (true) {
      loopSize *= 2;
      for (int ii=0; ii<loopSize; ii++) {
        cache.put("key_" + ii, new Object());
      }
      int misses = cache.getCacheMisses();
      refHits = cache.getRefHits();
      obj2 = cache.get("bar");
      if (cache.getCacheMisses() == misses+1) {
        break;
      }
    }
    assertSame(obj, obj2);
    assertEquals(refHits+1, cache.getRefHits());
  }

  public void testPutIfNew() throws Exception {
    Object o1 = cache.get("foo");
    assertEquals(0, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
    assertNull(o1);

    Object o2 = new Object();
    Object o3 = cache.putIfNew("foo", o2);
    assertSame(o2, o3);
    assertSame(o2, cache.get("foo"));

    Object o4 = new Object();
    Object o5 = cache.putIfNew("foo", o4);
    assertSame(o2, o5);
    assertNotSame(o4, o5);
    assertSame(o2, cache.get("foo"));
  }

  public void testRemovingFromLRU() throws Exception {
    Object obj = new Object();
    cache.put("baz", obj);
    obj = cache.get("baz");

    for (int ii=0; ii<cache.getMaxSize(); ii++) {
      cache.put("baz/test"+ii, new Object());
    }

    Object obj2 = cache.get("baz");
    assertEquals(1, cache.getCacheMisses());
  }

  public void testSnapshot() throws Exception {
    Object obj = new Object();
    cache.put("frob", obj);

    obj = new Object();
    cache.put("frob/test1", obj);

    Iterator snapshot = cache.snapshot().iterator();
    assertTrue(snapshot.hasNext());
    snapshot.next();

    obj = new Object();
    cache.put("frob/test2", obj);

    assertTrue(snapshot.hasNext());
    snapshot.next();

    assertFalse(snapshot.hasNext());
  }

}
