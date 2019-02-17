/*
 * Created: 2014-11-23
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
import at.co.malli.relpm.data.audiofiles.TrackFileList;
import at.co.malli.lib.gui.ExceptionDisplayer;
import at.co.malli.relpm.gui.MainGUI;
import at.co.malli.lib.gui.ProgressDialog;
import at.co.malli.lib.status.StatusNotifier;
import at.co.malli.lib.data.StringHelpers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Dietmar Malli
 */
public class PlaylistFileReadWorker extends SwingWorker<TrackFileList,Integer> //Finished,Last processed Line..
{
  private static final Logger logger = LogManager.getLogger(PlaylistFileReadWorker.class.getName());
  
  public static final int STATE_AUDIOFILE_NOT_FOUND = -3;   //Wait for user to show the file location...
  public static final int STATE_NONASCII_FOUND = -2;   //Wait for user to answer the encoding-question...
  public static final int STATE_READING = -1;          //-1 --> Reading File into RAM...
  public static final int STATE_BEGIN_PARSING = 0;     //Positive Values = Parsed Lines...
  
  private final MainGUI mainGui;
  private final File playlistFile;
  private final ArrayList<String> lines;
  private String encoding;
  private int lineCount;
  
  private final ProgressDialog prog;
  private final AtomicBoolean userAbort;
  
  private String unfoundPlaylistPath;
  private File userSuppliedAudioFile;
  
  /**
   * This class uses external classes to parse a paylist and does also some interaction with the user if needed.
   * @param reference A reference to the MainGUI from which it is usually set up.
   * @param file The playlist File object which should be read.
   */
  public PlaylistFileReadWorker (MainGUI reference, File file)
  {
    this.mainGui = reference;
    this.playlistFile = file;
    this.lines = new ArrayList<>();
    this.encoding = "DEFAULT";
    this.lineCount = 0;
    
    this.prog = new ProgressDialog(null, false); //must not be modal because of being called in edt...
    this.userAbort = new AtomicBoolean(false);
  }

  /**
   * As this method isn't ran on the EDT it can do time consuming tasks, like parsing a long playlist over the network.
   * @return The parsed playlist.
   * @throws Exception if something goes wrong ;)
   */
  @Override
  protected TrackFileList doInBackground () throws Exception
  {
    publish(STATE_READING);
    BufferedReader r = new BufferedReader(new FileReader(playlistFile));
    while(r.ready())
    {
      if(!prog.isPressedCancel())
        lines.add(r.readLine());
      else
        throw new Exception("The user aborted the file reading procedure...");
    }
    r.close();
    r = null;
    for(String line : lines)
    {
      if(StringHelpers.containsNonASCII(line))
      {
        encoding = "NONASCII";
        break;
      }
    }
    if(encoding.equals("NONASCII"))
    {
      publish(STATE_NONASCII_FOUND);
      synchronized(PlaylistFileReadWorker.class)
      {
        PlaylistFileReadWorker.class.wait();
      }
      if(userAbort.get())
        throw new Exception("The user closed the encoding dialog...");
    }
    if(!encoding.equals("DEFAULT"))
    {
      publish(STATE_READING);
      lines.clear();
      r = new BufferedReader(new InputStreamReader(new FileInputStream(playlistFile),encoding));
      while(r.ready())
      if(!prog.isPressedCancel())
        lines.add(r.readLine());
      else
        throw new Exception("The user aborted the non ASCII file reading procedure...");
      r.close();
      r = null;
    }
    lineCount = lines.size();
    publish(STATE_BEGIN_PARSING);
    PlaylistLineParser parser;
    String extension = FileHelpers.getExtension(playlistFile);
    switch(extension)
    {
      case "m3u":
      case "m3u8":
        parser = new M3ULineParser(playlistFile);
      break;
        
      default:
        parser = new M3ULineParser(playlistFile);
      break;
    }
    for(int i = 0; i<lineCount; i++)
    {
      if(prog.isPressedCancel())
        throw new Exception("The user closed the parsing dialog...");
      publish(i+1);
      try
      {
        if(userSuppliedAudioFile != null)
        {
          parser.addLine(lines.get(i),userSuppliedAudioFile);
          userSuppliedAudioFile = null;
        }
        else
          parser.addLine(lines.get(i));
      }
      catch (FileNotFoundException ex)
      {
        unfoundPlaylistPath = lines.get(i);
        publish(STATE_AUDIOFILE_NOT_FOUND);
        synchronized(PlaylistFileReadWorker.class)
        {
          PlaylistFileReadWorker.class.wait();
        }
        if(userAbort.get())
          throw new Exception("The user closed the JFileChooser...");
        publish(STATE_BEGIN_PARSING);
        i--; //redo current with new information... (for will increment again...)
      }
    }
    return new TrackFileList(parser.getTracks(),playlistFile);
  }


