/*
 * Created: 2014-11-27
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
import at.co.malli.lib.data.StringHelpers;
import at.co.malli.lib.gui.ExceptionDisplayer;
import at.co.malli.lib.gui.ProgressDialog;
import at.co.malli.lib.status.StatusNotifier;
import at.co.malli.relpm.data.SettingsProvider;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import at.co.malli.relpm.data.audiofiles.TrackFileList;
import at.co.malli.relpm.gui.MainGUI;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Dietmar Malli
 */
public class PlaylistFileWriteWorker extends SwingWorker<Object,Integer> //useless,states
{
  private static final Logger logger = LogManager.getLogger(PlaylistFileWriteWorker.class.getName());
  
  public static final int STATE_SEARCH_NONASCII = -1;
  public static final int STATE_NONASCII_FOUND = -2;
  public static final int STATE_WRITING_LAST100 = -3;
  public static final int STATE_BEGIN_WRITING = 0; //Positive values == written line..
  
  private final ProgressDialog prog;
  
  private AtomicBoolean userAbort;
  private String encoding;
  
  private final TrackFileList list;
  private final File playlistFile;
  private final MainGUI mainGUI;
  
  public PlaylistFileWriteWorker (MainGUI reference, TrackFileList toSave, File toWrite)
  {
    this.prog = new ProgressDialog(null, false); //must not be modal because of being called in edt...
    this.list = toSave;
    this.encoding = "DEFAULT";
    this.playlistFile = toWrite;
    this.mainGUI = reference;
    this.userAbort = new AtomicBoolean(false);
  }
  
  @Override
  protected Object doInBackground () throws Exception
  {
    publish(STATE_SEARCH_NONASCII);
    for(TrackFile item : list)
    {
      String info = item.getRelativePath();
      if(item.getArtist()!=null && item.getTitle()!=null)
        info+=" "+item.getArtist()+" "+item.getTitle();
      if(StringHelpers.containsNonASCII(info))
      {
        encoding = "NONASCII";
        logger.trace("Non-ASCII digit found in line:"+info);
        break;
      }
    }
    if(encoding.equals("NONASCII"))
    {
      publish(STATE_NONASCII_FOUND);
      synchronized(PlaylistFileWriteWorker.class)
      {
        PlaylistFileWriteWorker.class.wait();
      }
      if(userAbort.get())
        throw new Exception("The user closed the encoding dialog...");
    }
    publish(STATE_BEGIN_WRITING);
    BufferedWriter bw;
    if(!encoding.equals("DEFAULT"))
      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(playlistFile),encoding));
    else
      bw = new BufferedWriter(new FileWriter(playlistFile)); //ASCII-only.. no matter which encoding...
    PlaylistLineWriter lineWriter;
    String extension = FileHelpers.getExtension(playlistFile);
    switch(extension)
    {
      case "m3u":
      case "m3u8":
        lineWriter = new M3ULineWriter();
      break;
        
      default:
        lineWriter = new M3ULineWriter();
      break;
    }
    lineWriter.writeIntro(bw,playlistFile,encoding);
    for(int i = 0; i<list.getSize(); i++)
    {
      publish(i+1);
      lineWriter.writeTrackFile(bw, playlistFile, list.get(i));
    }
    lineWriter.writeOutro(bw,playlistFile,encoding);
    bw.flush();
    bw.close();
    if(SettingsProvider.getInstance().getBoolean("data.export.last100")) //Export another list with the last 100 songs
    {
      publish(STATE_WRITING_LAST100);
      String last100Dir = playlistFile.getParentFile().getAbsolutePath();
      String baseFilename = playlistFile.getName();
      String baseFileExtension = FileHelpers.getExtension(playlistFile);
      int index = baseFilename.indexOf("."+baseFileExtension);
      String wantedFilename = baseFilename.substring(0, index);
      File wantedFile = new File(last100Dir+"/"+wantedFilename+"_last100."+baseFileExtension);
      
      if(!encoding.equals("DEFAULT"))
        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(wantedFile),encoding));
      else
        bw = new BufferedWriter(new FileWriter(wantedFile)); //ASCII-only.. no matter which encoding...
      lineWriter.writeIntro(bw,wantedFile,encoding);
      int firstIndex = (list.getSize() >= 100 ? list.getSize() - 100 : 0);
      int lastIndex = list.getSize();
      for(int i = firstIndex; i < lastIndex; i++)
        lineWriter.writeTrackFile(bw, wantedFile, list.get(i));
      lineWriter.writeOutro(bw,wantedFile,encoding);
      bw.flush();
      bw.close();
    }
    return new Object();
  }

  @Override
  protected void process (List<Integer> chunks)
  {
    for(int state : chunks)
    {
      switch(state)
      {
        case STATE_SEARCH_NONASCII:
          prog.setText("Searching for NON-ASCII digits");
          prog.setValue(0);
          prog.setVisible(true);
        break;
        case STATE_NONASCII_FOUND:
          prog.setVisible(false);
          Set<String> keySet = Charset.availableCharsets().keySet();
          String[] charsets = keySet.toArray(new String[0]);
          Object answer = JOptionPane.showInputDialog(null,
                          "A Non-ASCII Digit was found in the information which should be written. "
                          + "Which Encoding should be used to write the file?",
                          "Character Encoding",JOptionPane.QUESTION_MESSAGE,null,charsets,null);
          if(answer == null)
            userAbort.set(true);
          else
            encoding = (String) answer;
          synchronized(PlaylistFileWriteWorker.class)
          {
            PlaylistFileWriteWorker.class.notifyAll();
          }
        break;
        case STATE_BEGIN_WRITING:
          prog.setText("Writing M3U file");
          prog.setMaximum(list.getSize());
          prog.setValue(0);
          prog.setVisible(true);
        break;
        case STATE_WRITING_LAST100:
          prog.setText("Writing last-100 M3U file");
          prog.setMaximum(100);
          prog.setValue(99); // we all know that feeling ;) Writing 100 files just can't take long..
          prog.setVisible(true);
        break;
        default: //Parsed line number...
          prog.setValue(state);
        break;
      }
    }
  }

  @Override
  protected void done ()
  {
    prog.dispose();
    try
    {
      get(); //Exceptions will be received here...
      mainGUI.handleWriteWorkerOutput(true);
      logger.trace("Finished writing playlist...");
      StatusNotifier.getInstance().fireStatusEvent(this, playlistFile.getName()+ " was successfully written...");
    }
    catch (ExecutionException ex)
    {
      mainGUI.handleWriteWorkerOutput(false);
      Throwable cause = ex.getCause();
      StatusNotifier.getInstance().fireStatusEvent(this, cause.getMessage());
      logger.trace(cause.getMessage());
    }
    catch (Exception ex)
    {
      mainGUI.handleWriteWorkerOutput(false);
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
  }
  
  
  
}
