/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

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
package org.lockss.util;

import java.util.*;
import org.json.JSONObject;

/**
 * JSON-related utility methods.
 */
public class JsonUtil {
  /**
   * Provides an error message formatted in JSON.
   * 
   * @param code
   *          An int with the code of the error to be formatted.
   * @param message
   *          A String with the message of the error to be formatted.
   * @return a String with the error message formatted in JSON.
   */
  public static String toJsonError(int code, String message) {
    JSONObject errorElement = new JSONObject();
    errorElement.put("code", code);

    if (message == null) {
      message = "";
    }

    errorElement.put("message", message);

    JSONObject responseBody = new JSONObject();
    responseBody.put("error", errorElement);
    return responseBody.toString();
  }

  /** Turn a BitSet into a long to facilitate inclusion in message */
  public static long asLong(BitSet val) {
    long res = 0L;
    for (int ix = 0; ix < val.length(); ix++) {
      res |= val.get(ix) ? (1L << ix) : 0L;
    }
    return res;
  }

  /** Turn a long into a BitSet */
  public static BitSet asBitSet(long value) {
    BitSet res = new BitSet();
    int ix = 0;
    while (value != 0L) {
      if (value % 2L != 0) {
        res.set(ix);
      }
      ix++;
      value = value >>> 1;
    }
    return res;
  }
}
