/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jms;

import java.io.*;
import java.util.*;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.*;
import org.apache.activemq.transport.DefaultTransportListener;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.log.*;
import org.lockss.util.*;
import org.lockss.util.jms.*;
import org.lockss.config.*;

/** Manages (starts & stops) an embedded ActiveMQ broker */
public class JMSManager extends BaseLockssManager
  implements ConfigurableManager  {

  protected static L4JLogger log = L4JLogger.getLogger();

  public static final String PREFIX = Configuration.PREFIX + "jms.";
  public static final String BROKER_PREFIX = PREFIX + "broker.";
  public static final String CONNECT_PREFIX = PREFIX + "connect.";

  /** If true, an ActiveMQ broker will be started at the address specified
   * by {@value #PARAM_BROKER_URI} or {@value #PARAM_CONNECT_URI}.  Only
   * checked at startup. */
  public static final String PARAM_START_BROKER = BROKER_PREFIX + "start";
  public static final boolean DEFAULT_START_BROKER = false;

  /** URL specifying protocol & address at which the broker will be started
   * (if {@value #PARAM_START_BROKER} is true), and (unless {@value
   * #PARAM_CONNECT_URI} is set), the URI to which producers and consumers
   * will connect.  Only checked at startup.  Usually one of:<ul>
   * <li><code>tcp://<i>hostname</i>:<i>port</i></code></li>
   * <li><code>vm://localhost?create=false</code></li></ul>
   *
   * <a href="http://activemq.apache.org/configuring-transports.html">See
   * here</a> for a full list of transport protocols. */
  public static final String PARAM_BROKER_URI = BROKER_PREFIX + "uri";
  public static String DEFAULT_BROKER_URI =
    "vm://localhost?create=true&broker.persistent=false";
//   public static String DEFAULT_BROKER_URI = "tcp://localhost:61616";

  /** Broker URI to which producers and consumers will connect.  <i>Eg</i>,
   * <code>failover:tcp://<i>hostname</i>:<i>port</i></code>&nbsp;.  If not
   * set {@value #PARAM_BROKER_URI} is used. */
  public static final String PARAM_CONNECT_URI = CONNECT_PREFIX + "uri";

  /** If true, use a failover transport to talk to the broker; see <a
   * href="http://activemq.apache.org/failover-transport-reference">here</a>. */
  public static final String PARAM_CONNECT_FAILOVER =
    CONNECT_PREFIX + "failover";
  public static boolean DEFAULT_CONNECT_FAILOVER = true;

  /** If true, the broker will use a persistent store */
  public static final String PARAM_IS_PERSISTENT =
    BROKER_PREFIX + "isPersistent";
  public static final boolean DEFAULT_IS_PERSISTENT = false;

  /** Persistent storage directory path.  If not set, defaults to
   * <i>diskSpacePaths</i><code>/activemq</code if that is set */
  public static final String PARAM_PERSISTENT_DIR =
    BROKER_PREFIX + "persistentDir";
  public static final String DEFAULT_PERSISTENT_DIR = "activemq";

  /** If true the broker will enable the JMX management interface */
  public static final String PARAM_ENABLE_JMX = BROKER_PREFIX + "enableJmx";
  public static final boolean DEFAULT_ENABLE_JMX = false;

  /** JMX listen port */
  public static final String PARAM_JMX_PORT = BROKER_PREFIX + "jmxPort";
  public static final int DEFAULT_JMX_PORT = 24629;

  private String brokerUri = DEFAULT_BROKER_URI;
  private String connectUri = DEFAULT_BROKER_URI;
  private boolean startBroker = DEFAULT_START_BROKER;

  private BrokerService broker;
  private Map<String,Connection> connectionMap = new HashMap<>();

  public void startService() {
    super.startService();
    log.info("startBroker: " + startBroker);
    if (startBroker) {
      broker = createBroker(brokerUri); 
    }
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      brokerUri = config.get(PARAM_BROKER_URI, DEFAULT_BROKER_URI);

      String curi = config.get(PARAM_CONNECT_URI, brokerUri);
      if (config.getBoolean(PARAM_CONNECT_FAILOVER, DEFAULT_CONNECT_FAILOVER)) {
	curi = "failover:(" + curi + ")";
      }
      connectUri = curi;
    }
    startBroker = config.getBoolean(PARAM_START_BROKER, DEFAULT_START_BROKER);
  }

  public void stopService() {
    closeConnections();
    if (broker != null) {
      try {
	broker.stop();
      } catch (Exception e) {
	log.error("Couldn't stop ActiveMQ broker", e);
      }
      broker = null;
    }
    super.stopService();
  }

  private void closeConnections() {
    synchronized (connectionMap) {
      for (String uri : connectionMap.keySet()) {
	try {
	  Connection conn = connectionMap.get(uri);
	  conn.close();
	} catch (JMSException e) {
	  log.error("Couldn't close JMS connection to {}", uri, e);
	}
      }
    }
  }

  public static JmsFactory getJmsFactoryStatic() {
    JMSManager mgr = LockssDaemon.getManagerByTypeStatic(JMSManager.class);
    return mgr.getJmsFactory();
  }

  public JmsFactory getJmsFactory() {
    return new JmsFactoryImpl(this);
  }

  /** Start and return a broker with config given by the params under
   * {@value #BROKER_PREFIX} */
  public static BrokerService createBroker(String uri) {
    Configuration config = ConfigManager.getCurrentConfig();
    boolean isPersistent = config.getBoolean(PARAM_IS_PERSISTENT,
					     DEFAULT_IS_PERSISTENT);
    File persistentDir = null;
    if (isPersistent) {
      ConfigManager cfgMgr = ConfigManager.getConfigManager();
      persistentDir = cfgMgr.findConfiguredDataDir(PARAM_PERSISTENT_DIR,
						      DEFAULT_PERSISTENT_DIR);
    }

    try {
      BrokerService res = new BrokerService();
//     res.setBrokerName("foo");
      StringBuilder sb = new StringBuilder();
      sb.append("Started broker ");
      sb.append(uri);

      res.setPersistent(isPersistent);
      sb.append(", persistent: ");
      sb.append(isPersistent);

      if (isPersistent && persistentDir != null) {
	PersistenceAdapter pa = res.getPersistenceAdapter();
	pa.setDirectory(new File(persistentDir.toString()));
	sb.append(" (");
	sb.append(persistentDir.toString());
	sb.append(")");
      }

      // This enables JMX for the whole JVM, so probably belongs elsewhere.
      // (It's odd that a system-wide management interface would be enabled
      // in the broker config.  I assume it can be enabled in other ways
      // too.)
      if (config.getBoolean(PARAM_ENABLE_JMX, DEFAULT_ENABLE_JMX)) {
	int port = config.getInt(PARAM_JMX_PORT, DEFAULT_JMX_PORT);
	sb.append(", jmxPort: ");
	sb.append(port);
	res.setUseJmx(true);
	res.getManagementContext().setConnectorPort(port);
      } else {
	res.setUseJmx(false);
      }

      res.addConnector(uri); 
      res.start();
      log.info(sb.toString());
      return res;
    } catch (Exception e) {
      log.error("Couldn't start ActiveMQ broker: " + uri, e);
      return null;
    }
  }

  /** Return the URI that should be used to connect to the JMS broker */
  public String getConnectUri() {
    return connectUri;
  }

  /** Return the URI that the JMS broker should listen on */
  public String getBrokerUri() {
    return brokerUri;
  }

  /** Return a connection to the configured broker */
  public Connection getConnection() throws JMSException {
    return getConnection(getConnectUri());
  }

  /** Return a connection to the specified broker */
  // If there's ever more than one broker, this should be changed to use
  // StripedExecutorService to run concurrent connect threads, one for each
  // broker URI.
  public Connection getConnection(String uri) throws JMSException {
    synchronized (connectionMap) {
      Connection conn = connectionMap.get(uri);
      if (conn == null) {
	ActiveMQConnectionFactory connectionFactory =
	  new ActiveMQConnectionFactory(uri);
	// create a new Connection
	conn = connectionFactory.createConnection();
	if (conn instanceof ActiveMQConnection) {
	  ActiveMQConnection amqConn = (ActiveMQConnection)conn;
	  amqConn.addTransportListener(new DefaultTransportListener() {
	      @Override
	      public void transportInterupted() {
		onTransportInterrupted();
	      }
	      @Override
	      public void transportResumed() {
		// Calling MessageProducer inside transportResumed() can
		// cause deadlock
		new Thread(() -> {onTransportResumed();}).start();
	      }});
	  connectionMap.put(uri, conn);
	} else {
	  log.warn("Couldn't add transport listener as {} isn't an ActiveMQConnection", conn);
	}
      }
      // start the connection in order to receive messages
      conn.start();
      return conn;
    }
  }

  void onTransportInterrupted() {
    synchronized (transportListeners) {
      for (TransportListener tl : transportListeners) {
	tl.transportInterrupted();
      }
    }
  }

  void onTransportResumed() {
    synchronized (transportListeners) {
      for (TransportListener tl : transportListeners) {
	tl.transportResumed();
      }
    }
  }

  private List<TransportListener> transportListeners = new ArrayList<>();

  public void registerTransportListener(TransportListener tl) {
    synchronized (transportListeners) {
      if (!transportListeners.contains(tl)) {
	transportListeners.add(tl);
      }
    }
  }

  public void unregisterTransportListener(TransportListener tl) {
    synchronized (transportListeners) {
      transportListeners.remove(tl);
    }
  }

  /** Clients can register an instance of this to listen for JMS transport
   * events */
  public interface TransportListener {
    default public void transportInterrupted() {
    }
    default public void transportResumed() {
    }
  }
}
