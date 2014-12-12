/*
 * Created: 2014-09-09
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

import at.co.malli.lib.data.FileHelpers;
import at.co.malli.relpm.data.ResourceUtils.PathResolutionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Dietmar Malli
 */
public class TrackFile
{
  private static final Logger logger = LogManager.getLogger(TrackFile.class.getName());
  
  private final File parsedPlaylistFile;
  private File absolutePath;
  private String relativePath;   //Path relative to playlist...
  private String title = null;   //Invalid state
  private String artist = null;  //Invalid state
  private int lengthSeconds = -1;//Invalid state


  /**
   * Instances of this class represent a file in a playlist. The methods for relative path resolution and tag reading
   * are called within the constructor of an instance.
   * @param playlist A file object referencing the the playlist to which the path should be relative...
   * @param pathFromPlaylist A String containing the path to the track usually taken from a playlist. It can either be
   *                         relative or absolute.
   * @throws java.io.FileNotFoundException If pathFromPlaylist could not be parsed.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the path of the found file from
   *         pathFromPlaylistcan't be relativized. This can for instance happen if the user supplies a file on D:\ for a
   *         playlist on C:\.
   */
  public TrackFile (File playlist, String pathFromPlaylist) throws FileNotFoundException, PathResolutionException
  {
    this.parsedPlaylistFile = playlist;
    File playlistDirectory = playlist.getParentFile();
    
    File f = new File(pathFromPlaylist); //Case absolute path supplied...
    if(f.isFile())
    {
      absolutePath = f;
      relativePath = FileHelpers.calculateRelativePath(playlistDirectory, absolutePath);
    }
    else
    {
      f = new File(playlistDirectory.getAbsolutePath(),pathFromPlaylist); //case already a relative path...
      if(f.isFile())
      {
        absolutePath = f;
        relativePath = pathFromPlaylist;
      }
      else
        throw new FileNotFoundException("File not found:"+pathFromPlaylist);
    }
    //If one "if()" works, one can read tags now... If none of them had worked one wouldn't have reached this line...
  }
  
  /**
   * Instances of this class represent a file in a playlist. The methods for relative path resolution and tag reading
   * are called within the constructor of an instance. This version of the constructor is usually called if an error
   * occured while trying to find the pathFromPlaylist in the normal constructor. This should give the user the
   * possibillity to point to a file referenced by the playlist which got moved without changing the pathFromPlaylist.
   * @param playlist A File object referencing the the playlist to which the path should be relative...
   * @param pathFromPlaylist A String containing the path to the track usually taken from a playlist. It can either be
   *                         relative or absolute.
   * @param userSuppliedAudioFile A File object pointing to the same file as pathFromPlaylist.
   * @throws java.io.FileNotFoundException If pathFromPlaylist could not be parsed.
   * @throws at.co.malli.relpm.data.ResourceUtils.PathResolutionException if the path of the found file from
   *         pathFromPlaylistcan't be relativized. This can for instance happen if the user supplies a file on D:\ for a
   *         playlist on C:\.
   */
  public TrackFile (File playlist, String pathFromPlaylist, File userSuppliedAudioFile) throws FileNotFoundException, PathResolutionException
  {
    this.parsedPlaylistFile = playlist;
    File playlistDirectory = playlist.getParentFile();
    
    if(userSuppliedAudioFile.isFile())
    {
      String fileName = new File(pathFromPlaylist).getName();
      File wanted = new File(userSuppliedAudioFile.getParent()+"/"+fileName);
      if(wanted.isFile())
        absolutePath = wanted;
      else
        throw new FileNotFoundException("Could not open calculated absolutePath" + wanted.getAbsolutePath());
      relativePath = FileHelpers.calculateRelativePath(playlistDirectory, absolutePath);
    }
    else
      throw new FileNotFoundException("Could not open userSuppliedAudioFile" + userSuppliedAudioFile.getAbsolutePath());
  }
  
  /**
   * This method reads the tags of an audiofile using an external class.
   */
  public void parseTagsFromFile ()
  {
    if(!FileHelpers.getExtension(absolutePath).equals("mp3"))
    {
      logger.warn("Reading the tag info of files other than mp3 is not supported (yet): " + absolutePath);
      artist=null;
      title=null;
      lengthSeconds=-1;
      return;
    }
    
    try
    {
      MP3TagReader reader = new MP3TagReader(absolutePath);
      artist = reader.getArtist();
      title = reader.getTitle();
      lengthSeconds = reader.getLengthSeconds();
    }
    catch (IOException | UnsupportedAudioFileException ex)
    {
      artist = null;
      title = null;
      lengthSeconds = -1;
      logger.warn("Could not read MP3 Tag Info",ex);
    }
  }
  
  /**
   * This Exception will be thrown if the filetype isn't supported yet.
   */
  public class FileTypeException extends Exception
  {
    private FileTypeException (String message)
    {
      super(message);
    }
  }

  /**
   * String representation of the file hopefully making sense.
   * @return The string representation of the file.
   */
  @Override
  public String toString ()
  {
    if(artist != null && title != null)
      return ""+artist+" - "+title;
    else if(title != null)
      return title;
    else
      return relativePath;
  }
  /**
   * Getter method for the title.
   * @return The title as a string or null.
   */
  public String getTitle ()
  {
    return title;
  }
  /**
   * Getter method for the artist.
   * @return The artist as a string or null.
   */
  public String getArtist ()
  {
    return artist;
  }
  /**
   * Getter method for the playing duration of the track.
   * @return The length in seconds as an integer value or -1 if it is unknown.
   */
  public int getLengthSeconds ()
  {
    return lengthSeconds;
  }
  /**
   * Setter method for the title of the track.
   * @param title The title of the track as String.
   */
  public void setTitle (String title)
  {
    this.title = title;
  }
  /**
   * Setter method for the artist of the track.
   * @param artist The artist of the track as String.
   */
  public void setArtist (String artist)
  {
    this.artist = artist;
  }
  /**
   * Setter method for the length of the track in seconds.
   * @param lengthSeconds The length of the track in seconds as integer.
   */
  public void setLengthSeconds (int lengthSeconds)
  {
    this.lengthSeconds = lengthSeconds;
  }
  
  /**
   * Getter method for the relative path.
   * @return The relative path as a string.
   */
  public String getRelativePath()
  {
    return relativePath;
  }
  /**
   * Getter method for the file object which represents the TrackFiles absolute path.
   * @return The absolute path as java.io.File.
   */
  public File getAbsolutePath ()
  {
    return absolutePath;
  }
  /**
   * Getter method for the file object which was initially parsed.
   * @return The initially parsed object as java.io.File.
   */
  public File getParsedPlaylistFile ()
  {
    return parsedPlaylistFile;
  }
  
  
}
