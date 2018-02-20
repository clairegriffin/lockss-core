/*

Copyright (c) 2013-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import org.apache.commons.collections.map.LinkedMap;
import org.lockss.alert.AlertManager;
import org.lockss.account.AccountManager;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.crawler.CrawlManager;
import org.lockss.daemon.*;
import org.lockss.daemon.status.StatusService;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.hasher.HashService;
import org.lockss.mail.MailService;
import org.lockss.metadata.MetadataDbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.*;
import org.lockss.truezip.*;
import org.lockss.poller.PollManager;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.remote.RemoteApi;
import org.lockss.repository.*;
import org.lockss.scheduler.SchedService;
import org.lockss.servlet.*;
import org.lockss.state.*;
import org.lockss.subscription.SubscriptionManager;
import org.lockss.util.*;
import org.lockss.clockss.*;
import org.lockss.safenet.*;

public class MockLockssDaemon extends LockssDaemon {
  private static Logger log = Logger.getLogger("MockLockssDaemon");

//   ResourceManager resourceManager = null;
//   WatchdogService wdogService = null;
//   MailService mailService = null;
//   AlertManager alertManager = null;
//   AccountManager accountManager = null;
//   RandomManager randomManager = null;
//   LockssKeyStoreManager keystoreManager = null;
//   HashService hashService = null;
//   SchedService schedService = null;
//   SystemMetrics systemMetrics = null;
//   PollManager pollManager = null;
//   PsmManager psmManager = null;
//   LcapDatagramComm commManager = null;
//   LcapStreamComm scommManager = null;
//   LcapDatagramRouter datagramRouterManager = null;
//   LcapRouter routerManager = null;
//   ProxyManager proxyManager = null;
//   ServletManager servletManager = null;
//   CrawlManager crawlManager = null;
//   RepositoryManager repositoryManager = null;
//   NodeManagerManager nodeManagerManager = null;
//   PluginManager pluginManager = null;
//   MetadataManager metadataManager = null;
//   IdentityManager identityManager = null;
//   TrueZipManager tzipManager = null;
//   StatusService statusService = null;
//   RemoteApi remoteApi = null;
//   IcpManager icpManager = null;
//   ClockssParams clockssParams = null;
//   DbManager dbManager = null;
//   MetadataDbManager metadataDbManager = null;
//   CounterReportsManager counterReportsManager = null;
//   SubscriptionManager subscriptionManager = null;
//   Cron cron = null;
//   EntitlementRegistryClient entitlementRegistryClient = null;
  private boolean suppressStartAuManagers = true;

  /** Unit tests that need a MockLockssDaemon should use {@link
   * LockssTestCase#getMockLockssDaemon()} rather than calling this
   * directly.  Some utilities (not descended from LockssTestCase) also
   * need one, so this constructor is protected to allow them to directly
   * create an instance (of their own subclass). */
  protected MockLockssDaemon() {
    this(null);
  }

  private MockLockssDaemon(List<String> urls) {
    super(urls);
    ConfigManager mgr = ConfigManager.getConfigManager();
    mgr.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration prevConfig,
					 Configuration.Differences changedKeys) {
	  setConfig(newConfig, prevConfig, changedKeys);
	}
      });
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Configuration.Differences changedKeys) {
    super.setConfig(config, prevConfig, changedKeys);
  }


  /** Does nothing */
  @Override
  public void startApp() throws Exception {
  }

  @Override
  public void stopApp() {
    auManagerMaps.clear();

    managerMap.clear();

    //super.stopDaemon();
  }

  /** Set the testing mode.  (Normally done through config and daemon
   * startup.) */
  public void setTestingMode(String mode) {
    testingMode = mode;
  }

  @Override
  public AppSpec getAppSpec() {
    if (appSpec == null) {
      appSpec = new AppSpec()
	.setName("Mock Lockss Daemon")
// 	.setArgs("")
	.setAppManagers(super.getAppManagerDescs());
    }
    return appSpec;
  }

  ManagerDesc findManagerDesc(String key) {
    return findDesc(getManagerDescs(), key);
  }

  ManagerDesc findAuManagerDesc(String key) {
    return findDesc(getAuManagerDescs(), key);
  }

  ManagerDesc findDesc(ManagerDesc[] descs, String key) {
    for(int i=0; i< descs.length; i++) {
      ManagerDesc desc = descs[i];
      if (key.equals(desc.getKey())) {
	return desc;
      }
    }
    return null;
  }

  /** Create a manager instance, mimicking what LockssDaemon does */
  LockssManager newManager(String key) {
    log.debug2("Loading manager: " + key);
    ManagerDesc desc = findManagerDesc(key);
    if (desc == null) {
      throw new LockssAppException("No ManagerDesc for: " + key);
    }
    if (log.isDebug2()) {
      log.debug2("Manager class: " + getManagerClassName(desc));
    }
    try {
      return initManager(desc);
    } catch (Exception e) {
      log.error("Error creating manager", e);
      throw new LockssAppException("Can't load manager: " + e.toString());
    }
  }

  public <T> T getManagerByType(Class<T> mgrType) {
    T mgr = (T)managerMap.get(managerKey(mgrType));
    if (mgr == null) {
      mgr = (T)newManager(managerKey(mgrType));
    }
    return mgr;
  }

  /**
   * return the watchdog service instance
   * @return the WatchdogService
   */
  public WatchdogService getWatchdogService() {
    return getManagerByType(WatchdogService.class);
  }

  /**
   * return the mail manager instance
   * @return the MailService
   */
  public MailService getMailService() {
    return getManagerByType(MailService.class);
  }

  /**
   * return the resource manager instance
   * @return the ResourceManager
   */
  public ResourceManager getResourceManager() {
    return getManagerByType(ResourceManager.class);
  }

  /**
   * return the alert manager instance
   * @return the AlertManager
   */
  public AlertManager getAlertManager() {
    return getManagerByType(AlertManager.class);
  }

  /**
   * return the account manager instance
   * @return the AccountManager
   */
  public AccountManager getAccountManager() {
    return getManagerByType(AccountManager.class);
  }

  /**
   * return the random manager instance
   * @return the RandomManager
   */
  public RandomManager getRandomManager() {
    return getManagerByType(RandomManager.class);
  }

  /**
   * return the keystore manager instance
   * @return the KeystoreManager
   */
  public LockssKeyStoreManager getKeystoreManager() {
    return getManagerByType(LockssKeyStoreManager.class);
  }

  /**
   * return the hash service instance
   * @return the HashService
   */
  public HashService getHashService() {
    return getManagerByType(HashService.class);
  }

  /**
   * return the sched service instance
   * @return the SchedService
   */
  public SchedService getSchedService() {
    return getManagerByType(SchedService.class);
  }

  /**
   * return the SystemMetrics instance
   * @return the SystemMetrics
   */
  public SystemMetrics getSystemMetrics() {
    return getManagerByType(SystemMetrics.class);
  }

  /**
   * return the poll manager instance
   * @return the PollManager
   */
  public PollManager getPollManager() {
    return getManagerByType(PollManager.class);
  }

  /**
   * return the psm manager instance
   * @return the PsmManager
   */
  public PsmManager getPsmManager() {
    return getManagerByType(PsmManager.class);
  }

  /**
   * return the datagram communication manager instance
   * @return the LcapDatagramComm
   */
  public LcapDatagramComm getDatagramCommManager() {
    return getManagerByType(LcapDatagramComm.class);
  }

  /**
   * return the stream communication manager instance
   * @return the LcapStreamComm
   */
  public LcapStreamComm getStreamCommManager() {
    return getManagerByType(LcapStreamComm.class);
  }

  /**
   * return the datagram router manager instance
   * @return the LcapDatagramRouter
   */
  public LcapDatagramRouter getDatagramRouterManager() {
    return getManagerByType(LcapDatagramRouter.class);
  }

  /**
   * return the router manager instance
   * @return the LcapRouter
   */
  public LcapRouter getRouterManager() {
    return getManagerByType(LcapRouter.class);
  }

  /**
   * return the proxy manager instance
   * @return the ProxyManager
   */
  public ProxyManager getProxyManager() {
    return getManagerByType(ProxyManager.class);
  }

  /**
   * return the servlet manager instance
   * @return the ServletManager
   */
  public ServletManager getServletManager() {
    return getManagerByType(AdminServletManager.class);
  }

  /**
   * return the TrueZip manager instance
   * @return the TrueZipManager
   */
  public TrueZipManager getTrueZipManager() {
    return getManagerByType(TrueZipManager.class);
  }

  /**
   * return the crawl manager instance
   * @return the CrawlManager
   */
  public CrawlManager getCrawlManager() {
    return getManagerByType(CrawlManager.class);
  }

  /**
   * return the node manager status instance
   * @return the TreewalkManager
   */
  public NodeManagerManager getNodeManagerManager() {
    return getManagerByType(NodeManagerManager.class);
  }

  /**
   * return the repository manager instance
   * @return the RepositoryManager
   */
  public RepositoryManager getRepositoryManager() {
    return getManagerByType(RepositoryManager.class);
  }

  /**
   * return the plugin manager instance
   * @return the PluginManager
   */
  public PluginManager getPluginManager() {
    return getManagerByType(PluginManager.class);
  }

  /**
   * return the metadata manager instance
   * @return the MetadataManager
   */
  public MetadataManager getMetadataManager() {
    return getManagerByType(MetadataManager.class);
  }

  /**
   * return the Identity Manager
   * @return IdentityManager
   */
  public IdentityManager getIdentityManager() {
    return getManagerByType(IdentityManager.class);
  }

  public boolean hasIdentityManager() {
    return managerMap.containsKey(managerKey(IdentityManager.class));
  }

  /**
   * return the database manager instance
   * @return the DbManager
   */
