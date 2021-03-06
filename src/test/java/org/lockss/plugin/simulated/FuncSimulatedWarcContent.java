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

package org.lockss.plugin.simulated;

import java.io.*;
import java.util.*;

import org.lockss.config.*;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.util.*;

import junit.framework.*;

/**
 * Functional test of the WARC file simulated content
 * generator.  It does not test the regular simulated
 * content generator which is used to generate the
 * content that is then packed into the WARC file and
 * deleted.
 *
 * @author  David S. H. Rosenthal
 * @author  Felix Ostrowski
 * @version 0.0
 */

public class FuncSimulatedWarcContent extends LockssTestCase {
  static final Logger log = Logger.getLogger();

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;

  String warcFileName = null;

  public FuncSimulatedWarcContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doWarcFile", "true");
    ConfigurationUtil.addFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.getHashService();

    theDaemon.setDaemonInited(true);

    theDaemon.getPluginManager().startService();
    theDaemon.getHashService().startService();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }

  public void tearDown() throws Exception {
    theDaemon.getPluginManager().stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "2");
    conf.put("fileTypes",
	     "" + (SimulatedContentGenerator.FILE_TYPE_HTML +
		   SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("badCachedFileLoc", "2,2");
    conf.put("badCachedFileNum", "2");
    return conf;
  }

  public void testSimulatedWarcContent() throws Exception {
    createContent();
    crawlContent();
    checkContent();
  }

  protected void createContent() {
    log.debug("createContent()");
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setOddBranchesHaveContent(true);

    sau.deleteContentTree();
    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
    // Now find the WARC file
    String fileRoot = sau.getRootDir() + SimulatedContentGenerator.ROOT_NAME;
    log.debug("root dir " + fileRoot);
    File rootDir = new File(fileRoot);
    assertTrue(rootDir.exists());
    assertTrue(rootDir.isDirectory());
    String[] names = rootDir.list();
    for (int i = 0; i < names.length; i++) {
      log.debug3("Dir entry " + names[i]);
      if (names[i].endsWith(".warc.gz") || names[i].endsWith(".warc")) {
        warcFileName = names[i];
        break;
      }
    }
    assertNotNull(warcFileName);
    log.debug("WARC file name " + warcFileName);
  }

  protected void crawlContent() {
    log.debug("crawlContent()");
    Crawler crawler =
      new NoCrawlEndActionsFollowLinkCrawler(sau, new MockAuState());
    crawler.doCrawl();
  }

  protected void checkContent() throws IOException {
    log.debug("checkContent()");
    checkRoot();
  }

  protected void checkRoot() {
    log.debug("checkRoot()");
    CachedUrlSet set = sau.getAuCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    ArrayList childL = new ArrayList(1);
    CachedUrlSet cus = null;
    while (setIt.hasNext()) {
      cus = (CachedUrlSet) setIt.next();
      childL.add(cus.getUrl());
    }

    assertIsomorphic(sau.getUrlStems(), childL);

    setIt = cus.flatSetIterator();
    childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }

    // XXX assumpiton here that simulated content fits into a single warc.gz
    assertIsomorphic(ListUtil.list(sau.getUrlRoot() + "/" + warcFileName,
				   sau.getUrlRoot() + "/index.html"),
		     childL);
  }

  public static Test suite() {
    return new TestSuite(FuncSimulatedWarcContent.class);
  }

}
