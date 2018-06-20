/*

 Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.config;

import java.io.InputStream;
import org.lockss.rs.multipart.TextMultipartResponse;
import org.lockss.util.Logger;
import org.springframework.http.HttpStatus;

/**
 * A representation of a Configuration REST web service configuration section.
 */
public class RestConfigSection {
  private static Logger log = Logger.getLogger(RestConfigSection.class);

  private String sectionName = null;
  private InputStream inputStream = null;
  private String etag = null;
  private TextMultipartResponse response = null;
  private HttpStatus statusCode = null;
  private String errorMessage = null;
  private String contentType = null;

  /**
   * Constructor.
   */
  public RestConfigSection() {
  }

  /**
   * Copy constructor.
   */
  public RestConfigSection(RestConfigSection source) {
    // Validation of the source object.
    if (source == null) {
      String errorMessage = "Source RestConfigSection object is null";
      log.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }

    sectionName = source.sectionName;
    inputStream = source.inputStream;
    etag = source.etag;
    response = source.response;
    statusCode = source.statusCode;
    errorMessage = source.errorMessage;
    contentType = source.contentType;
  }

  public String getSectionName() {
    return sectionName;
  }

  public RestConfigSection setSectionName(String sectionName) {
    this.sectionName = sectionName;
    return this;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public RestConfigSection setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
    return this;
  }

  public String getEtag() {
    return etag;
  }

  public RestConfigSection setEtag(String etag) {
    this.etag = etag;
    return this;
  }

  public TextMultipartResponse getResponse() {
    return response;
  }

  public RestConfigSection setResponse(TextMultipartResponse response) {
    this.response = response;
    return this;
  }

  public HttpStatus getStatusCode() {
    return statusCode;
  }

  public RestConfigSection setStatusCode(HttpStatus statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public RestConfigSection setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public RestConfigSection setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  @Override
  public String toString() {
    return "RestConfigSection [sectionName=" + sectionName + ", etag=" + etag
	+ ", response=" + response + ", statusCode=" + statusCode
	+ ", errorMessage=" + errorMessage + ", contentType=" + contentType
	+ "]";
  }
}
