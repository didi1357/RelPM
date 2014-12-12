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
import at.co.malli.relpm.data.SettingsProvider;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;


/**
 * @author Dietmar Malli
 */
public class M3ULineParser implements PlaylistLineParser
{
  private final ArrayList<TrackFile> tracks = new ArrayList<>();
  private final File playlist;
  private final String[] linePair;

  /**
   * @param currentPlaylist The playlist which will be parsed.
   */
  public M3ULineParser (File currentPlaylist)
  {
    this.playlist = currentPlaylist;
    linePair = new String[2];
  }
  
  /**
   * Reads the tags from a supplied EXTINF line from
   * @param line
   * @param track 
   */
  private void parseEXTINF(String line, TrackFile track)
  {
    if(line==null)
    {
      track.parseTagsFromFile();
      return;
    }
    String info = line.replaceFirst("#EXTINF:","");
    String[] split = info.split(",");
    track.setLengthSeconds(Integer.valueOf(split[0]));
    info = info.substring(info.indexOf(",")+1); //remove until and including the ,
    split = info.split(" - ",2); //Will create an array of two..
    track.setArtist(split[0]);
    track.setTitle(split[1]);
  }
  
  /**
   * When a line of the playlist file was successfully decoded it should be parsed by this method.
   * @param m3uLine One line of the earlier supplied playlist File.
   * @throws FileNotFoundException if the track referenced in the m3uLine was not found.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the track referenced in the m3uLine can not
   *         be transalted into a relative path. This will occur if the path of the playlist is for example on C:\ and
   *         the referenced track on D:\
   */
  @Override
  public void addLine (String m3uLine) throws FileNotFoundException, PathResolutionException
  {
    if(m3uLine.startsWith("#EXTM3U") || m3uLine.startsWith(" ") || m3uLine.startsWith("\n") || m3uLine.startsWith("\r")
       || m3uLine.isEmpty())
      return;//empty lines...
    
    if(m3uLine.startsWith("#EXTINF"))
      linePair[0]=m3uLine;
    else
    {
      linePair[1]=m3uLine;
      
      TrackFile currentTrack = new TrackFile(playlist, linePair[1]); //File not Found Exception could occure here...
      if(SettingsProvider.getInstance().getBoolean("data.import.parseM3Utags"))
        parseEXTINF(linePair[0],currentTrack);
      else
        currentTrack.parseTagsFromFile();
      tracks.add(currentTrack);
      linePair[0]=null;
      linePair[1]=null;
    }
  }

  /**
   * When the line was supplied to addLine (String m3uLine) and it threw a FileNotFoundException, the user can be asked
   * to supply the location of the file on it's own. This function is in turn able to correct the issue once the user
   * supplied the correct file.
   * @param m3uLine One line of the earlier supplied playlist File.
   * @param userSuppliedAudioFile File object representing the track.
   * @throws FileNotFoundException if the track referenced in the m3uLine was still not found.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the track referenced in the m3uLine can not
   *         be transalted into a relative path. This will occur if the path of the playlist is for example on C:\ and
   *         the referenced track on D:\
   */
  @Override
  public void addLine (String m3uLine, File userSuppliedAudioFile) throws FileNotFoundException, PathResolutionException
  {
    if(m3uLine.startsWith("#EXTM3U") || m3uLine.startsWith(" ") || m3uLine.startsWith("\n") || m3uLine.startsWith("\r")
       || m3uLine.isEmpty())
      return;//empty lines...
    
    if(m3uLine.startsWith("#EXTINF"))
      linePair[0]=m3uLine;
    else
    {
      linePair[1]=m3uLine;
    
      TrackFile currentTrack = new TrackFile(playlist, m3uLine, userSuppliedAudioFile); //File not Found Exception could occure here...
      if(SettingsProvider.getInstance().getBoolean("data.import.parseM3Utags"))
        parseEXTINF(linePair[0],currentTrack);
      else
        currentTrack.parseTagsFromFile();
      tracks.add(currentTrack);
      linePair[0]=null;
      linePair[1]=null;
    }
  }
  
  /**
   * @return The ArrayList containing the TrackFile objects parsed from the playlist.
   */
  public ArrayList<TrackFile> getTracks ()
  {
    return tracks;
  }


  
}
