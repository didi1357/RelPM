/*
 * Created: 2014-12-07
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

import at.co.malli.relpm.data.ResourceUtils;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * @author Dietmar Malli
 */
public interface PlaylistLineWriter
{
  /**
   * Implementations should write one or more lines representing the given TrackFile into the stream.
   * @param w The stream into which it should be written.
   * @param playlist The playlist to write. This is needed for relative path calculation.
   *                 (If initially parsed file != this file)
   * @param track The TrackFile containing the information to write.
   * @throws java.io.IOException If an error occured during writing to the stream.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the track referenced in the playlist can
   *         not be transalted into a relative path. This will occur if the path of the playlist is for example on C:\
   *         and the referenced track on D:\
   */
  public void writeTrackFile (BufferedWriter w, File playlist, TrackFile track) throws IOException, ResourceUtils.PathResolutionException;
  
  /**
   * Implementations should write stuff which needs to be above all track-representing lines. (For example #EXTM3U for
   * an extended M3U file.)
   * @param w The stream into which it should be written.
   * @param playlist The playlist to write.
   * @param encoding The encoding in which the playlist is written. XML would need that.
   * @throws java.io.IOException If an error occured during writing to the stream.
   */
  public void writeIntro (BufferedWriter w, File playlist, String encoding) throws IOException;
  
  /**
   * Implementations should write stuff which needs to under all track-representing lines.
   * @param w The stream into which it should be written.
   * @param playlist The playlist to write.
   * @param encoding The encoding in which the playlist is written.
   * @throws java.io.IOException If an error occured during writing to the stream.
   */
  public void writeOutro (BufferedWriter w, File playlist, String encoding) throws IOException;
}
