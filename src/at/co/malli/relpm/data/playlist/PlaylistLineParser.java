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
package at.co.malli.relpm.data.playlist;

import at.co.malli.relpm.data.ResourceUtils.PathResolutionException;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;


/**
 * This interface should be implemented by classes which parse playlist types. (for example .m3u or .pls)
 * @author Dietmar Malli
 */
public interface PlaylistLineParser
{  
  /**
   * When a line of the playlist file was successfully decoded it should be parsed by this method.
   * @param playlistLine One line of the earlier supplied playlist File.
   * @throws FileNotFoundException if the track referenced in the m3uLine was not found.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the track referenced in the playlistLine
   *         can not be transalted into a relative path. This will occur if the path of the playlist is for example on
   *         C:\ and the referenced track on D:\.
   */
  public void addLine (String playlistLine) throws FileNotFoundException, PathResolutionException;
  
  //user showed a file... try to find files in that directory...
  /**
   * When the line was supplied to addLine (String playlistLine) and it threw a FileNotFoundException, the user can be
   * asked to supply the location of the file on it's own. Implementations of this method should search for the filename
   * supplied in playlistLine in the directory referenced by the file which the user showed us.
   * @param playlistLine One line of the earlier supplied playlist File.
   * @param userSuppliedAudioFile File object representing the track.
   * @throws FileNotFoundException if the track referenced in the m3uLine was still not found.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the track referenced in the playlist can
   *         not be transalted into a relative path. This will occur if the path of the playlist is for example on C:\
   *         and the referenced track on D:\
   */
  public void addLine (String playlistLine, File userSuppliedAudioFile) throws FileNotFoundException, PathResolutionException;
  
  /**
   * @return Implemenations of this class should return an ArrayList containing the TrackFile objects parsed from the
   *         playlist.
   */
  public ArrayList<TrackFile> getTracks ();


}