//   public DbManager getDbManager() {
//     return getManagerByType(DbManager.class);
//   }

  /**
   * return the metadata database manager instance
   * @return the MetadataDbManager
   */
  public MetadataDbManager getMetadataDbManager() {
    return getManagerByType(MetadataDbManager.class);
  }

  /**
   * return the COUNTER reports manager instance
   * @return the CounterReportsManager
   */
  public CounterReportsManager getCounterReportsManager() {
    return getManagerByType(CounterReportsManager.class);
  }

  /**
   * return the subscription manager instance
   * @return the SusbcriptionManager
   */
  public SubscriptionManager getSusbcriptionManager() {
    return getManagerByType(SubscriptionManager.class);
  }

  /**
   * return the cron instance
   * @return the Cron
   */
  public Cron getCron() {
    return getManagerByType(Cron.class);
  }

  public StatusService getStatusService() {
    return getManagerByType(StatusService.class);
  }

  /**
   * return the RemoteApi instance
   * @return the RemoteApi
   */
  public RemoteApi getRemoteApi() {
    return getManagerByType(RemoteApi.class);
  }

  /**
   * return the ClockssParams instance
   * @return the ClockssParams
   */
  public ClockssParams getClockssParams() {
    return getManagerByType(ClockssParams.class);
  }

  private boolean forceIsClockss = false;

  public void setClockss(boolean val) {
    forceIsClockss = val;
  }

  public boolean isClockss() {
    return forceIsClockss || super.isClockss();
  }

  /**
   * Store a LockssManager instance in the mock daemon
   * @param mgrKey the manager key
   * @param mgr the new manager
   */
  public void setManagerByKey(String mgrKey, LockssManager mgr) {
    managerMap.put(mgrKey, mgr);
  }

  /**
   * Store a LockssManager instance in the mock daemon
   * @param mgrType the manager type
   * @param mgr the new manager
   */
  public void setManagerByType(Class mgrType, LockssManager mgr) {
    managerMap.put(managerKey(mgrType), mgr);
  }

  /**
   * Set the datagram CommManager
   * @param commMan the new manager
   */
  public void setDatagramCommManager(LcapDatagramComm commMan) {
    managerMap.put(LockssDaemon.DATAGRAM_COMM_MANAGER, commMan);
  }

  /**
   * Set the stream CommManager
   * @param scommMan the new manager
   */
  public void setStreamCommManager(LcapStreamComm scommMan) {
    managerMap.put(LockssDaemon.STREAM_COMM_MANAGER, scommMan);
  }

  /**
   * Set the DatagramRouterManager
   * @param datagramRouterMan the new manager
   */
  public void setDatagramRouterManager(LcapDatagramRouter datagramRouterMan) {
    managerMap.put(LockssDaemon.DATAGRAM_ROUTER_MANAGER,
		   datagramRouterMan);
  }

  /**
   * Set the RouterManager
   * @param routerMan the new manager
   */
  public void setRouterManager(LcapRouter routerMan) {
    managerMap.put(LockssDaemon.ROUTER_MANAGER, routerMan);
  }

  /**
   * Set the CrawlManager
   * @param crawlMan the new manager
   */
  public void setCrawlManager(CrawlManager crawlMan) {
    managerMap.put(LockssDaemon.CRAWL_MANAGER, crawlMan);
  }

  /**
   * Set the RepositoryManager
   * @param repositoryMan the new manager
   */
  public void setRepositoryManager(RepositoryManager repositoryMan) {
    managerMap.put(LockssDaemon.REPOSITORY_MANAGER, repositoryMan);
  }

  /**
   * Set the NodeManagerManager
   * @param nodeManMan the new manager
   */
  public void setNodeManagerManager(NodeManagerManager nodeManMan) {
    managerMap.put(LockssDaemon.NODE_MANAGER_MANAGER, nodeManMan);
  }

  /**
   * Set the WatchdogService
   * @param wdogService the new service
   */
  public void setWatchdogService(WatchdogService wdogService) {
    managerMap.put(LockssDaemon.WATCHDOG_SERVICE, wdogService);
  }

  /**
   * Set the MailService
   * @param mailMan the new manager
   */
  public void setMailService(MailService mailMan) {
    managerMap.put(LockssDaemon.MAIL_SERVICE, mailMan);
  }

  /**
   * Set the AlertManager
   * @param alertMan the new manager
   */
  public void setAlertManager(AlertManager alertMan) {
    managerMap.put(LockssDaemon.ALERT_MANAGER, alertMan);
  }

  /**
   * Set the AccountManager
   * @param accountMan the new manager
   */
  public void setAccountManager(AccountManager accountMan) {
    managerMap.put(LockssDaemon.ACCOUNT_MANAGER, accountMan);
  }

  /**
   * Set the RandomManager
   * @param randomMan the new manager
   */
  public void setRandomManager(RandomManager randomMan) {
    managerMap.put(LockssDaemon.RANDOM_MANAGER, randomMan);
  }

  /**
   * Set the KeystoreManager
   * @param keystoreMan the new manager
   */
  public void setKeystoreManager(LockssKeyStoreManager keystoreMan) {
    managerMap.put(LockssDaemon.KEYSTORE_MANAGER, keystoreMan);
  }

  /**
   * Set the HashService
   * @param hashServ the new service
   */
  public void setHashService(HashService hashServ) {
    managerMap.put(LockssDaemon.HASH_SERVICE, hashServ);
  }

  /**
   * Set the SchedService
   * @param schedServ the new service
   */
  public void setSchedService(SchedService schedServ) {
    managerMap.put(LockssDaemon.SCHED_SERVICE, schedServ);
  }

  /**
   * Set the IdentityManager
   * @param idMan the new manager
   */
  public void setIdentityManager(IdentityManager idMan) {
    managerMap.put(LockssDaemon.IDENTITY_MANAGER, idMan);
  }

  /**
   * Set the MetadataManager
   * @param metadataMan the new manager
   */
  public void setMetadataManager(MetadataManager metadataMan) {
    managerMap.put(LockssDaemon.METADATA_MANAGER, metadataMan);
  }

  /**
   * Set the PluginManager
   * @param pluginMan the new manager
   */
  public void setPluginManager(PluginManager pluginMan) {
    managerMap.put(LockssDaemon.PLUGIN_MANAGER, pluginMan);
  }

  /**
   * Set the PollManager
   * @param pollMan the new manager
   */
  public void setPollManager(PollManager pollMan) {
    managerMap.put(LockssDaemon.POLL_MANAGER, pollMan);
  }

  /**
   * Set the ProxyManager
   * @param proxyMgr the new manager
   */
  public void setProxyManager(ProxyManager proxyMgr) {
    managerMap.put(LockssDaemon.PROXY_MANAGER, proxyMgr);
  }

  /**
   * Set the ServletManager
   * @param servletMgr the new manager
   */
  public void setServletManager(ServletManager servletMgr) {
    managerMap.put(LockssDaemon.SERVLET_MANAGER, servletMgr);
  }

  /**
   * Set the TrueZipManager
   * @param tzMgr the new manager
   */
  public void setTrueZipManager(TrueZipManager tzMgr) {
    managerMap.put(LockssDaemon.TRUEZIP_MANAGER, tzMgr);
  }

