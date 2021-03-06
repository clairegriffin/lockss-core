/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
// Some portions of this code are:
// ========================================================================
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.

package org.lockss.jetty;

import java.io.IOException;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.net.ssl.*;

import org.mortbay.http.*;
import org.mortbay.util.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;


/**
 * Extension of org.mortbay.http.SslListener that works with an externally
 * supplied, initialized, KeyManagerFactory.
 */
public class LockssSslListener extends SslListener {
  private static Logger log = Logger.getLogger();

  private KeyManagerFactory _keyManagerFactory;

  private List<String> disableProtocols;

  public LockssSslListener() {
    super();
  }

  public LockssSslListener(InetAddrPort p_address) {
    super(p_address);
  }

  public void setKeyManagerFactory(KeyManagerFactory fact) {
    _keyManagerFactory = fact;
  }

  public KeyManagerFactory getKeyManagerFactory() {
    return _keyManagerFactory;
  }

  @Override
  protected SSLServerSocketFactory createFactory() throws Exception {
    if (_keyManagerFactory == null) {
      return super.createFactory();
    }
    RandomManager rmgr = LockssDaemon.getLockssDaemon().getRandomManager();
    SecureRandom rng = rmgr.getSecureRandom();

    SSLContext context = SSLContext.getInstance(getProtocol());
    context.init(_keyManagerFactory.getKeyManagers(),
		 null, rng);
    return context.getServerSocketFactory();
  }

  @Override
  protected ServerSocket newServerSocket(InetAddrPort p_address,
					 int p_acceptQueueSize)
      throws IOException {
    ServerSocket sock = super.newServerSocket(p_address, p_acceptQueueSize);
    if (sock instanceof SSLServerSocket) {
      disableSelectedProtocols((SSLServerSocket)sock);
    }
    return sock;
  }

  public void setDisableProtocols(List<String> protos) {
    disableProtocols = protos;
  }

  private void disableSelectedProtocols(SSLServerSocket sock) {
    if (disableProtocols == null) return;
    Set<String> enaprotos = new HashSet<String>();
    for (String s : sock.getEnabledProtocols()) {
      if (disableProtocols.contains(s)) {
	continue;
      }
      enaprotos.add(s);
    }
    sock.setEnabledProtocols(enaprotos.toArray(new String[0]));
  }
}
