/*

Copyright (c) 2013-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.app;

import java.util.*;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.JMSException;

import org.lockss.alert.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.jms.*;
import org.lockss.log.*;
import org.lockss.app.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.util.time.Deadline;
import org.lockss.util.jms.*;

/**
 * Base implementation of LockssManager
 */

public abstract class BaseLockssManager implements LockssManager {

  private static final L4JLogger log = L4JLogger.getLogger();

  protected LockssApp theApp = null;
  private Configuration.Callback configCallback;
  protected boolean isInited = false;
  protected boolean isStarted = false;
  protected boolean shuttingDown = false;
  protected OneShotSemaphore startedSem = new OneShotSemaphore();


  protected String getClassName() {
    return ClassUtil.getClassNameWithoutPackage(getClass());
  }

  /**
   * Called to initialize each service in turn.  Service should extend
   * this to perform any internal initialization necessary before service
   * can be called from outside.  No calls to other services may be made in
   * this method.
   * @param app the {@link LockssApp}
   * @throws LockssAppException
   */
  public void initService(LockssApp app) throws LockssAppException {
    isInited = true;
    log.debug2("{}.initService()", getClassName());
    if(theApp == null) {
      theApp = app;
      registerDefaultConfigCallback();
    }
    else {
      throw new LockssAppException("Multiple Instantiation.");
    }
  }

  /** Called to start each service in turn, after all services have been
   * initialized.  Service should extend this to perform any startup
   * necessary. */
  public void startService() {
    isStarted = true;
    log.debug2("{}.startService()", getClassName());
  }

  /** Called by LockssApp after service is started.  (Here because Java
   * doesn't have "around" methods) */
  public void serviceStarted() {
    startedSem.fill();
  }

  /** Called to stop a service.  Service should extend this to stop all
   * ongoing activity (<i>eg</i>, threads). */
  public void stopService() {
    log.debug2("{}.stopService()", getClassName());
    stopJms();
    shuttingDown = true;
    // checkpoint here
    unregisterConfig();
    // Logically, we should set theApp = null here, but that breaks several
    // tests, which sometimes stop managers twice.
//     theApp = null;
  }

  /** Return the app instance in which this manager is running */
  public LockssApp getApp() {
    return theApp;
  }

  /** Return true if the manager is shutting down */
  public boolean isShuttingDown() {
    return shuttingDown;
  }

  /**
   * Return true iff all the app services have been initialized.
   * @return true if the app is inited
   */
  protected boolean isAppInited() {
    return theApp.isAppInited();
  }

  /**
   * Return true iff this manager's initService() has been called.
   * @return true if the manager is inited
   */
  public boolean isInited() {
    return isInited;
  }

  /** Wait until the service is started.
   * @param timer limits the time to wait.  If null, returns immediately.
   * @return true if started, false if timer expired.
   */
  public boolean waitStarted(Deadline timer) {
    while (!startedSem.isFull() && !timer.expired()) {
      try {
	startedSem.waitFull(timer);
      } catch (InterruptedException e) {
	// no action - check timer
      }
    }
    return startedSem.isFull();
  }

  /**
   * Wait until the service has is started.  Return true if true iff this manager's startService() has been called.
   * @return true if the manager is started
   */
  public boolean waitStarted() {
    return startedSem.isFull();
  }

  /**
   * Return true iff this manager's startService() has been called.  Note
   * this becomes true as soon as startService() is called, while
   * waitStarted() doesn't return until startService() has completed
   * @return true if the manager is started
   */
  public boolean isStarted() {
    return isStarted;
  }

  public <T> T getManagerByType(Class<T> mgrType) {
    return theApp.getManagerByType(mgrType);
  }

  private void registerConfigCallback(Configuration.Callback callback) {
    if(callback == null || this.configCallback != null) {
      throw new LockssAppException("Invalid callback registration: "
				       + callback);
    }
    configCallback = callback;

    getConfigManager().registerConfigurationCallback(configCallback);
  }

  private void registerDefaultConfigCallback() {
    if (this instanceof ConfigurableManager) {
      Configuration.Callback cb =
	new DefaultConfigCallback((ConfigurableManager)this);
      registerConfigCallback(cb);
    }
  }

  private void unregisterConfig() {
    if(configCallback != null) {
      getConfigManager().unregisterConfigurationCallback(configCallback);
      configCallback = null;
    }
  }

  /** Convenience method to (re)invoke the manager's setConfig(new, old,
   * ...) method with the current config and empty previous config. */
  protected void resetConfig() {
    if (this instanceof ConfigurableManager) {
      ConfigurableManager cmgr = (ConfigurableManager)this;
      Configuration cur = CurrentConfig.getCurrentConfig();
      cmgr.setConfig(cur, ConfigManager.EMPTY_CONFIGURATION,
		     cur.differences(null));  // all differences
    } else {
      throw new RuntimeException("Not a ConfigurableManager");
    }
  }

  /** Wait forever for the repo service to be ready.
   * @return true if the repo is ready, false if not or if the status can't
   * be determined (no RestServicesManager, no binding for the repo svc, no
   * status available)
   */
  protected boolean waitForRepo() {
    return waitForRepo(Deadline.MAX);
  }

