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

package org.lockss.daemon;

import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.daemon.ProxyInfo.*;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.proxy.icp.IcpManager;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for ProxyInfo.
 */
public class TestProxyInfo extends LockssTestCase {
  static final String HOST = "host.org";

  private ProxyInfo pi;
  private Plugin plug;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    pi = new ProxyInfo(HOST);
    plug = new MockPlugin();
    getMockLockssDaemon().getPluginManager().startService();
  }

  public void testGetProxyHost() {
    String h = "1.3.4.22";
    Properties p = new Properties();
    p.put(IdentityManager.PARAM_LOCAL_IP, h);
    ConfigurationUtil.addFromProps(p);
    assertEquals(h, new ProxyInfo().getProxyHost());
    assertEquals("foo", new ProxyInfo("foo").getProxyHost());
  }

  // platform param should supersede local ip
  public void testGetProxyHostFromPlatform() {
    String h = "fq.dn.org";
    Properties p = new Properties();
    p.put(ConfigManager.PARAM_PLATFORM_FQDN, h);
    p.put(IdentityManager.PARAM_LOCAL_IP, "superseded.by.platform");
    ConfigurationUtil.addFromProps(p);
    assertEquals(h, new ProxyInfo().getProxyHost());
    assertEquals("foo", new ProxyInfo("foo").getProxyHost());
  }

  String ifsRE =
      " // .*\\n"
    + " if \\(shExpMatch\\(url, \\\"http://foo\\.bar/\\*\\\"\\)\\)\\n"
    + " { return \\\"PROXY " + HOST + ":24670; DIRECT\\\"; }\\n\\n"
    + " // .*\\n"
    + " if \\(shExpMatch\\(url, \\\"http://x\\.com/\\*\\\"\\)\\)\\n"
    + " { return \\\"PROXY " + HOST + ":24670; DIRECT\\\"; }\\n\\n";

  List urlStems = ListUtil.list("http://foo.bar", "http://x.com");

  Set makeUrlStemSet() {
    return makeUrlStemSet(urlStems, 1);
  }

  Set makeUrlStemSet(List stems, int ix) {
    for (Iterator iter = stems.iterator(); iter.hasNext(); ix++) {
      String urlStem = (String)iter.next();
      MockArchivalUnit au = new MockArchivalUnit();
      au.setName("MockAU " + ix);
      au.setUrlStems(ListUtil.list(urlStem));
      PluginTestUtil.registerArchivalUnit(plug, au);
    }
    return getMockLockssDaemon().getPluginManager().getAllStems();
  }

  class MyMockArchivalUnit extends MockArchivalUnit {
  }

  class MyRegistryArchivalUnit extends RegistryArchivalUnit {
    private Collection urlStems;
    public MyRegistryArchivalUnit(RegistryPlugin plugin) {
      super(plugin);
    }
    public Collection getUrlStems() {
      return urlStems;
    }
    void setUrlStems(Collection urlStems) {
      this.urlStems = urlStems;
    }
  }

  public void testGeneratePacFile() throws Exception {
    final String headRE =
        "// PAC file\\n"
      + "// Generated .* by LOCKSS cache .*\\n\\n"
      + "// Generated from .*\n"
      + "function FindProxyForURL\\(url, host\\) {\\n";
    final String tailRE =
        " return \\\"DIRECT\\\";\\n"
      + "}\\n";
    String pf = pi.generatePacFile(makeUrlStemSet(), "http://www.foobar.com/test", false);
    assertMatchesRE("PAC file didn't match RE.  File contents:\n" + pf,
		    headRE + ifsRE + tailRE, pf);
  }

  
  public void testGenerateEncapsulatedPacFile() throws Exception {
    
    // Test one: the test created by Thib.
    
    final String oldfile = "# foo\n" +
      "function FindProxyForURL(url, host) {\n" +
      "return some_logic(url, host);\n}\n";
    final String encapsulated = "# foo\n" +
      "function FindProxyForURL_0(url, host) {\n" +
      "return some_logic(url, host);\n}\n";

    final String headRE =
        "// PAC file\\n"
      + "// Generated .* by LOCKSS cache .*\\n\\n"
      + "// Generated from .*\\n"
      + "function FindProxyForURL\\(url, host\\) {\\n";
    final String tailRE =
        " return FindProxyForURL_0\\(url, host\\);\\n"
      + "}\\n\\n"
      + "// Encapsulated PAC file follows \\(msg\\)\\n\\n";
    final String pat =
      headRE + ifsRE + tailRE + StringUtil.escapeNonAlphaNum(encapsulated);

    String pf = pi.generateEncapsulatedPacFile(makeUrlStemSet(), oldfile, "(msg)", "http://www.foobar.com/test", false);
    assertMatchesRE("PAC file didn't match RE.  File contents:\n" + pf,
		    pat, pf);
  }

  public void testRemoveCommentLines() {
    assertEquals("", removeCommentLines(""));
    assertEquals("foo", removeCommentLines("#bar\nfoo\n####"));
    assertEquals("foo\n", removeCommentLines("#bar\nfoo\n####\n"));
  }

  public void testGenerateEZProxyFragment() throws Exception {
    final String frag =
        "Proxy host.org:24670\n"
      + "\n"
      + "Title MockAU 1\n"
      + "URL http://foo.bar\n"
      + "Domain foo.bar\n"
      + "\n"
      + "Title MockAU 2\n"
      + "URL http://x.com\n"
      + "Domain x.com\n"
      + "\n"
      + "Proxy\n";

    String s = pi.generateEZProxyFragment(makeUrlStemSet(), "http://www.foobar.com/test.html");
    assertTrue(s.startsWith("#"));
    assertEquals(frag, removeCommentLines(s));
  }

  String removeCommentLines(String str) {
    return removeCommentLines(str, "#");
  }

  String removeCommentLines(String str, String beginComment) {
    List lines = StringUtil.breakAt(str, '\n');
    for (ListIterator iter = lines.listIterator(); iter.hasNext(); ) {
      if (((String)iter.next()).startsWith(beginComment)) {
	iter.remove();
      }
    }
    return StringUtil.separatedString(lines, "\n");
  }

  String removeEmptyLines(String str) {
    List lines = StringUtil.breakAt(str, '\n');
    for (ListIterator iter = lines.listIterator(); iter.hasNext(); ) {
      if (((String)iter.next()).length() == 0) {
        iter.remove();
      }
    }
    return StringUtil.separatedString(lines, "\n");
  }

  public void testFragmentBuilder() {
//    final String url1 = "http://bar.com";
//    final String url2 = "http://foo.com";

    FragmentBuilder builder = pi.new FragmentBuilder("http://www.foobar.com/test") {
      protected void generateEntry(StringBuffer buffer, String urlStem, String comment) {}
    };

    StringBuffer buffer;

    buffer = new StringBuffer();
    builder.generateBeginning(buffer);
    assertEquals("", buffer.toString());

    buffer = new StringBuffer();
    builder.generateEnd(buffer);
    assertEquals("", buffer.toString());

    buffer = new StringBuffer();
    builder.generateEmpty(buffer);
    assertEquals("", buffer.toString());
  }

  public void testFragmentBuilderComment() {
    final List<String> comments = new ArrayList<String>();

    FragmentBuilder builder = pi.new FragmentBuilder("http://www.foobar.com/test") {
      protected void generateEntry(StringBuffer buffer, String urlStem,
				   String comment) {
	comments.add(comment);
      }
    };

    String url = "http://host1.com/";
    makeUrlStemSet(ListUtil.list(url), 1);
    url = "http://host2.com/";
    makeUrlStemSet(ListUtil.list(url, url), 10);
    url = "http://host3.com/";
    makeUrlStemSet(ListUtil.list(url, url, url), 20);
    url = "http://host4.com/";
    builder.generateFragment(makeUrlStemSet(ListUtil.list(url, url, url,
							  url, url), 40));
    assertEquals(ListUtil.list("MockAU 1",
			       "MockAU 10, MockAU 11",
			       "MockAU 20, MockAU 21, (1 more)",
			       "MockAU 40, MockAU 41, (3 more)"),
		 comments);
  }

  public void testSquidFragmentBuilder() {
    MockLockssDaemon mockLockssDaemon = getMockLockssDaemon();
    IcpManager testableIcpManager = new IcpManager() {
      public synchronized boolean isIcpServerRunning() { return true; }
    };
    mockLockssDaemon.setIcpManager(testableIcpManager);
    testableIcpManager.initService(mockLockssDaemon);
    mockLockssDaemon.setDaemonInited(true);
    testableIcpManager.startService();
    
    SquidFragmentBuilder builder = pi.new SquidFragmentBuilder("http://www.foobar.com/test") {
      protected void generateEntry(StringBuffer buffer, String urlStem, String comment) {}
    };

    assertEquals(HOST.replaceAll("\\.", "-") + "-domains",
                 builder.encodeAclName());

    StringBuffer buffer;

    buffer = new StringBuffer();
    builder.commonHeader(buffer);
    assertEveryLineMatchesRE(buffer.toString(), "^#");

    buffer = new StringBuffer();
    builder.commonUsage(buffer, "#");
    assertEveryLineMatchesRE(buffer.toString(), "^#");

    buffer = new StringBuffer();
    builder.commonUsage(buffer, "\u263a");
    assertEveryLineMatchesRE(removeCommentLines(buffer.toString()), "^\u263a");
  }

  public void testExternalSquidFragmentBuilder() {
    ExternalSquidFragmentBuilder builder = pi.new ExternalSquidFragmentBuilder("http://www.foobar.com/test");

    StringBuffer buffer;

    buffer = new StringBuffer();
    builder.generateEntry(buffer, "http://foo.com", "Foo");
    assertMatchesRE("# Foo\\n" + "foo.com",
		    removeEmptyLines(buffer.toString())
    );

    // Verify that we're including the URL.
    // Important note: at this time, the IcpManager is not available on my machine,
    // therefore I cannot run the following test.
    
//    buffer = new StringBuffer();
//    builder.generateBeginning(buffer);
//    assertMatchesRE("http://www.foobar.com/test",
//        removeEmptyLines(buffer.toString())
//    );

  }

  public void testSquidConfigFragmentBuilder() {
    StringBuffer buffer;
    SquidConfigFragmentBuilder builder;
    
    // Verify the entry
    buffer = new StringBuffer();

    builder = pi.new SquidConfigFragmentBuilder("http://www.foobar.com/test", false);
    builder.generateEntry(buffer, "http://foo.com", "Foo");
    assertMatchesRE(
          "# Foo\\n"
        + "acl " + builder.encodeAclName() + " dstdomain foo.com",
        removeEmptyLines(buffer.toString())
    );
    
    // Verify the fragment building.  
    Set<String> urlStems;
    
    urlStems = new HashSet<String>();
    urlStems.add("foo.com");
    
    // isDirectFirst = true.
    buffer = new StringBuffer();
    
    builder = pi.new SquidConfigFragmentBuilder("http://www.foobar.com/test", true);
    builder.generateBeginning(buffer);
    assertMatchesRE(
          "prefer_direct on",
        removeEmptyLines(buffer.toString())
    );
    assertMatchesRE("http://www.foobar.com/test",
        removeEmptyLines(buffer.toString())
    );
    
    // isDirectFirst = false
    buffer = new StringBuffer();
    
    builder = pi.new SquidConfigFragmentBuilder("http://www.foobar.com/test", false);
    builder.generateBeginning(buffer);
    assertNotMatchesRE(
          "prefer_direct on",
        removeEmptyLines(buffer.toString())
    );
    assertMatchesRE("http://www.foobar.com/test",
        removeEmptyLines(buffer.toString())
    );
   
    
  }

  public void testEZProxyFragmentBuilder() {
    EZProxyFragmentBuilder builder = pi.new EZProxyFragmentBuilder("http://www.foobar.com/test");

    StringBuffer buffer;

    buffer = new StringBuffer();
    builder.generateEntry(buffer, "http://foo.com", "Foo");
    assertMatchesRE(
          "Title Foo\\n"
        + "URL http://foo.com\\n"
        + "Domain foo.com",
        removeEmptyLines(buffer.toString())
    );
    
    // Verify that we include the source URL.
    buffer = new StringBuffer();
    builder.generateBeginning(buffer);
    assertMatchesRE("http://www.foobar.com/test",
        removeEmptyLines(buffer.toString())
    );
  }

  public void testPacFileFragmentBuilder() {
    StringBuffer buffer;
    PacFileFragmentBuilder builder;
    
    // Test one: the Direct goes last.
    buffer = new StringBuffer();
    
    builder = pi.new PacFileFragmentBuilder("http://www.foobar.com/test", false);
    builder.generateEntry(buffer, "http://foo.com", "Foo");

    assertMatchesRE(
          " // Foo\\n"
        + " if \\(shExpMatch\\(url, \\\"http://foo\\.com/\\*\\\"\\)\\)\\n"
        + " { return \\\"PROXY " + HOST + ":24670; DIRECT\\\"; }",
        removeEmptyLines(buffer.toString())
    );
    
    // Verify that we have the original URL in the output.
    buffer = new StringBuffer();
    builder.generateBeginning(buffer);
    assertMatchesRE("http://www.foobar.com/test",
            removeEmptyLines(buffer.toString())
        );
    
    // Test two: the Direct goes first.
    buffer = new StringBuffer();
    
    builder = pi.new PacFileFragmentBuilder("http://www.foobar.com/test/", true);
    builder.generateEntry(buffer, "http://foo.com", "Foo");

    assertMatchesRE(
          " // Foo\\n"
        + " if \\(shExpMatch\\(url, \\\"http://foo\\.com/\\*\\\"\\)\\)\\n"
        + " { return \\\"DIRECT; PROXY " + HOST + ":24670\\\"; }",
        removeEmptyLines(buffer.toString())
    );

    // Verify that we have the original URL in the output.
    buffer = new StringBuffer();
    builder.generateBeginning(buffer);
    assertMatchesRE("http://www.foobar.com/test",
            removeEmptyLines(buffer.toString())
        );

  }

  public void testEncapsulatedPacFileFragmentBuilder() throws Exception {
    EncapsulatedPacFileFragmentBuilder builder =
      pi.new EncapsulatedPacFileFragmentBuilder(null, null, "http://www.foobar.com/test/", false);

    final String js1 = "function func0(foo, bar) { stmt; }\n";
    final String js2 = "function func1(foo, bar) { stmt; }\n";
    final String js3 = "function func00(foo, bar) { stmt; }\n";
    assertEquals("newname0", builder.findUnusedName(js1, "newname"));
    assertEquals("func1", builder.findUnusedName(js1, "func"));
    assertEquals("func2", builder.findUnusedName(js1 + js2, "func"));
    assertEquals("func0", builder.findUnusedName(js3, "func"));
    assertEquals("func01", builder.findUnusedName(js3, "func0"));

    final String js4 =
      "function func(foo, bar) { func(bar, foo); func0(1,2); func_3(1) }\n";
    final String exp =
      "function func1(foo, bar) { func1(bar, foo); func0(1,2); func_3(1) }\n";
    assertEquals(exp, builder.jsReplace(js4, "func", "func1"));
  }

  protected static void assertEveryLineMatchesRE(String lines, String regex) {
    String[] line = lines.split("\\n");
    for (int ix = 0 ; ix < line.length ; ++ix) {
      assertMatchesRE(regex, line[ix]);
    }
  }

}
