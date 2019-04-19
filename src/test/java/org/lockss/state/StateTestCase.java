/*

Copyright (c) 2018-2019 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

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

package org.lockss.state;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import org.junit.*;

import org.lockss.app.StoreException;
import org.lockss.config.ConfigManager;
import org.lockss.config.db.ConfigDbManager;
import org.lockss.log.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.state.AuSuspectUrlVersions.SuspectUrlVersion;
import static org.lockss.protocol.AgreementType.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.SetUtil;
import org.lockss.util.time.TimeBase;

/**
 * Framework, utilities and assertions for state-related tests
 */
public abstract class StateTestCase extends LockssTestCase4 {
  static L4JLogger log = L4JLogger.getLogger();

  protected MockLockssDaemon daemon;
  protected PluginManager pluginMgr;
  protected BaseStateManager stateMgr;
  protected MockIdentityManager idMgr;

  protected String tmpdir;
  protected MockArchivalUnit mau1;
  protected MockArchivalUnit mau2;

  protected List<MockPeerIdentity> peerIdentityList;
  protected PeerIdentity pid0, pid1, pid2, pid3;

  protected static String AUID1 = MockPlugin.KEY + "&base_url~aaa1";
  protected static String AUID2 = MockPlugin.KEY + "&base_url~aaa2";
  protected static List CDN_STEMS =
    ListUtil.list("http://abc.com", "https://xyz.org");

  @Before
  public void setUp() throws Exception {
    super.setUp();

    tmpdir = setUpDiskSpace();
    daemon = getMockLockssDaemon();

    // Start the managers needed by the subclass
    startManagers();
    pluginMgr = daemon.getPluginManager();

    stateMgr = (BaseStateManager)daemon.setUpStateManager(makeStateManager());


    MockPlugin plugin = new MockPlugin(daemon);
    mau1 = new MockArchivalUnit(plugin, AUID1);
    mau2 = new MockArchivalUnit(plugin, AUID2);

    peerIdentityList =
      ListUtil.list(new MockPeerIdentity("tcp:[127.0.0.0]:1231"),
		    new MockPeerIdentity("tcp:[127.0.0.1]:1231"),
		    new MockPeerIdentity("tcp:[127.0.0.2]:1231"),
		    new MockPeerIdentity("tcp:[127.0.0.3]:1231"));
    pid0 = peerIdentityList.get(0);
    pid1 = peerIdentityList.get(1);
    pid2 = peerIdentityList.get(2);
    pid3 = peerIdentityList.get(3);
    log.debug("pid0: {}, pid1: {}", pid0, pid1);

    idMgr = new MockIdentityManager();
    daemon.setManagerByType(IdentityManager.class, idMgr);
    idMgr.initService(daemon);

    // Register fake PeerIdentity s with IdentityManager
    for (PeerIdentity pid : peerIdentityList) {
      idMgr.addPeerIdentity(pid.getIdString(), pid);
    }
  }

  @After
  public void tearDown() throws Exception {
    log.debug("tearDown");
    stateMgr.stopService();
    super.tearDown();
  }

  // Subclasses may implement to start necessary managers
  protected void startManagers() {
  }

  // Subclasses must implement to create their desired StateManager
  // implementation
  protected abstract StateManager makeStateManager();

  protected void storeAuAgreements(AuAgreements aua, PeerIdentity... pids) {
    aua.storeAuAgreements(Arrays.stream(pids).collect(Collectors.toSet()));
  }

  // Assert the values in a single agreement
  protected void assertAgreeTime(float agree,
				 long time,
				 PeerAgreement peerAgreement) {
    assertEquals(agree, peerAgreement.getPercentAgreement(), .001);
    assertEquals(time, peerAgreement.getPercentAgreementTime());
  }

  /** A StateManager with an assignable StateStore */
  protected static class MyPersistentStateManager
    extends PersistentStateManager {

    StateStore sstore;

    @Override
    protected StateStore getStateStore() throws StoreException {
      if (sstore != null) return sstore;
      return super.getStateStore();
    }

    void setStateStore(StateStore val) {
      sstore = val;
    }

  }

