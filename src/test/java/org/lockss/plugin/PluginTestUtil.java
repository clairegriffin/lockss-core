/*
 * $Id$
 *

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.plugin.simulated.*;
import org.lockss.util.*;
import org.lockss.util.test.PrivilegedAccessor;
import org.lockss.app.*;
import org.lockss.config.*;

/**
 * Utilities for manipulating plugins and their components in tests
 */

public class PluginTestUtil {
  static Logger log = Logger.getLogger();
  static List aulist = new LinkedList();

  public static void registerArchivalUnit(Plugin plug, ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    log.debug3(mgr.toString());
    String plugid = au.getPluginId();
    log.debug("registerArchivalUnit plugin = " + plug +
	      "au = " + au);
    if (plug != mgr.getPlugin(plugid)) {
      try {
	PrivilegedAccessor.invokeMethod(mgr, "setPlugin",
					ListUtil.list(plugid, plug).toArray());
      } catch (Exception e) {
	log.error("Couldn't register AU", e);
	throw new RuntimeException(e.toString());
      }
    }
    PluginTestable tp = (PluginTestable)plug;
    tp.registerArchivalUnit(au);
    try {
      PrivilegedAccessor.invokeMethod(mgr, "putAuInMap",
				      ListUtil.list(au).toArray());
    } catch (InvocationTargetException e) {
      log.error("Couldn't register AU", e);
      log.error("Nested", e.getTargetException());
      throw new RuntimeException(e.toString());
    } catch (Exception e) {
      log.error("Couldn't register AU", e);
      throw new RuntimeException(e.toString());
    }
    aulist.add(au);
  }

  public static void registerArchivalUnit(ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    String plugid = au.getPluginId();
    Plugin plug = mgr.getPlugin(plugid);
    log.debug("registerArchivalUnit id = " + au.getAuId() +
	      ", pluginid = " + plugid + ", plugin = " + plug);
    if (plug == null) {
      MockPlugin mp = new MockPlugin();
      mp.setPluginId(plugid);
      plug = mp;
    }
    registerArchivalUnit(plug, au);
  }

  /*
  public static void registerArchivalUnit(ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    log.debug(mgr.toString());
    String plugid = au.getPluginId();
    Plugin plug = mgr.getPlugin(plugid);
    log.debug("registerArchivalUnit id = " + au.getAuId() +
	      ", pluginid = " + plugid + ", plugin = " + plug);
    if (plug == null) {
      MockPlugin mp = new MockPlugin();
      mp.setPluginId(plugid);
      plug = mp;
      try {
	PrivilegedAccessor.invokeMethod(mgr, "setPlugin",
					ListUtil.list(plugid, mp).toArray());
      } catch (Exception e) {
	log.error("Couldn't register AU", e);
	throw new RuntimeException(e.toString());
      }
    }
    PluginTestable tp = (PluginTestable)plug;
    tp.registerArchivalUnit(au);
    try {
      PrivilegedAccessor.invokeMethod(mgr, "putAuMap",
				      ListUtil.list(plug, au).toArray());
    } catch (Exception e) {
      log.error("Couldn't register AU", e);
      throw new RuntimeException(e.toString());
    }
    aulist.add(au);
  }
  */
  public static void unregisterArchivalUnit(ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    String plugid = au.getPluginId();
    Plugin plug = mgr.getPlugin(plugid);
    if (plug != null) {
      PluginTestable tp = (PluginTestable)plug;
      tp.unregisterArchivalUnit(au);
      aulist.remove(au);
    }
    mgr.stopAu(au, AuEvent.forAu(au, AuEvent.Type.Delete));
  }

