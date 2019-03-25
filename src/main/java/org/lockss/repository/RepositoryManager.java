/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.collections.map.LinkedMap;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.os.PlatformUtil;
import org.lockss.util.time.Deadline;
import org.lockss.util.time.TimeBase;
import org.lockss.util.time.TimeUtil;
import org.lockss.plugin.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.laaws.rs.core.*;

/**
 * RepositoryManager is the center of the per AU repositories.  It manages
 * the repository config parameters.
 */
public class RepositoryManager
    extends BaseLockssDaemonManager implements ConfigurableManager {

  private static Logger log = Logger.getLogger();

  public static final String PREFIX = Configuration.PREFIX + "repository.";

  /** Maximum length of a filesystem path component. */
  public static final String PARAM_MAX_COMPONENT_LENGTH =
      PREFIX + "maxComponentLength";
  public static final int DEFAULT_MAX_COMPONENT_LENGTH = 255;


  /** @see #PARAM_CHECK_UNNORMALIZED */
  public enum CheckUnnormalizedMode {No, Log, Fix};

  /** Check for existing nodes with unnormalized names (created by very old
   * daemon that didn't normalize): None, Log, Fix */
  public static final String PARAM_CHECK_UNNORMALIZED =
      PREFIX + "checkUnnormalized";
  public static final CheckUnnormalizedMode DEFAULT_CHECK_UNNORMALIZED =
      CheckUnnormalizedMode.Log;

  /** Temporary specification of (new) LockssRepository for all AU storage.
   * <ul><li>volatile:<i>collection</i> - use a volatile LockssRepository</li>
   * <li>local:<i>collection</i>:<i>path</i> - use a local LockssRepository
   * at <i>path</i></li>
   * <li>rest:<i>collection</i>:<i>url</i> - use a remote LockssRepository
   * at <i>url</i></li></ul>
   */
  public static final String PARAM_V2_REPOSITORY =
      PREFIX + "v2Repository";
  public static final String DEFAULT_V2_REPOSITORY = "volatile:baz";
//   public static final String DEFAULT_V2_REPOSITORY = null;

  public static final String PARAM_PERSIST_INDEX_NAME =
      PREFIX + "persistIndexName";
  public static final String DEFAULT_PERSIST_INDEX_NAME = "artifact-index.ser";

  static final String DISK_PREFIX = PREFIX + "diskSpace.";


  static final String PARAM_DISK_WARN_FRRE_MB = DISK_PREFIX + "warn.freeMB";
  static final int DEFAULT_DISK_WARN_FRRE_MB = 5000;
  static final String PARAM_DISK_FULL_FRRE_MB = DISK_PREFIX + "full.freeMB";
  static final int DEFAULT_DISK_FULL_FRRE_MB = 100;
  static final String PARAM_DISK_WARN_FRRE_PERCENT =
      DISK_PREFIX + "warn.freePercent";
  static final double DEFAULT_DISK_WARN_FRRE_PERCENT = .02;
  static final String PARAM_DISK_FULL_FRRE_PERCENT =
      DISK_PREFIX + "full.freePercent";
  static final double DEFAULT_DISK_FULL_FRRE_PERCENT = .01;

  private PlatformUtil platInfo = PlatformUtil.getInstance();
  private List repoList = Collections.EMPTY_LIST;
  Map localRepos = new HashMap();
  private static int maxComponentLength = DEFAULT_MAX_COMPONENT_LENGTH;
  private static CheckUnnormalizedMode checkUnnormalized =
      DEFAULT_CHECK_UNNORMALIZED;

  private RepositoryAndCollection v2Repo = null;

  PlatformUtil.DF paramDFWarn =
      PlatformUtil.DF.makeThreshold(DEFAULT_DISK_WARN_FRRE_MB,
          DEFAULT_DISK_WARN_FRRE_PERCENT);
  PlatformUtil.DF paramDFFull =
      PlatformUtil.DF.makeThreshold(DEFAULT_DISK_FULL_FRRE_MB,
          DEFAULT_DISK_FULL_FRRE_PERCENT);

  public void startService() {
    super.startService();
    localRepos = new HashMap();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
      Configuration.Differences changedKeys) {
    //  Build list of repositories from list of disk (fs) paths).  Needs to
    //  be generalized if ever another repository implementation.
    if (changedKeys.contains(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST)) {
      List lst = new ArrayList();
      String dspace =
          config.get(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, "");
      List paths = StringUtil.breakAt(dspace, ';');
      if (paths != null) {
        for (Iterator iter = paths.iterator(); iter.hasNext(); ) {
          lst.add("local:" + (String)iter.next());
        }
      }
      repoList = lst;
    }
    if (changedKeys.contains(DISK_PREFIX)) {
      int minMB = config.getInt(PARAM_DISK_WARN_FRRE_MB,
          DEFAULT_DISK_WARN_FRRE_MB);
      double minPer = config.getPercentage(PARAM_DISK_WARN_FRRE_PERCENT,
          DEFAULT_DISK_WARN_FRRE_PERCENT);
      paramDFWarn = PlatformUtil.DF.makeThreshold(minMB, minPer);
      minMB = config.getInt(PARAM_DISK_FULL_FRRE_MB,
          DEFAULT_DISK_FULL_FRRE_MB);
      minPer = config.getPercentage(PARAM_DISK_FULL_FRRE_PERCENT,
          DEFAULT_DISK_FULL_FRRE_PERCENT);
      paramDFFull = PlatformUtil.DF.makeThreshold(minMB, minPer);
    }
    if (changedKeys.contains(PREFIX)) {
      maxComponentLength = config.getInt(PARAM_MAX_COMPONENT_LENGTH,
          DEFAULT_MAX_COMPONENT_LENGTH);
      checkUnnormalized =
          (CheckUnnormalizedMode)
              config.getEnum(CheckUnnormalizedMode.class,
                  PARAM_CHECK_UNNORMALIZED, DEFAULT_CHECK_UNNORMALIZED);
    }
    processV2RepoSpec(config.get(PARAM_V2_REPOSITORY,
				   DEFAULT_V2_REPOSITORY),
	config.get(PARAM_PERSIST_INDEX_NAME, DEFAULT_PERSIST_INDEX_NAME));
  }

  static Pattern REPO_SPEC_PATTERN =
    Pattern.compile("([^:]+):([^:]+)(?::(.*$))?");

  private void processV2RepoSpec(String spec, String persistedIndexName) {
    if (!StringUtil.isNullString(System.getProperty("oldrepo"))) {
      return;
    }
    if (spec != null) {
      // currently set this only once
      if (v2Repo == null) {
	LockssRepository repo = null;
	Matcher m1 = REPO_SPEC_PATTERN.matcher(spec);
	if (m1.matches()) {
	  String coll = m1.group(2);
	  if (StringUtil.isNullString(coll)) {
	    log.critical("Illegal V2 repository spec: " + spec);
	  } else {
	    String repoSpec = m1.group(1);
	    switch (repoSpec) {
	    case "volatile":
	      try {
		repo = LockssRepositoryFactory.createVolatileRepository();
	      } catch (IOException e) {
	        // This should never happen - never actually thrown by volatile implementation
	        log.critical("Caught IOException when attempting to create a volatile repository!", e);
	      }
	      break;
	    case "local":
	      String s = m1.group(3);
	      if (StringUtil.isNullString(s)) {
		log.critical("Illegal V2 repository spec: " + spec);
	      } else {
		File path = new File(s);
		try {
		  repo = LockssRepositoryFactory.createLocalRepository(path,
		      persistedIndexName);
		} catch (IOException e) {
		  log.critical("Illegal V2 repository path: " + path +
			       ", persistedIndexName: " + persistedIndexName +
			       ": " + e.getMessage());
		}
	      }
	      break;
	    case "rest":
	      String u = m1.group(3);
	      if (StringUtil.isNullString(u)) {
		log.critical("Illegal V2 repository spec: " + spec);
	      } else {
		try {
		  URL url = new URL(u);
		  repo =
		    LockssRepositoryFactory.createRestLockssRepository(url);
		} catch (MalformedURLException e) {
		  log.critical("Illegal V2 repository spec URL: " + spec +
			       ": " + e.getMessage());
		}
	      }
	      break;
	    default:
	      log.critical("Illegal V2 repository spec: " + spec);
	    }
	    if (repo != null) {
	      v2Repo = new RepositoryAndCollection(repoSpec, repo, coll);
	    }
	  }
	} else {
	  log.critical("Illegal V2 repository spec: " + spec);
	}
      }
    } else {
      v2Repo = null;
    }
  }

  public class RepositoryAndCollection {
    private String spec;
    private LockssRepository repo;
    private String collection;

    private RepositoryAndCollection(String spec,
				    LockssRepository repo,
				    String collection) {
      this.spec = spec;
      this.repo = repo;
      this.collection = collection;
    }

    public String getSpec() {
      return spec;
    }

    public LockssRepository getRepository() {
      return repo;
    }

    public String getCollection() {
      return collection;
    }
  }

  public static boolean isV2Repo() {
    RepositoryAndCollection rac =
      LockssDaemon.getLockssDaemon().getRepositoryManager().getV2Repository();
    return rac != null && rac.getRepository() != null;
  }

  public RepositoryAndCollection getV2Repository() {
    return v2Repo;
  }

  public static int getMaxComponentLength() {
    return maxComponentLength;
  }

  public static CheckUnnormalizedMode getCheckUnnormalizedMode() {
    return checkUnnormalized;
  }

  /** Return list of known repository names.  Needs a registration
   * mechanism if ever another repository implementation. */
  public List<String> getRepositoryList() {
    return repoList;
  }

  public PlatformUtil.DF getRepositoryDF(String repoName) {
    // XXXREPO
    return platInfo.getJavaDF(".");
  }

  public Map<String,PlatformUtil.DF> getRepositoryMap() {
    Map<String,PlatformUtil.DF> repoMap = new LinkedMap();
    for (String repo : getRepositoryList()) {
      repoMap.put(repo, getRepositoryDF(repo));
    }
    return repoMap;
  }

  public String findLeastFullRepository() {
    return findLeastFullRepository(getRepositoryMap());
  }

  public String findLeastFullRepository(Map<String,PlatformUtil.DF> repoMap) {
    String mostFree = null;
    for (String repo : repoMap.keySet()) {
      PlatformUtil.DF df = repoMap.get(repo);
      if (df != null) {
        if (mostFree == null ||
            (repoMap.get(mostFree)).getAvail() < df.getAvail()) {
          mostFree = repo;
        }
      }
    }
    return mostFree;
  }

  public PlatformUtil.DF getDiskWarnThreshold() {
    return paramDFWarn;
  }

  public PlatformUtil.DF getDiskFullThreshold() {
    return paramDFFull;
  }

  public List<String> findExistingRepositoriesFor(String auid) {
    LockssRepository repo = v2Repo.getRepository();
    try {
      for (String id : repo.getAuIds(v2Repo.getCollection())) {
	if (auid.equals(id)) {
	  return Collections.singletonList(v2Repo.getSpec());
	}
      }
    } catch (IOException e) {
      log.warning("Error getting list of AUID in repository collection");
      return Collections.emptyList();
    }
    return Collections.emptyList();
  }

  /**
   * Return the disk space used by the AU, including all overhead,
   * optionally calculating it if necessary.
   * @param repoAuPath the full path to an AU dir in a LockssRepositoryImpl
   * @param calcIfUnknown if true, size will calculated if unknown (time
   * consumeing)
   * @return the AU's disk usage in bytes, or -1 if unknown
   */
  // XXXREPO
  public long getRepoDiskUsage(String repoAuPath, boolean calcIfUnknown) {
    return -1;
  }

}
