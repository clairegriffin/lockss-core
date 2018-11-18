/*

Copyright (c) 2007-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.io.*;
import java.util.*;
import java.net.*;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.exploded.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.state.*;

/**
 * Functional tests for the WARC file crawler.  It
 * does not test the non-WARC file functionality,
 * which is provided by FollowLinkCrawler.
 *
 * Uses ActualWarcContentGenerator to create a
 * web site with a permission page that links to
 * an WARC file containing the rest of the content
 * that in the FollowLinkCrawler case would have
 * been generated by SimulatedContentGenerator.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class FuncWarcExploder2 extends LockssTestCase {
  static Logger log = Logger.getLogger();

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  PluginManager pluginMgr;
  int lastCrawlResult = Crawler.STATUS_UNKNOWN;
  String lastCrawlMessage = null;
  private CrawlManagerImpl crawlMgr;

  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int maxDepth=DEFAULT_MAX_DEPTH;
  static final String GOOD_YEAR = "1968";
  static final String COLLECTION = "SomeStuff";

  static String[] url = {
"http://www.content.org/branch1/branch1/branch1/001file.bin",
"http://www.content.org/branch1/branch1/branch1/002file.bin",
"http://www.content.org/branch1/branch1/branch1/index.html",
"http://www.content.org/branch1/branch1/001file.bin",
"http://www.content.org/branch1/branch1/002file.bin",
"http://www.content.org/branch1/branch1/branch2/001file.bin",
"http://www.content.org/branch1/branch1/branch2/002file.bin",
"http://www.content.org/branch1/branch1/branch2/index.html",
"http://www.content.org/branch1/branch1/index.html",
"http://www.content.org/branch1/001file.bin",
"http://www.content.org/branch1/002file.bin",
"http://www.content.org/branch1/branch2/branch1/001file.bin",
"http://www.content.org/branch1/branch2/branch1/002file.bin",
"http://www.content.org/branch1/branch2/branch1/index.html",
"http://www.content.org/branch1/branch2/001file.bin",
"http://www.content.org/branch1/branch2/002file.bin",
"http://www.content.org/branch1/branch2/branch2/001file.bin",
"http://www.content.org/branch1/branch2/branch2/002file.bin",
"http://www.content.org/branch1/branch2/branch2/index.html",
"http://www.content.org/branch1/branch2/index.html",
"http://www.content.org/branch1/index.html",
"http://www.content.org/001file.bin",
"http://www.content.org/002file.bin",
"http://www.content.org/branch2/branch1/branch1/001file.bin",
"http://www.content.org/branch2/branch1/branch1/002file.bin",
"http://www.content.org/branch2/branch1/branch1/index.html",
"http://www.content.org/branch2/branch1/001file.bin",
"http://www.content.org/branch2/branch1/002file.bin",
"http://www.content.org/branch2/branch1/branch2/001file.bin",
"http://www.content.org/branch2/branch1/branch2/002file.bin",
"http://www.content.org/branch2/branch1/branch2/index.html",
"http://www.content.org/branch2/branch1/index.html",
"http://www.content.org/branch2/001file.bin",
"http://www.content.org/branch2/002file.bin",
"http://www.content.org/branch2/branch2/branch1/001file.bin",
"http://www.content.org/branch2/branch2/branch1/002file.bin",
"http://www.content.org/branch2/branch2/branch1/index.html",
"http://www.content.org/branch2/branch2/001file.bin",
"http://www.content.org/branch2/branch2/002file.bin",
"http://www.content.org/branch2/branch2/branch2/001file.bin",
"http://www.content.org/branch2/branch2/branch2/002file.bin",
"http://www.content.org/branch2/branch2/branch2/index.html",
"http://www.content.org/branch2/branch2/index.html",
"http://www.content.org/branch2/index.html",
"http://www.website.org/branch1/branch1/branch1/001file.bin",
"http://www.website.org/branch1/branch1/branch1/002file.bin",
"http://www.website.org/branch1/branch1/branch1/index.html",
"http://www.website.org/branch1/branch1/001file.bin",
"http://www.website.org/branch1/branch1/002file.bin",
"http://www.website.org/branch1/branch1/branch2/001file.bin",
"http://www.website.org/branch1/branch1/branch2/002file.bin",
"http://www.website.org/branch1/branch1/branch2/index.html",
"http://www.website.org/branch1/branch1/index.html",
"http://www.website.org/branch1/001file.bin",
"http://www.website.org/branch1/002file.bin",
"http://www.website.org/branch1/branch2/branch1/001file.bin",
"http://www.website.org/branch1/branch2/branch1/002file.bin",
"http://www.website.org/branch1/branch2/branch1/index.html",
"http://www.website.org/branch1/branch2/001file.bin",
"http://www.website.org/branch1/branch2/002file.bin",
"http://www.website.org/branch1/branch2/branch2/001file.bin",
"http://www.website.org/branch1/branch2/branch2/002file.bin",
"http://www.website.org/branch1/branch2/branch2/index.html",
"http://www.website.org/branch1/branch2/index.html",
"http://www.website.org/branch1/index.html",
"http://www.website.org/001file.bin",
"http://www.website.org/002file.bin",
"http://www.website.org/branch2/branch1/branch1/001file.bin",
"http://www.website.org/branch2/branch1/branch1/002file.bin",
"http://www.website.org/branch2/branch1/branch1/index.html",
"http://www.website.org/branch2/branch1/001file.bin",
"http://www.website.org/branch2/branch1/002file.bin",
"http://www.website.org/branch2/branch1/branch2/001file.bin",
"http://www.website.org/branch2/branch1/branch2/002file.bin",
"http://www.website.org/branch2/branch1/branch2/index.html",
"http://www.website.org/branch2/branch1/index.html",
"http://www.website.org/branch2/001file.bin",
"http://www.website.org/branch2/002file.bin",
"http://www.website.org/branch2/branch2/branch1/001file.bin",
"http://www.website.org/branch2/branch2/branch1/002file.bin",
"http://www.website.org/branch2/branch2/branch1/index.html",
"http://www.website.org/branch2/branch2/001file.bin",
"http://www.website.org/branch2/branch2/002file.bin",
"http://www.website.org/branch2/branch2/branch2/001file.bin",
"http://www.website.org/branch2/branch2/branch2/002file.bin",
"http://www.website.org/branch2/branch2/branch2/index.html",
"http://www.website.org/branch2/branch2/index.html",
"http://www.website.org/branch2/index.html",
"http://www.library.org/branch1/branch1/branch1/001file.bin",
"http://www.library.org/branch1/branch1/branch1/002file.bin",
"http://www.library.org/branch1/branch1/branch1/index.html",
"http://www.library.org/branch1/branch1/001file.bin",
"http://www.library.org/branch1/branch1/002file.bin",
"http://www.library.org/branch1/branch1/branch2/001file.bin",
"http://www.library.org/branch1/branch1/branch2/002file.bin",
"http://www.library.org/branch1/branch1/branch2/index.html",
"http://www.library.org/branch1/branch1/index.html",
"http://www.library.org/branch1/001file.bin",
"http://www.library.org/branch1/002file.bin",
"http://www.library.org/branch1/branch2/branch1/001file.bin",
"http://www.library.org/branch1/branch2/branch1/002file.bin",
"http://www.library.org/branch1/branch2/branch1/index.html",
"http://www.library.org/branch1/branch2/001file.bin",
"http://www.library.org/branch1/branch2/002file.bin",
"http://www.library.org/branch1/branch2/branch2/001file.bin",
"http://www.library.org/branch1/branch2/branch2/002file.bin",
"http://www.library.org/branch1/branch2/branch2/index.html",
"http://www.library.org/branch1/branch2/index.html",
"http://www.library.org/branch1/index.html",
"http://www.library.org/001file.bin",
"http://www.library.org/002file.bin",
"http://www.library.org/branch2/branch1/branch1/001file.bin",
"http://www.library.org/branch2/branch1/branch1/002file.bin",
"http://www.library.org/branch2/branch1/branch1/index.html",
"http://www.library.org/branch2/branch1/001file.bin",
"http://www.library.org/branch2/branch1/002file.bin",
"http://www.library.org/branch2/branch1/branch2/001file.bin",
"http://www.library.org/branch2/branch1/branch2/002file.bin",
"http://www.library.org/branch2/branch1/branch2/index.html",
"http://www.library.org/branch2/branch1/index.html",
"http://www.library.org/branch2/001file.bin",
"http://www.library.org/branch2/002file.bin",
"http://www.library.org/branch2/branch2/branch1/001file.bin",
"http://www.library.org/branch2/branch2/branch1/002file.bin",
"http://www.library.org/branch2/branch2/branch1/index.html",
"http://www.library.org/branch2/branch2/001file.bin",
"http://www.library.org/branch2/branch2/002file.bin",
"http://www.library.org/branch2/branch2/branch2/001file.bin",
"http://www.library.org/branch2/branch2/branch2/002file.bin",
"http://www.library.org/branch2/branch2/branch2/index.html",
"http://www.library.org/branch2/branch2/index.html",
"http://www.library.org/branch2/index.html",
  };

  static String[] url2 = {
    "http://www.example.com/index.html",
    "http://www.example.com/content.warc.gz",
  };

  public static void main(String[] args) throws Exception {
    // XXX should be much simpler.
    FuncWarcExploder2 test = new FuncWarcExploder2();
    if (args.length>0) {
      try {
        maxDepth = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) { }
    }

    log.info("Setting up for depth " + maxDepth);
    test.setUp(maxDepth);
    log.info("Running up for depth " + maxDepth);
    test.testRunSelf1();
    test.tearDown();
  }

  public void setUp() throws Exception {
    super.setUp();
    useOldRepo();
    this.setUp(DEFAULT_MAX_DEPTH);
  }

  public void setUp(int max) throws Exception {

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH, ""+max);
    maxDepth=max;
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);

    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doWarcFile", "true");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.actualWarcFile", "true");

    props.setProperty(FollowLinkCrawler.PARAM_STORE_ARCHIVES, "true");
    String explodedPluginName =
      "org.lockss.crawler.FuncArcExploder2MockExplodedPlugin";
    props.setProperty(Exploder.PARAM_EXPLODED_PLUGIN_NAME, explodedPluginName);
    props.setProperty(Exploder.PARAM_EXPLODED_AU_YEAR, GOOD_YEAR);
    props.setProperty(Exploder.PARAM_EXPLODED_AU_COLLECTION, COLLECTION);
    ConfigurationUtil.addFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();

    theDaemon.setUpAuConfig();

    pluginMgr = theDaemon.getPluginManager();
    crawlMgr = new NoPauseCrawlManagerImpl();
    theDaemon.setCrawlManager(crawlMgr);
    crawlMgr.initService(theDaemon);
    theDaemon.getRepositoryManager().startService();
    theDaemon.suppressStartAuManagers(false);

    // pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    pluginMgr.startLoadablePlugins();
    String explodedPluginKey = pluginMgr.pluginKeyFromName(explodedPluginName);
    pluginMgr.ensurePluginLoaded(explodedPluginKey);

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    sau.setUrlConsumerFactory(new ExplodingUrlConsumerFactory());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "3");
    conf.put("branch", "1");
    conf.put("numFiles", "2");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_BIN);
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  public void testRunSelf1() throws Exception {
    runTest(false);
  }

  public void testRunSelf2() throws Exception {
    runTest(true);
  }

  public void runTest(boolean good) throws Exception {
    log.debug3("About to create content");
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();

    log.debug3("About to crawl content");
    boolean res = crawlContent(good ? null : url[url.length - 1]);
    if (good) {
      assertTrue("Crawl failed", res);
      if (false) assertTrue("Crawl should succeed but got " + lastCrawlResult +
          (lastCrawlMessage == null ? "" : " with " + lastCrawlMessage),
          lastCrawlResult == Crawler.STATUS_SUCCESSFUL);
    } else {
      assertFalse("Crawl succeeded", res);
      if (false) assertTrue("Crawl should get STATUS_PLUGIN_ERROR but got " +
          lastCrawlResult +
          (lastCrawlMessage == null ? "" : " with " + lastCrawlMessage),
          lastCrawlResult == Crawler.STATUS_PLUGIN_ERROR);
      return;
    }

    // read all the files links from the root of the simcontent
    // check the link level of the file and see if it contains
    // in myCUS (check if the crawler crawl within the max. depth)
    CachedUrlSet myCUS = sau.getAuCachedUrlSet();
    File dir = new File(simDir);
    if(dir.isDirectory()) {
      checkExplodedUrls();
      checkUnExplodedUrls();

      log.debug("Check finished.");
    } else {
      log.error("Error: The root path of the simulated" +
          " content ["+ dir +"] is not a directory");
    }

    // Test PluginManager.getAuContentSize(), just because this is a
    // convenient place to do it.  If the simulated AU params are changed, or
    // SimulatedContentGenerator is changed, this number may have to
    // change.  NB - because the WARC files are compressed,  their
    // size varies randomly by a small amount.
    long expected = 35775;
    long actual = AuUtil.getAuContentSize(sau, true);
    long error = expected - actual;
    long absError = (error < 0 ? -error : error);
    assertTrue("size mismatch " + expected + " vs. " + actual, absError < 60);

    List sbc = ((MySimulatedArchivalUnit)sau).sbc;
    Bag b = new HashBag(sbc);
    Set uniq = new HashSet(b.uniqueSet());
    for (Iterator iter = uniq.iterator(); iter.hasNext(); ) {
      b.remove(iter.next(), 1);
    }
    // Permission pages get checked twice.  Hard to avoid that, so allow it
    b.removeAll(sau.getPermissionUrls());
    // archives get checked twice - from checkThruFileTree & checkExplodedUrls
    b.remove(url2[url2.length - 1]);
    // This test is screwed up by the use of shouldBeCached() in
    // WarcExploder() to find the AU to store the URL in.
    //assertEmpty("shouldBeCached() called multiple times on same URLs.", b);

  }

  //recursive caller to check through the whole file tree
  private void checkThruFileTree(File f[], CachedUrlSet myCUS){
    for (int ix=0; ix<f.length; ix++) {
      log.debug3("Check: " + f[ix].getAbsolutePath());
      if (f[ix].isDirectory()) {
        // get all the files and links there and iterate
        checkThruFileTree(f[ix].listFiles(), myCUS);
      } else {

        // get the f[ix] 's level information
        String fileUrl = sau.mapContentFileNameToUrl(f[ix].getAbsolutePath());
        int fileLevel = sau.getLinkDepth(fileUrl);
        log.debug2("File: " + fileUrl + " in Level " + fileLevel);

        CachedUrl cu = theDaemon.getPluginManager().findCachedUrl(fileUrl);
        if (fileLevel <= maxDepth) {
          assertTrue(cu + " has no content", cu.hasContent());
        } else {
          assertFalse(cu + " has content when it shouldn't",
              cu.hasContent());
        }
      }
    }
    return; // when all "File" in the array are checked
  }


  private void checkExplodedUrls() {
    log.debug2("Checking Exploded URLs.");
    for (int i = 0; i < url.length; i++) {
      CachedUrl cu = theDaemon.getPluginManager().findCachedUrl(url[i]);
      assertTrue(url[i] + " not in any AU", cu != null);
      log.debug2("Check: " + url[i] + " cu " + cu + " au " + cu.getArchivalUnit().getAuId());
      assertTrue(cu + " has no content", cu.hasContent());
      assertTrue(cu + " isn't ExplodedArchivalUnit",
          (cu.getArchivalUnit() instanceof ExplodedArchivalUnit));
      assertNotEquals(sau, cu.getArchivalUnit());
    }
    log.debug2("Checking Exploded URLs done.");
  }

  private void checkUnExplodedUrls() {
    log.debug2("Checking UnExploded URLs.");
    for (int i = 0; i < url2.length; i++) {
      CachedUrl cu = theDaemon.getPluginManager().findCachedUrl(url2[i]);
      assertTrue(url2[i] + " not in any AU", cu != null);
      log.debug2("Check: " + url2[i] + " cu " + cu + " au " + cu.getArchivalUnit().getAuId());
      assertTrue(cu + " has no content", cu.hasContent());
      assertTrue(cu + " isn't MySimulatedArchivalUnit",
          (cu.getArchivalUnit() instanceof MySimulatedArchivalUnit));
      assertEquals(sau, cu.getArchivalUnit());
    }
    log.debug2("Checking UnExploded URLs done.");
  }


  private void createContent() {
    log.debug("Generating tree of size 3x1x2 with "+fileSize
        +"byte files...");
    sau.generateContentTree();
  }

  private boolean crawlContent(String bad) {
    log.debug("Crawling tree..." + (bad == null ? "" : " fail at " + bad));
    sau.setRule(new MyCrawlRule());
    sau.setExploderPattern(".warc.gz$");
    sau.setExploderHelper(new MyExploderHelper(bad));
    AuState maus = new MyMockAuState(sau);
    FollowLinkCrawler crawler = new FollowLinkCrawler(sau, maus);
    crawler.setCrawlManager(crawlMgr);
    boolean res = crawler.doCrawl();
    lastCrawlResult = maus.getLastCrawlResult();
    lastCrawlMessage = maus.getLastCrawlResultMsg();
    log.debug2("End crawl " + res + " " + lastCrawlResult + " " +
        (lastCrawlMessage != null ? lastCrawlMessage : "null"));
    return res;
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
        ArchivalUnit au = new MySimulatedArchivalUnit(this);
        au.setConfiguration(auConfig);
        return au;
      }
  }

  public static class MySimulatedArchivalUnit extends SimulatedArchivalUnit {
    List sbc = new ArrayList();

    public MySimulatedArchivalUnit(Plugin owner) {
      super(owner);
    }

    protected CrawlRule makeRules() {
      return new MyCrawlRule();
    }

    public boolean shouldBeCached(String url) {
      boolean ret = false;
      if (url.startsWith("http://www.example.com/warc")) {
        url2[url2.length-1] = url;
      }
      for (int i = 0; i < url2.length; i++) {
        if (url2[i].equals(url)) {
          sbc.add(url);
          ret = super.shouldBeCached(url);
        }
      }
      if (false) {
        // This can be helpful to track down problems - h/t TAL.
        log.debug3("shouldBeCached: " + ret + " - " + url, new Throwable());
      } else {
        log.debug3("shouldBeCached: " + ret + " - " + url);
      }
      return (ret);
    }
  }

  public static class MyCrawlRule implements CrawlRule {
    public int match(String url) {
      if (url.startsWith("http://www.example.com")) {
        return CrawlRule.INCLUDE;
      }
      return CrawlRule.EXCLUDE;
    }
  }

  public static class MyMockAuState extends MockAuState {

    public MyMockAuState(ArchivalUnit au) {
      super(au);
    }

    public void newCrawlFinished(int result, String msg) {
      log.debug("Crawl finished " + result + " " + msg);
    }
  }

  public static class MyExploderHelper implements ExploderHelper {
    private static String badName;
    public MyExploderHelper(String bad) {
      badName = bad;
    }

    private static final String suffix[] = {
      ".txt",
      ".html",
      ".pdf",
      ".jpg",
      ".bin",
    };
    public static final String[] mimeType = {
      "text/plain",
      "text/html",
      "application/pdf",
      "image/jpg",
      "application/octet-stream",
    };

    public void process(ArchiveEntry ae) {
      String baseUrl = null;
      String restOfUrl = ae.getName();
      log.debug3("process(" + restOfUrl + ") " + badName);
      if (restOfUrl == null || restOfUrl.equals(badName)) {
        log.debug("Synthetic failure at " + badName);
        return;
      }
      URL url;
      try {
        url = new URL(restOfUrl);
        if (!"http".equals(url.getProtocol())) {
          log.debug2("ignoring: " + url.toString());
        }
      } catch (MalformedURLException ex) {
        log.debug2("Bad URL: " + (restOfUrl == null ? "null" : restOfUrl));
        return;
      }
      // XXX For now, put the content in an AU per host
      baseUrl = "http://" + url.getHost() + "/";
      restOfUrl = url.getFile();
      log.debug(ae.getName() + " mapped to " +
          baseUrl + " plus " + restOfUrl);
      ae.setBaseUrl(baseUrl);
      ae.setRestOfUrl(restOfUrl);
      // XXX may be necessary to synthesize some header fields
      CIProperties props = new CIProperties();
      props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
      ae.setAuProps(props);
    }

    @Override
    public void setWatchdog(LockssWatchdog wdog) {
      //do nothing
      
    }

    @Override
    public void pokeWDog() {
      //do nothing
      
    }
  }

}