  public static void unregisterAllArchivalUnits() {
    log.debug("unregisterAllArchivalUnits()");
    List aus = new ArrayList(aulist);
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      unregisterArchivalUnit((ArchivalUnit)iter.next());
    }
  }

  public static Plugin findPlugin(String pluginName) {
    return findPlugin(pluginName, null);
  }

  public static Plugin findPlugin(String pluginName, ClassLoader loader) {
    PluginManager pluginMgr = getPluginManager();
    String key = pluginMgr.pluginKeyFromName(pluginName);
    pluginMgr.ensurePluginLoaded(key, loader);
    return pluginMgr.getPlugin(key);
  }

  public static Plugin findPlugin(Class pluginClass) {
    return findPlugin(pluginClass.getName());
  }

  public static ArchivalUnit createAu(Plugin plugin,
				      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return getPluginManager().createAu(plugin, auConfig,
                                       AuEvent.model(AuEvent.Type.Create));
  }

  public static ArchivalUnit createAndStartAu(Plugin plugin,
					      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return startAu(createAu(plugin, auConfig));
  }

  static ArchivalUnit startAu(ArchivalUnit au) {
    LockssDaemon daemon = au.getPlugin().getDaemon();
//     daemon.getLockssRepository(au).startService();
    return au;
  }

  public static ArchivalUnit createAu(String pluginName,
				      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAu(findPlugin(pluginName), auConfig);
  }

  public static ArchivalUnit createAndStartAu(String pluginName,
					      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAndStartAu(findPlugin(pluginName), auConfig);
  }

  public static ArchivalUnit createAu(Class pluginClass,
				      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAu(findPlugin(pluginClass.getName()), auConfig);
  }

  public static ArchivalUnit createAndStartAu(Class pluginClass,
					      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAndStartAu(findPlugin(pluginClass.getName()), auConfig);
  }

  public static SimulatedArchivalUnit createSimAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return (SimulatedArchivalUnit)createAu(findPlugin(SimulatedPlugin.class),
					   auConfig);
  }

  static Configuration getAuConfig(TdbAu tau) {
    PluginManager mgr = getPluginManager();
    Plugin plugin = tau.getPlugin(mgr);
    TitleConfig tc = new TitleConfig(tau, plugin);
    return tc.getConfig();
  }

  public static ArchivalUnit createAu(TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    PluginManager mgr = getPluginManager();
    Plugin plugin = findPlugin(tau.getPluginId());
    return createAu(plugin, getAuConfig(tau));
  }

  public static ArchivalUnit createAu(Plugin plugin, TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    return createAu(plugin, getAuConfig(tau));
  }

  public static ArchivalUnit createAndStartAu(TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    return startAu(createAu(tau));
  }

  public static ArchivalUnit createAndStartAu(Plugin plugin, TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    return startAu(createAu(plugin, tau));
  }

  public static SimulatedArchivalUnit
    createAndStartSimAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAndStartSimAu(SimulatedPlugin.class, auConfig);
  }

  public static SimulatedArchivalUnit
    createAndStartSimAu(Class pluginClass, Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return (SimulatedArchivalUnit)createAndStartAu(pluginClass, auConfig);
  }

  public static void crawlSimAu(SimulatedArchivalUnit sau) {
    if (!sau.hasContentTree()) {
//       log.debug("Creating simulated content tree: " + sau.getParamMap());
      sau.generateContentTree();
    }
    log.debug("Crawling simulated content");
    NoCrawlEndActionsFollowLinkCrawler crawler =
      new NoCrawlEndActionsFollowLinkCrawler(sau, AuUtil.getAuState(sau));
    //crawler.setCrawlManager(crawlMgr);
    crawler.doCrawl();
  }


  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu) {
    return copyAu(fromAu, toAu, null, null, null);
  }

  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu,
			       String ifMatch) {
    return copyAu(fromAu, toAu, ifMatch, null, null);
  }

  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu,
			       String ifMatch, String pat, String rep) {
    return copyCus(fromAu.getAuCachedUrlSet(), toAu, ifMatch, pat, rep);
  }

  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu) {
    return copyCus(fromCus, toAu, null, null, null);
  }

  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu,
				String ifMatch, String pat, String rep) {
    boolean res = true;
    ArchivalUnit fromAu = fromCus.getArchivalUnit();
    Pattern pattern = null;
    if (pat != null) {
      pattern = Pattern.compile(pat);
    }
    Pattern ifMatchPat = null;
    if (ifMatch != null) {
      ifMatchPat = Pattern.compile(ifMatch);
    }
    for (CachedUrl cu : fromCus.getCuIterable()) {
      try {
        String fromUrl = cu.getUrl();
        String toUrl = fromUrl;
        if (ifMatchPat != null) {
          Matcher mat = ifMatchPat.matcher(fromUrl);
          if (!mat.find()) {
            log.debug3("no match: " + fromUrl + ", " + ifMatchPat);
            continue;
          }
        }
        if (pattern != null) {
          Matcher mat = pattern.matcher(fromUrl);
          toUrl = mat.replaceAll(rep);
        }
        CIProperties props = cu.getProperties();
        if (props == null) {
        }
        UrlCacher uc = toAu.makeUrlCacher(
            new UrlData(cu.getUnfilteredInputStream(), props, toUrl));
        uc.storeContent();
        if (!toUrl.equals(fromUrl)) {
          log.debug2("Copied " + fromUrl + " to " + toUrl);
        } else {
          log.debug2("Copied " + fromUrl);
        }
      } catch (Exception e) {
        log.error("Couldn't copy " + cu.getUrl(), e);
        res = false;
      } finally {
        cu.release();
      }
    }
    return res;
  }

  public static List<String> urlsOf(final Iterable<CachedUrl> iterable) {
    return new ArrayList<String>() {{
      for (CachedUrl cu : iterable) {
	add(cu.getUrl());
      }
    }};
  }

  public static List<String> urlsOf(final Iterator<CachedUrlSetNode> iter) {
    return new ArrayList<String>() {{
      while (iter.hasNext()) {
	add(iter.next().getUrl());
      }
    }};
  }

  private static PluginManager getPluginManager() {
    return
      (PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
  }

}
