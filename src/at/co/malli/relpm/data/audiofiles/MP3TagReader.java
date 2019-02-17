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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * @author Dietmar Malli
 */
public class MP3TagReader implements TagReader
{
  private final String title;
  private final String artist;
  private final int lengthSeconds;
  private final File file;


  /**
   * Instances of this class read the tag information of an MP3 file using an external library.
   * @param file Instance of a file object containing the absolute path of the file to parse.
   * @throws IOException if an I/O-Exception occurs.
   * @throws UnsupportedAudioFileException if the File does not point to valid audio file data recognized by the system.
   */
  public MP3TagReader (File file) throws IOException, UnsupportedAudioFileException
  {
    this.file = file;
    
    AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(this.file);
    Map<String, Object> properties = baseFileFormat.properties(); //Bug workaround?
    artist = (String) properties.get("author"); //null is a sane value for TrackFile...
    title = (String) properties.get("title"); //null is a sane value for TrackFile...
    if(properties.get("duration") != null)
      lengthSeconds = (int) (((long)properties.get("duration"))/(1000*1000));
    else
      lengthSeconds = -1;
  }

  
  /**
   * @return The title parsed in the constructor of an instance.
   */
  @Override
  public String getTitle ()
  {
    return title;
  }
  
  /**
   * @return The artist parsed in the constructor of an instance.
   */
  @Override
  public String getArtist ()
  {
    return artist;
  }
  
  /**
   * @return The length in seconds parsed in the constructor of an instance.
   */
  @Override
  public int getLengthSeconds ()
  {
    return lengthSeconds;
  }
  
}