  /** Wait for the repo service to be ready.
   * @param until time at which to give up waiting
   * @return true if the repo is ready, false if not or if the status can't
   * be determined (no RestServicesManager, no binding for the repo svc, no
   * status available, timed out
   */
  protected boolean waitForRepo(Deadline until) {
    RestServicesManager svcsMgr =
      getApp().getManagerByType(RestServicesManager.class);
    if (svcsMgr != null) {
      RestServicesManager.ServiceStatus stat =
	svcsMgr.waitServiceReady(ServiceDescr.SVC_REPO, until);
      if (stat != null) {
	return stat.isReady();
      }
    }
    return false;
  }

  /** Return true if the repository on which the specified AU resides is
   * ready
   * @param auid
   * @return true if the repo is ready, false if not
   */
  protected boolean isRepoReady(String auid) {
    RepositoryManager repoMgr =
      getApp().getManagerByType(RepositoryManager.class);
    if (repoMgr == null) {
      throw new IllegalStateException("No RepositoryManager");
    }
    return repoMgr.isRepoReady(auid);
  }

  // JMS Producer and Consumer setup

  protected JmsConsumer jmsConsumer;
  protected JmsProducer jmsProducer;

  protected JMSManager getJMSManager() {
    return theApp.getManagerByType(JMSManager.class);
  }

  /** Establish a JMS listener for the topic; store it in <tt>jmsConsumer</tt>
   * @param clientId
   * @param topicName
   * @param listener receives incoming messages
   */
  protected void setUpJmsReceive(String clientId,
				 String topicName,
				 MessageListener listener) {
    setUpJmsReceive(clientId, topicName, false, listener);
  }

  /** Establish a JMS listener for the topic; store it in <tt>jmsConsumer</tt>
   * @param clientId
   * @param topicName
   * @param noLocal if true, the listener will not receive messages sent by
   * a Producer created with the same connection
   * @param listener receives incoming messages
   */
  protected void setUpJmsReceive(String clientId,
				 String topicName,
				 boolean noLocal,
				 MessageListener listener) {
    log.debug("Creating consumer for " + getClassName());
    try {
      jmsConsumer =
	getJMSManager().getJmsFactory()
	.createTopicConsumer(clientId, topicName, noLocal, listener);
    } catch (JMSException e) {
      log.fatal("Couldn't create jms consumer for " + getClassName(), e);
    }
  }

  /** Establish a JMS producer for the topic; store it in <tt>jmsProducer</tt>.
   * @param clientId
   * @param topicName
   */
  protected void setUpJmsSend(String clientId, String topicName) {
    log.debug("Creating producer for " + getClassName());
    try {
      jmsProducer =
	getJMSManager().getJmsFactory()
	.createTopicProducer(clientId, topicName);
    } catch (JMSException e) {
      log.error("Couldn't create jms producer for " + getClassName(), e);
    }
  }

  /** Cleanly stop the JMS producer and/or consumer */
  protected void stopJms() {
    JmsProducer p = jmsProducer;
    if (p != null) {
      try {
	jmsProducer = null;
	p.close();
      } catch (JMSException e) {
	log.error("Couldn't stop jms producer for " + getClassName(), e);
      }
    }
    JmsConsumer c = jmsConsumer;
    if (c != null) {
      try {
	jmsConsumer = null;
	c.close();
      } catch (JMSException e) {
	log.error("Couldn't stop jms consumer for " + getClassName(), e);
      }
    }
  }

  /** Subclasses should override to handle recieved Map messages */
  protected void receiveMessage(Map map) {
  }

  /** A MessageListener suitable for receiving messages whose payload is a
   * map.  Dispatches received messages to {@link #receiveMessage(Map)} */
  public class MapMessageListener extends JmsConsumerImpl.SubscriptionListener {

    public MapMessageListener(String listenerName) {
      super(listenerName);
    }

    @Override
    public void onMessage(Message message) {
      try {
        Object msgObject =  JmsUtil.convertMessage(message);
	if (msgObject instanceof Map) {
	  receiveMessage((Map)msgObject);
	} else {
	  log.warn("Unknown notification type, not Map: " + msgObject);
	}
      } catch (JMSException e) {
	log.warn("Failed to decode message: {}", message, e);
      }
    }
  }



  // Convenience manager accessors

  public ConfigManager getConfigManager() {
    return theApp.getConfigManager();
  }

  public AlertManager getAlertManager() {
    return theApp.getAlertManager();
  }

  private static class DefaultConfigCallback
    implements Configuration.Callback {

    ConfigurableManager mgr;
    DefaultConfigCallback(ConfigurableManager mgr) {
      this.mgr = mgr;
    }

    public void configurationChanged(Configuration newConfig,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {
      mgr.setConfig(newConfig, prevConfig, changedKeys);
    }

    public void auConfigChanged(String auid) {
      mgr.auConfigChanged(auid);
    }

    public void auConfigRemoved(String auid) {
      mgr.auConfigRemoved(auid);
    }
  }

  /**
   * Provides the default root directory for temporary files.
   * 
   * @return a String with the root directory for temporary files.
   */
  protected String getDefaultTempRootDirectory() {
    final String DEBUG_HEADER = "getDefaultTempRootDirectory(): ";
    String defaultTempRootDir = null;
    Configuration config = ConfigManager.getCurrentConfig();

    @SuppressWarnings("unchecked")
    List<String> dSpaceList =
	config.getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);

    if (dSpaceList != null && !dSpaceList.isEmpty()) {
      defaultTempRootDir = dSpaceList.get(0);
    } else {
      defaultTempRootDir = config.get(ConfigManager.PARAM_TMPDIR);
    }

    log.debug2(DEBUG_HEADER + "defaultTempDbRootDir = '"
	       + defaultTempRootDir + "'.");
    return defaultTempRootDir;
  }
}
