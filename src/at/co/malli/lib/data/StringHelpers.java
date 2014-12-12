/*
 * Created: 2014-12-08
 * 
 * ====================================================================
 *    The author of this file licenses it to you under the Apache
 *    License, Version 2.0. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 * ====================================================================
 */
package at.co.malli.lib.data;

/**
 * @author Dietmar Malli
 */
public class StringHelpers {
  /**
   * A small helper method to find non-ASCII digits in a String.
   * @param line The String to scan.
   * @return true if there was a non-ASCII digit found in the line.
   */
  public static boolean containsNonASCII (String line)
  {
    int[] charArr = new int[line.length()];
    for (int i = 0; i < line.length(); i++)
      charArr[i] = line.codePointAt(i);
    for(int c : charArr)
    {
      if((c>=1 && c<=9) || c==11 || c==12 || (c>=14 && c<=31) || c>=127)
      {
        return true;
      }
    }
    return false;
  }
}
