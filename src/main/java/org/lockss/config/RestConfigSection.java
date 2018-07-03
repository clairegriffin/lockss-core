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
import java.util.List;
import org.lockss.rs.multipart.MultipartResponse;
import org.springframework.http.HttpStatus;

/**
 * A representation of a Configuration REST web service configuration section.
 */
public class RestConfigSection {
  private String sectionName = null;
  private List<String> ifMatch = null;
  private List<String> ifNoneMatch = null;
  private InputStream inputStream = null;
  private String etag = null;
  private MultipartResponse response = null;
  private HttpStatus statusCode = null;
  private String errorMessage = null;
  private String contentType = null;
  private long contentLength = 0;

  /**
   * Constructor.
   */
  public RestConfigSection() {
  }

  public String getSectionName() {
    return sectionName;
  }

  public RestConfigSection setSectionName(String sectionName) {
    this.sectionName = sectionName;
    return this;
  }

  public List<String> getIfMatch() {
    return ifMatch;
  }

  public RestConfigSection setIfMatch(List<String> ifMatch) {
    this.ifMatch = ifMatch;
    return this;
  }

  public List<String> getIfNoneMatch() {
    return ifNoneMatch;
  }

  public RestConfigSection setIfNoneMatch(List<String> ifNoneMatch) {
    this.ifNoneMatch = ifNoneMatch;
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

  public MultipartResponse getResponse() {
    return response;
  }

  public RestConfigSection setResponse(MultipartResponse response) {
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

  public long getContentLength() {
    return contentLength;
  }

  public RestConfigSection setContentLength(long contentLength) {
    this.contentLength = contentLength;
    return this;
  }

  @Override
  public String toString() {
    return "[RestConfigSection sectionName=" + sectionName
	+ ", ifMatch=" + ifMatch + ", ifNoneMatch=" + ifNoneMatch
	+ ", etag=" + etag + ", response=" + response
	+ ", statusCode=" + statusCode + ", errorMessage=" + errorMessage
	+ ", contentType=" + contentType + ", contentLength=" + contentLength
	+ "]";
  }
}