  /**
   * As this method is ran on the EDT the whole user interaction is done here. The doInBackgroundMethod is usually
   * blocked by a wait() call during the user interaction is active. It must be notified from here.
   * @param chunks 
   */
  @Override
  protected void process (List<Integer> chunks)
  {
    for(int state : chunks)
    {
      switch(state)
      {
        case STATE_READING:
          prog.setText("Reading file into RAM");
          prog.setValue(0);
          prog.setVisible(true);
        break;
        case STATE_NONASCII_FOUND:
          prog.setVisible(false);
          Set<String> keySet = Charset.availableCharsets().keySet();
          String[] charsets = keySet.toArray(new String[0]);
          Object answer = JOptionPane.showInputDialog(null,
                          "A Non-ASCII Digit was found in the file... Which Encoding should be used to read the file?",
                          "Character Encoding",JOptionPane.QUESTION_MESSAGE,null,charsets,null);
          if(answer == null)
            userAbort.set(true);
          else
            encoding = (String) answer;
          synchronized(PlaylistFileReadWorker.class)
          {
            PlaylistFileReadWorker.class.notifyAll();
          }
        break;
        case STATE_BEGIN_PARSING:
          prog.setText("Parsing M3U lines");
          prog.setMaximum(lineCount);
          prog.setValue(0);
          prog.setVisible(true);
        break;
        case STATE_AUDIOFILE_NOT_FOUND:
          prog.setVisible(false);
          JOptionPane.showMessageDialog(null, "The file " + unfoundPlaylistPath +
                                        " was not found... Please locate it in the following dialog.", "File not found",
                                        JOptionPane.INFORMATION_MESSAGE);
          JFileChooser chooser = new JFileChooser();
          userSuppliedAudioFile = playlistFile; //initialize with Playlist-Directory...
          if(userSuppliedAudioFile != null)
            chooser.setSelectedFile(userSuppliedAudioFile);
          FileNameExtensionFilter mp3 = new FileNameExtensionFilter("MP3-Files (.mp3)", "mp3");
          FileNameExtensionFilter audio = new FileNameExtensionFilter("Audio-Files (.mp3, .wav, .ogg)", "mp3", "wav", "ogg");
          chooser.setFileFilter(mp3);
          chooser.addChoosableFileFilter(audio);
          chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
          int returnVal = chooser.showOpenDialog(null);
          if(returnVal == JFileChooser.APPROVE_OPTION)
            userSuppliedAudioFile = chooser.getSelectedFile();
          else
            userAbort.set(true);
          synchronized(PlaylistFileReadWorker.class)
          {
            PlaylistFileReadWorker.class.notifyAll();
          }
        default: //Parsed line number...
          prog.setValue(state);
        break;
      }
    }
  }
  
  /**
   * This method is called from the EDT thread. So I can return values to the MainGUI from here.
   */
  @Override
  protected void done()
  {
    prog.dispose();
    try
    {
      TrackFileList data = get();
      logger.trace("Finished reading playlist...");
      StatusNotifier.getInstance().fireStatusEvent(this, playlistFile.getAbsolutePath() + " was successfully read...");
      mainGui.handleReadWorkerOutput(data);
    }
    catch (ExecutionException ex)
    {
      Throwable cause = ex.getCause();
      StatusNotifier.getInstance().fireStatusEvent(this, cause.getMessage());
      logger.trace(cause.getMessage());
    }
    catch (Exception ex)
    {
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
  }
}
