/*
 * $Id$
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.util.zip.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.base.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;

/**
 * A convenience class which takes care of handling the content
 * tree itself for the case where the content is in a ZIP file.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class SimulatedZipContentGenerator extends SimulatedContentGenerator {
  private static Logger logger = Logger.getLogger();
  int maxSize = 100000000;
  String[] suffix = {
    ".txt",
    ".html",
    ".pdf",
    ".jpg",
    ".bin",
  };
  String[] mimeType = {
    "text/plain",
    "text/html",
    "application/pdf",
    "image/jpg",
    "application/octet-stream",
  };
  String stem = "http://www.content.org/";

  public SimulatedZipContentGenerator(String rootPath) {
    super(rootPath);
    logger.debug3("Created instance for " + rootPath);
  }

  public String generateContentTree() {
    String ret = super.generateContentTree();
    ZipOutputStream zos = makeZipStream();
    //  There should now be a suitable hierarchy at contentRoot,
    //  except that there needs to be a link to the eventual ZIP file.
    try {
      logger.debug2("About to pack content in ZIP at " + contentRoot);
      packIntoZipFile(new File(contentRoot), "", zos, 0);
      zos.close();
      logger.debug2("Packed content in ZIP at " + contentRoot );
      linkToZipFiles();
    } catch (IOException ex) {
      logger.error("pack() threw " + ex);
      return null;
    }
    printZipFiles();
    return ret;
  }

  private byte[] buf = new byte[1024];

  private void packIntoZipFile(File f, String stem, ZipOutputStream zos,
			       int lev) throws IOException {
    String fPath = f.getCanonicalPath();
    logger.debug3("packIntoZipFile(" + fPath + ") lev " + lev);
    if (f.isDirectory()) {
      // Iterate through the directory
      File[] names = f.listFiles();
      for (int i = 0; i < names.length; i++) {
	String newPath = fPath + File.separator + names[i].getName();
	String newUrl = stem;
	if (!newUrl.equals("")) {
	  newUrl += File.separator;
	}
	newUrl += names[i].getName();
	File newFile = new File(newPath);
	packIntoZipFile(newFile, newUrl, zos, lev + 1);
      }
      {
	String[] namesLeft = f.list();
	if (namesLeft.length == 0) {
	  logger.debug3(fPath + " empty");
	} else for (int j = 0; j < namesLeft.length; j++) {
	  logger.debug3(fPath + " contains " + namesLeft[j]);
	}
      }
      if (lev > 1) {
	logger.debug3("rmdir(" + fPath + ")");
	f.delete();
      }
    } else if (f.isFile()) {
      logger.debug3("File " + fPath + " lev " + lev);
      String extension = ".zip";
      if (!fPath.endsWith(extension) &&
	  !(fPath.endsWith("index.html") && lev <= 1)) {
	String uri = stem;
	String contentType = mimeType[mimeType.length-1];
	String hostIP = "127.0.0.1";
	long timeStamp = System.currentTimeMillis();
	int recordLength = (int) f.length();
	InputStream is = new FileInputStream(f);

	for (int i = 0; i < suffix.length; i++) {
	  String name = f.getName();
	  if (name.endsWith(suffix[i])) {
	    contentType = mimeType[i];
	    break;
	  }
	}

	logger.debug3("Packing " + fPath + " type " + contentType +
		      " bytes " + recordLength);
	ZipEntry ze = new ZipEntry(uri);
	ze.setSize(recordLength);
	zos.putNextEntry(ze);
	FileInputStream fis = new FileInputStream(f);
	int bytesRead = 0;
	long bytesWritten = 0;
	while ((bytesRead = fis.read(buf)) != -1) {
	  zos.write(buf, 0, bytesRead);
	  bytesWritten += bytesRead;
	}
	zos.closeEntry();
	logger.debug3("Packed " + fPath + " bytes " + bytesWritten +
		      " of " + recordLength);
	logger.debug3("Deleting " + fPath);
	f.delete();
      } else {
	logger.debug3("Ignoring " + fPath);
      }
    } else {
      String msg = fPath + " is neither file not dir";
      logger.error(msg);
      throw new IOException(msg);
    }
  }

  private ZipOutputStream makeZipStream() {
    FileOutputStream fos = null;
    ZipOutputStream zos = null;
    try {
      fos = new FileOutputStream(new File(contentRoot, "content.zip"));
      zos = new ZipOutputStream(fos);
      zos.setMethod(ZipOutputStream.DEFLATED);
      zos.setLevel(Deflater.BEST_COMPRESSION);
    } catch (FileNotFoundException ex) {
      logger.error(ex.toString());
    }
    return zos;
  }

  private void linkToZipFiles() {
    File dir = new File(contentRoot);
    if (dir.isDirectory()) {
      File index = new File(dir, INDEX_NAME);
      if (index.exists() && index.isFile()) try {
	FileOutputStream fos = new FileOutputStream(index);
	PrintWriter pw = new PrintWriter(fos);
	logger.debug3("Re-creating index file at " + index.getAbsolutePath());
	String file_content =
	  getIndexContent(dir, INDEX_NAME, LockssPermission.LOCKSS_PERMISSION_STRING);
	pw.print(file_content);
	pw.flush();
	pw.close();
	fos.close();
      } catch (IOException ex) {
	logger.error("linkToZipFiles() threw " + ex);
      } else {
	logger.error("index.html missing");
      }
    } else {
      logger.error("Directory " + contentRoot + " missing");
    }
  }

  private void printZipFiles() {
    File dir = new File(contentRoot);
    if (dir.isDirectory()) try {
      String[] fileNames = dir.list();

      for (int i = 0; i < fileNames.length; i++) {
	if (fileNames[i].endsWith(".zip")) {
	  logger.debug3("Archive: " + fileNames[i]);

	  File f = new File(dir, fileNames[i]);
	  logger.debug3("Opening " + f.getAbsolutePath() + " bytes " +
			f.length());
	  ZipFile zf = new ZipFile(f);
	  for (Enumeration en = zf.entries(); en.hasMoreElements(); ) {
	    ZipEntry ze = (ZipEntry)en.nextElement();
	    logger.debug3("Found: " + ze.getName() + " bytes " + ze.getSize());
	  }
	}
      }
    } catch (IOException ex) {
      logger.error("printZipFiles() threw " + ex);
    } else {
      logger.error("Directory " + contentRoot + " missing");
    }
  }
}
