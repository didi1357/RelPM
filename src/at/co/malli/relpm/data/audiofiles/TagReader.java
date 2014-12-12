/*
 * Created: 2014-11-25
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
package at.co.malli.relpm.data.audiofiles;


/**
 * Implementations of this class should read and provide the tag information of an audiofile type.
 * @author Dietmar Malli
 */
public interface TagReader
{
  /**
   * @return Implementations of this method should supply the title of an audiofile which was parsed in the constructor
   *         of the implementation.
   */
  public String getTitle();
  
  /**
   * @return Implementations of this method should supply the artist of an audiofile which was parsed in the constructor
   *         of the implementation.
   */
  public String getArtist();
  
  /**
   * @return Implementations of this method should supply the length in seconds of an audiofile which was parsed in the
   *         constructor of the implementation.
   */
  public int getLengthSeconds();
}
