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

import at.co.malli.lib.data.FileHelpers;
import at.co.malli.relpm.data.ResourceUtils;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * @author Dietmar Malli
 */
public class M3ULineWriter implements PlaylistLineWriter
{
  /**
   * Writes an #EXTINF: line if the info is available and a line with the relative path in every case.
   * @param w The stream into which it should be written.
   * @param playlist The playlist to write. This is needed for relative path calculation.
   *                 (If initially parsed file != this file)
   * @param track The file containing the information to write.
   * @throws java.io.IOException If an error occured during writing to the stream.
   */
  @Override
  public void writeTrackFile (BufferedWriter w, File playlist, TrackFile track) throws IOException, ResourceUtils.PathResolutionException
  {
    int lengthSeconds = track.getLengthSeconds();
    String artist = track.getArtist();
    String title = track.getTitle();
    if(lengthSeconds != -1 && artist != null && title != null)
      w.append("#EXTINF:"+track.getLengthSeconds()+","+track.getArtist()+" - "+track.getTitle()+"\r\n");
    if(playlist.getAbsolutePath().equals(track.getParsedPlaylistFile().getAbsolutePath()))
      w.append(track.getRelativePath()+"\r\n\r\n"); //Change nothing.. It's the same file object which was read...
    else
    {
      String relative = FileHelpers.calculateRelativePath(playlist.getParentFile(), track.getAbsolutePath());
      w.append(relative+"\r\n\r\n");
    }
  }

  /**
   * Writes #EXTM3U
   * @param w The stream into which it should be written.
   * @param playlist The playlist to write.
   * @param encoding The encoding in which the playlist is written. XML would need that.
   * @throws java.io.IOException If an error occured during writing to the stream.
   */
  @Override
  public void writeIntro (BufferedWriter w, File playlist, String encoding) throws IOException
  {
    w.append("#EXTM3U\r\n");
  }

  /**
   * Writes nothing... Unused for M3U...
   * @param w The stream into which it should be written.
   * @param playlist The playlist to write.
   * @param encoding The encoding in which the playlist is written.
   * @throws java.io.IOException If an error occured during writing to the stream.
   */
  @Override
  public void writeOutro (BufferedWriter w, File playlist, String encoding) throws IOException
  {
  }
  
}