//   /**
//    * Set the DbManager
//    * @param dbMan the new manager
//    */
//   public void setDbManager(DbManager dbMan) {
//     managerMap.put(LockssDaemon.DB_MANAGER, dbMan);
//   }

  /**
   * Set the MetadataDbManager
   * @param mdDbMan the new manager
   */
  public void setMetadataDbManager(MetadataDbManager mdDbMan) {
    managerMap.put(LockssDaemon.METADATA_DB_MANAGER, mdDbMan);
  }

  /**
   * Set the CounterReportsManager
   * @param counterReportsMan the new manager
   */
  public void setCounterReportsManager(CounterReportsManager counterReportsMan) {
    managerMap.put(LockssDaemon.COUNTER_REPORTS_MANAGER, counterReportsMan);
  }

  /**
   * Set the SubscriptionManager
   * @param subscriptionMan the new manager
   */
  public void setSubscriptionManager(SubscriptionManager subscriptionMan) {
    managerMap.put(LockssDaemon.SUBSCRIPTION_MANAGER, subscriptionMan);
  }

  /**
   * Set the SystemMetrics
   * @param sysMetrics the new metrics
   */
  public void setSystemMetrics(SystemMetrics sysMetrics) {
    managerMap.put(LockssDaemon.SYSTEM_METRICS, sysMetrics);
  }

  /**
   * Set the RemoteApi
   * @param sysMetrics the new metrics
   */
  public void setRemoteApi(RemoteApi rmtApi) {
    managerMap.put(LockssDaemon.REMOTE_API, rmtApi);
  }

  /**
   * Set the Cron
   * @param cron the new cron
   */
  public void setCron(Cron cron) {
    managerMap.put(LockssDaemon.CRON, cron);
  }

  /**
   * Set the EntitlementRegistryClient
   * @param pluginMan the new manager
   */
  public void setEntitlementRegistryClient(EntitlementRegistryClient entitlementRegistryClient) {
    managerMap.put(LockssDaemon.SAFENET_MANAGER, entitlementRegistryClient);
  }

  // AU managers

  /** Create an AU manager instance, mimicking what LockssDaemon does */
  public LockssAuManager newAuManager(String key, ArchivalUnit au) {
    ManagerDesc desc = findAuManagerDesc(key);
    if (desc == null) {
      throw new LockssAppException("No AU ManagerDesc for: " + key);
    }
    log.debug2("Loading manager: " + desc.getKey() + " for " + au);
    try {
      LockssAuManager mgr = initAuManager(desc, au);
      setAuManager(desc, au, mgr);
      return mgr;
    } catch (Exception e) {
      log.error("Error starting au manager", e);
      throw new LockssAppException("Can't load au manager: " + e.toString());
    }
  }

  public void setAuManager(String key, ArchivalUnit au, LockssAuManager mgr) {
    setAuManager(findAuManagerDesc(key), au, mgr);
  }

  void setAuManager(ManagerDesc desc, ArchivalUnit au, LockssAuManager mgr) {
    LinkedMap auMgrMap = (LinkedMap)auManagerMaps.get(au);
    if (auMgrMap == null) {
      auMgrMap = new LinkedMap();
      auManagerMaps.put(au, auMgrMap);
    }
    auMgrMap.put(desc.getKey(), mgr);
  }

  /** AU managers are normally not started on AU creation.  Call this with
   * false to cause them to be started. */
  public void suppressStartAuManagers(boolean val) {
    suppressStartAuManagers = val;
  }

  /** Overridden to prevent managers from being started.  See {@link
   * #suppressStartAuManagers(boolean)} to cause them to be started. */
  public void startOrReconfigureAuManagers(ArchivalUnit au,
					   Configuration auConfig)
      throws Exception {
    if (!suppressStartAuManagers) {
      super.startOrReconfigureAuManagers(au, auConfig);
    }
  }

  /** For tests that override startOrReconfigureAuManagers and want to
   * conditionally start them. */
  public void reallyStartOrReconfigureAuManagers(ArchivalUnit au,
						 Configuration auConfig)
      throws Exception {
    super.startOrReconfigureAuManagers(au, auConfig);
  }

  /** Return ActivityRegulator for AU */
  public ActivityRegulator getActivityRegulator(ArchivalUnit au) {
    try {
      return super.getActivityRegulator(au);
    } catch (IllegalArgumentException e) {
      return (ActivityRegulator)newAuManager(LockssDaemon.ACTIVITY_REGULATOR,
					     au);
    }
  }

  /** Return LockssRepository for AU */
  public LockssRepository getLockssRepository(ArchivalUnit au) {
    try {
      return super.getLockssRepository(au);
    } catch (IllegalArgumentException e) {
      return (LockssRepository)newAuManager(LockssDaemon.LOCKSS_REPOSITORY,
					    au);
    }
  }

  /** Return NodeManager for AU */
  public NodeManager getNodeManager(ArchivalUnit au) {
    try {
      return super.getNodeManager(au);
    } catch (IllegalArgumentException e) {
      return (NodeManager)newAuManager(LockssDaemon.NODE_MANAGER, au);
    }
  }

  /** Return HistoryRepository for AU */
  public HistoryRepository getHistoryRepository(ArchivalUnit au) {
    try {
      return super.getHistoryRepository(au);
    } catch (IllegalArgumentException e) {
      return (HistoryRepository)newAuManager(LockssDaemon.HISTORY_REPOSITORY,
          au);
    }
  }

  /**
   * Set the ActivityRegulator for a given AU.
   * @param actReg the new regulator
   * @param au the ArchivalUnit
   */
  public void setActivityRegulator(ActivityRegulator actReg, ArchivalUnit au) {
    setAuManager(ACTIVITY_REGULATOR, au, actReg);
  }

  /**
   * Set the LockssRepository for a given AU.
   * @param repo the new repository
   * @param au the ArchivalUnit
   */
  public void setLockssRepository(LockssRepository repo, ArchivalUnit au) {
    setAuManager(LOCKSS_REPOSITORY, au, repo);
  }

  /**
   * Set the NodeManager for a given AU.
   * @param nodeMan the new manager
   * @param au the ArchivalUnit
   */
  public void setNodeManager(NodeManager nodeMan, ArchivalUnit au) {
    setAuManager(NODE_MANAGER, au, nodeMan);
  }

  /**
   * Set the HistoryRepository for a given AU.
   * @param histRepo the new repository
   * @param au the ArchivalUnit
   */
  public void setHistoryRepository(HistoryRepository histRepo, ArchivalUnit au) {
    setAuManager(HISTORY_REPOSITORY, au, histRepo);
  }

  /**
   * <p>Forcibly sets the ICP manager to a new value.</p>
   * @param icpManager A new ICP manager to use.
   */
  public void setIcpManager(IcpManager icpManager) {
    managerMap.put(LockssDaemon.ICP_MANAGER, icpManager);
  }

  private boolean daemonInited = false;
  private boolean daemonRunning = false;

  /**
   * @return true iff all managers have been inited
   */
  public boolean isDaemonInited() {
    return daemonInited;
  }

  // need to override this one too, inherited from LockssApp
  public boolean isAppInited() {
    return isDaemonInited();
  }

  /**
   * @return true iff all managers have been started
   */
  public boolean isDaemonRunning() {
    return daemonRunning;
  }

  // need to override this one too, inherited from LockssApp
  public boolean isAppRunning() {
    return isDaemonRunning();
  }

  /** set daemonInited
   * @param val true if inited
   */
  public void setDaemonInited(boolean val) {
    daemonInited = val;
  }

  /** set daemonRunning
   * @param val true if running
   */
  public void setDaemonRunning(boolean val) {
    daemonRunning = val;
  }

  public void setAusStarted(boolean val) {
    if (val) {
      ausStarted.fill();
    } else {
      ausStarted = new OneShotSemaphore();
    }
  }
}
