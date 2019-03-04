/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University.
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

package org.lockss.daemon;

import java.io.File;
import java.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.TitleConfig;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.daemon.TitleConfig
 */

public class TestTitleConfig extends LockssTestCase {

  private PluginManager pmgr;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    getMockLockssDaemon().setUpAuConfig();

    pmgr = getMockLockssDaemon().getPluginManager();
    setUpDiskSpace();
  }

  public void tearDown() throws Exception {
    getMockLockssDaemon().stopDaemon();
    super.tearDown();
  }

  public void testConstructors() {
    String id = "pid";
    String name = "foo 2";
    MockPlugin mp = new MockPlugin();
    mp.setPluginId(id);
    TitleConfig tc1 = new TitleConfig(name, mp);
    TitleConfig tc2 = new TitleConfig(name, id);
    assertSame(id, tc1.getPluginName());
    assertSame(id, tc2.getPluginName());
    assertEquals(name, tc1.getDisplayName());
  }

  public void testAccessors1() {
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setJournalTitle("42");
    assertEquals("42", tc1.getJournalTitle());
    tc1.setPluginVersion("4");
    assertEquals("4", tc1.getPluginVersion());
    List foo = new ArrayList();
    tc1.setParams(foo);
    assertSame(foo, tc1.getParams());

    assertEmpty(tc1.getAttributes());
    Map map = MapUtil.map("key1", "val2", "k0", "1");
    tc1.setAttributes(map);
    assertSame(map, tc1.getAttributes());
  }

  public void testGetConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Configuration config = tc1.getConfig();
    Configuration exp = ConfigManager.newConfiguration();
    exp.put("key1", "a");
    exp.put("key2", "foo");
    assertEquals(exp, config);
  }

  public void testGetConfigExcludesDefaultOnlyParam() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2").setDefaultOnly(true);
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Configuration config = tc1.getConfig();
    Configuration exp = ConfigManager.newConfiguration();
    exp.put("key1", "a");
    assertEquals(exp, config);
  }

  public void testGetNoEditKeys() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    a2.setEditable(true);
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Collection u1 = tc1.getUnEditableKeys();
    assertIsomorphic(ListUtil.list("key1"), u1);
  }

  public void testMatchesConfig() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    Configuration config = tc1.getConfig();
    assertTrue(tc1.matchesConfig(config));
    // should still match with an extra param in config
    config.put("key4", "v");
    assertTrue(tc1.matchesConfig(config));
    // but not with a disagreeing one
    config.put("key1", "v");
    assertFalse(tc1.matchesConfig(config));
    // make it match again
    config.put("key1", "a");
    assertTrue(tc1.matchesConfig(config));
    // then make a param editable, which should make it not match
    a1.setEditable(true);
    assertFalse(tc1.matchesConfig(config));
  }

  public void testIsSingleAu() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamDescr d3 = new ConfigParamDescr("key3");
    d3.setDefinitional(false);
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    ConfigParamAssignment a3 = new ConfigParamAssignment(d3, "bar");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    MockPlugin mp = new MockPlugin();
    // matching plugin descrs and tc descrs
    mp.setAuConfigDescrs(ListUtil.list(d1, d2));
    assertTrue(tc1.isSingleAu(mp));
    // tc missing required plugin param
    tc1.setParams(ListUtil.list(a1));
    assertFalse(tc1.isSingleAu(mp));
    // tc has the one required, extra plugin param (d3) ok
    mp.setAuConfigDescrs(ListUtil.list(d1, d3));
    assertTrue(tc1.isSingleAu(mp));
    // extra params in tc ok
    tc1.setParams(ListUtil.list(a1, a2, a3));
    assertTrue(tc1.isSingleAu(mp));
    mp.setAuConfigDescrs(ListUtil.list(d1, d2, d3));
    assertTrue(tc1.isSingleAu(mp));
  }

  public void testGetAuId() {
    MockPlugin mp = new MockPlugin();
    mp.setPluginId("pid");
    ConfigParamDescr d1 = new ConfigParamDescr("base_url");
    ConfigParamDescr d2 = new ConfigParamDescr("volume");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    String auid = tc1.getAuId(pmgr, mp);
    assertEquals("pid&base_url~a&volume~foo", auid);
    assertSame(auid, tc1.getAuId(pmgr, mp));
  }

  public void testIsActionable() throws Exception {
//     ConfigurationUtil.addFromArgs(PluginManager.PARAM_REMOVE_STOPPED_AUS,
// 				  "true");

    pmgr.startService();  // deactivateAu() below requires service runnning
    MockPlugin mp = new MockPlugin();
    ConfigParamDescr d1 = new ConfigParamDescr("base_url");
    ConfigParamDescr d2 = new ConfigParamDescr("volume");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    String auid = tc1.getAuId(pmgr, mp);
    assertTrue(tc1.isActionable(pmgr, TitleSet.SET_ADDABLE));
    assertFalse(tc1.isActionable(pmgr, TitleSet.SET_DELABLE));
    assertFalse(tc1.isActionable(pmgr, TitleSet.SET_REACTABLE));

    // Create an AU matching this TitleConfig
    ArchivalUnit au =
      PluginTestUtil.createAndStartAu(mp.getPluginId(), tc1.getConfig());
    assertEquals(tc1.getAuId(pmgr, mp), au.getAuId());

    assertFalse(tc1.isActionable(pmgr, TitleSet.SET_ADDABLE));
    assertTrue(tc1.isActionable(pmgr, TitleSet.SET_DELABLE));
    assertFalse(tc1.isActionable(pmgr, TitleSet.SET_REACTABLE));

    // Deactivate the AU
    pmgr.deactivateAu(au);

    assertFalse(tc1.isActionable(pmgr, TitleSet.SET_ADDABLE));
    assertFalse(tc1.isActionable(pmgr, TitleSet.SET_DELABLE));
    assertTrue(tc1.isActionable(pmgr, TitleSet.SET_REACTABLE));
  }

  public void testEqualsHashWithNulls() {
    TitleConfig tc1 = new TitleConfig(null, (String)null);
    TitleConfig tc1c = new TitleConfig(null, (String)null);
    assertEquals(tc1, tc1c);
    assertEquals(tc1.hashCode(), tc1c.hashCode());
  }

  public void testEqualsHash() {
    ConfigParamDescr d1 = new ConfigParamDescr("key1");
    ConfigParamDescr d2 = new ConfigParamDescr("key2");
    ConfigParamAssignment a1 = new ConfigParamAssignment(d1, "a");
    ConfigParamAssignment a2 = new ConfigParamAssignment(d2, "foo");
    TitleConfig tc1 = new TitleConfig("a", "b");
    tc1.setParams(ListUtil.list(a1, a2));
    tc1.setJournalTitle("jtitle");
    tc1.setPluginVersion("2");
    tc1.setEstimatedSize(42);
    ConfigParamDescr d1c = new ConfigParamDescr("key1");
    ConfigParamDescr d2c = new ConfigParamDescr("key2");
    ConfigParamAssignment a1c = new ConfigParamAssignment(d1c, "a");
    ConfigParamAssignment a2c = new ConfigParamAssignment(d2c, "foo");
    TitleConfig tc1c = new TitleConfig("a", "b");
    tc1c.setParams(ListUtil.list(a1c, a2c));
    tc1c.setJournalTitle("jtitle");
    tc1c.setPluginVersion("2");
    tc1c.setEstimatedSize(42);
    assertEquals(tc1, tc1c);
    assertEquals(tc1.hashCode(), tc1c.hashCode());
    // params are order-independent
    tc1c.setParams(ListUtil.list(a2c, a1c));
    assertEquals(tc1, tc1c);
    assertEquals(tc1.hashCode(), tc1c.hashCode());
    tc1c.setParams(ListUtil.list(a1c, new ConfigParamAssignment(d1c, "b")));
    assertNotEquals(tc1, tc1c);
    tc1c.setParams(ListUtil.list(a1c, a2c));
    assertEquals(tc1, tc1c);
    tc1c.setJournalTitle("sdfsdf");
    assertNotEquals(tc1, tc1c);
    tc1.setJournalTitle("sdfsdf");
    assertEquals(tc1, tc1c);
    tc1c.setPluginVersion("3");
    assertNotEquals(tc1, tc1c);
    tc1.setPluginVersion("3");
    assertEquals(tc1, tc1c);
    tc1c.setEstimatedSize(420);
    assertNotEquals(tc1, tc1c);
    tc1.setEstimatedSize(420);
    assertEquals(tc1, tc1c);

    assertNotEquals(new TitleConfig("a", "b"), new TitleConfig("2", "b"));
    assertNotEquals(new TitleConfig("a", "b"), new TitleConfig("a", "2"));

  }

}
