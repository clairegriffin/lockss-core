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

package org.lockss.daemon;
import java.util.*;
import org.lockss.db.DbException;
import org.lockss.util.rest.exception.LockssRestException;

/** Enumerates a set of titles ({@link TitleConfig}), presumably for AU
 * configuration purposes */
public interface TitleSet extends Comparable {
  public static final int SET_ADDABLE = 1;
  public static final int SET_DELABLE = 2;
  public static final int SET_REACTABLE = 4;

  /** Return the human-readable name of the set of titles.
   * @return the set's name */
  String getName();

  /** Return the identifier of the title set (unaccented name, safe to put
   * in HTML form attributes.
   * @return the identifier */
  public String getId();

  /** Return the titles in the set.
   * @return a collection of {@link TitleConfig} */
  Collection<TitleConfig> getTitles() throws DbException, LockssRestException;

  /** Return the number of titles in the set that can be
   * added/deleted/reactivated.
   * @return number of titles for which the action can be performed */
  int countTitles(int action) throws DbException, LockssRestException;

  /** return true iff set appropriate for specified action */
  boolean isSetActionable(int action);
}