  /** A simple in-memory StateStore.  Serializes / deserializes as an easy
   * way to ensure no sharing between stored & retrieved objects, */
  protected class MyStateStore implements StateStore {

    Map<String,String> austates = new HashMap<>();
    Map<String,String> auagmnts = new HashMap<>();
    Map<String,String> ausuvs = new HashMap<>();
    Map<String,String> audpis = new HashMap<>();

    @Override
    public AuStateBean findArchivalUnitState(String auId)
	throws StoreException, IOException {
      log.debug("findArchivalUnitState("+auId+") = "
		+austates.get(auId));
      return getAuState(auId);
    }

    void putAuState(String auId, AuStateBean ausb) {
      try {
	austates.put(auId, ausb.toJson());
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
    }

    AuStateBean getAuState(String auId) throws IOException {
      String json = austates.get(auId);
      if (json != null) {
	return AuStateBean.fromJson(auId, json, daemon);
      }
      return null;
    }

    @Override
    public Long updateArchivalUnitState(String auId, AuStateBean ausb,
					Set<String> fields)
	throws StoreException {
      if (!austates.containsKey(auId)) {
	log.debug("Storing new AuStateBean: {}", auId);
      }
      putAuState(auId, ausb);
      return 1L;
    }

    MyStateStore setStoredAuState(String auId, String json) {
      austates.put(auId, json);
      log.debug("setStoredAuState("+auId+", "+json+")");
      return this;
    }

    String getStoredAuState(String auId) {
      return austates.get(auId);
    }

    @Override
    public AuAgreements findAuAgreements(String key)
	throws IOException {
      return getAuAgreeements(key);
    }

    @Override
    public Long updateAuAgreements(String key, AuAgreements aua,
				   Set<PeerIdentity> peers) {
      putAuAgreements(key, aua);
      return 1L;
    }

    void putAuAgreements(String key, AuAgreements aua) {
      try {
	auagmnts.put(key, aua.toJson());
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
    }

    AuAgreements getAuAgreeements(String auId) throws IOException {
      String json = auagmnts.get(auId);
      if (json != null) {
	return AuAgreements.fromJson(auId, json, daemon);
      }
      return null;
    }

    MyStateStore setStoredAuAgreements(String auId, String json) {
      auagmnts.put(auId, json);
      log.debug("setStoredAuAgreements("+auId+", "+json+")");
      return this;
    }

    String getStoredAuAgreements(String auId) {
      return auagmnts.get(auId);
    }

    @Override
    public AuSuspectUrlVersions findAuSuspectUrlVersions(String key)
	      throws IOException {
      return getAuSuspectUrlVersions(key);
    }

    @Override
    public Long updateAuSuspectUrlVersions(String key,
					   AuSuspectUrlVersions ausuv,
					   Set<SuspectUrlVersion> urlVersions)
					     throws StoreException {
      putAuSuspectUrlVersions(key, ausuv);
      return 1L;
    }

    void putAuSuspectUrlVersions(String key, AuSuspectUrlVersions aua) {
      try {
	ausuvs.put(key, aua.toJson());
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
    }

    AuSuspectUrlVersions getAuSuspectUrlVersions(String auId)
	throws IOException {
      String json = ausuvs.get(auId);
      if (json != null) {
	return AuSuspectUrlVersions.fromJson(auId, json, daemon);
      }
      return null;
    }

    @Override
    public DatedPeerIdSet findDatedPeerIdSet(String key) throws IOException {
      return (DatedPeerIdSet)getDatedPeerIdSet(key);
    }

    @Override
    public Long updateDatedPeerIdSet(String key, DatedPeerIdSet dpis,
				     Set<PeerIdentity> peers) {
      putDatedPeerIdSet(key, dpis);
      return 1L;
    }

    void putDatedPeerIdSet(String key, DatedPeerIdSet dpis) {
      try {
	audpis.put(key, dpis.toJson());
      } catch (IOException e) {
	throw new RuntimeException(e);
      }
    }

    PersistentPeerIdSetImpl getDatedPeerIdSet(String auId) throws IOException {
      String json = audpis.get(auId);
      if (json != null) {
	return PersistentPeerIdSetImpl.fromJson(auId, json, daemon);
      }
      return null;
    }
  }
}
