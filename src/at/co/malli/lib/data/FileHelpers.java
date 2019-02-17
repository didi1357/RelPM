/*
 * Created: 2014-11-24
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

import at.co.malli.relpm.data.ResourceUtils;
import java.io.File;


/**
 * @author Dietmar Malli
 */
public class FileHelpers
{
  /**
   * A small helper method to parse the FileExtension by parsing the filename.
   * @param file The file to parse.
   * @return The file extension as a String. (For example "mp3")
   */
  public static String getExtension(File file)
  {
    String[] split = file.getName().split("\\.");
    String extension = split[split.length-1];
    return extension;
  }
  
  /**
   * This function is trying to calculate the relative path of a file.
   * @param playlistDirectory The directory of the playlist to which the path should be relative.
   * @param track An instance containing the valid absolute path which should be converted to a relative path.
   * @return The relative path using an external class.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the path can't be relativized. This can
   *         for instance happen if the user supplies a file on D:\ for a playlist on C:\.
   */
  public static String calculateRelativePath (File playlistDirectory, File track) throws ResourceUtils.PathResolutionException
  {
    String list = playlistDirectory.getAbsolutePath();
    String absolute = track.getAbsolutePath();
    String relative;
    relative = ResourceUtils.getRelativePath(absolute, list, "/");
    return relative;
  }
}
