/*

 Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.rs.exception;

public class LockssRestHttpException extends LockssRestException {
  private static final long serialVersionUID = -4151454747192096531L;

  private int httpStatusCode;
  private String httpStatusMessage;

  /**
   * Default constructor.
   */
  public LockssRestHttpException() {
    super();
  }

  /**
   * Constructor with a specified message.
   * 
   * @param message
   *          A String with the exception message.
   */
  public LockssRestHttpException(String message) {
    super(message);
  }

  /**
   * Provides the HTTP status code.
   * 
   * @return an int with the HTTP status code.
   */
  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  /**
   * Saves the HTTP status code.
   * 
   * @param httpStatusCode
   *          An int with the HTTP status code.
   */
  public void setHttpStatusCode(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

  /**
   * Provides the HTTP status message.
   * 
   * @return a String with the HTTP status message.
   */
  public String getHttpStatusMessage() {
    return httpStatusMessage;
  }

  /**
   * Saves the HTTP status message.
   * 
   * @param httpStatusMessage
   *          A String with the HTTP status message.
   */
  public void setHttpStatusMessage(String httpStatusMessage) {
    this.httpStatusMessage = httpStatusMessage;
  }
}
