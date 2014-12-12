/*
 * Created: 2014-11-30
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
package at.co.malli.relpm.gui;

import at.co.malli.relpm.data.SettingsProvider;
import at.co.malli.relpm.data.playlist.PlaylistFileReadWorker;
import at.co.malli.lib.status.StatusNotifier;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import javax.swing.TransferHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Dietmar Malli
 */
public class CenterTransferHandler extends TransferHandler
{

  private static final Logger logger = LogManager.getLogger(CenterTransferHandler.class.getName());
  private final MainGUI ref;


  /**
   * This class makes it possible to drag a new file into the GUI.
   * @param ref Reference to the main gui window.
   */
  public CenterTransferHandler (MainGUI ref)
  {
    super();
    this.ref = ref;
  }


  /**
   * This limits the importable stuff to files... Users aren't able to drag for example pure text into the window.
   * @param support An instance of TransferHandler.TransferSupport. This is usually created by Java.
   * @return This method should return true if the dragged thing is a File.
   */
  @Override
  public boolean canImport (TransferHandler.TransferSupport support)
  {
    if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
      return false;

    return true;
  }

  /**
   * This method actually imports the data. It is usually called by Java.
   * @param support An instance of TransferHandler.TransferSupport. This is usually created by Java.
   * @return This method should return true if the dragged data was successfully imported.
   */
  @Override
  public boolean importData (TransferHandler.TransferSupport support)
  {
    if (!canImport(support))
      return false;
    
    Transferable t = support.getTransferable();
    try
    {
      java.util.List<File> list = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

      File playlist = list.get(0); //Dirty way of disabling multi-fileopen
      if (playlist.exists() && playlist.isFile())
      {
        SettingsProvider.getInstance().set("gui.lastFile", playlist.getAbsoluteFile());
        PlaylistFileReadWorker readWorker = new PlaylistFileReadWorker(ref, playlist);
        readWorker.execute();
      }
      else
        throw new Exception("Can not open file " + playlist.getAbsolutePath());
    }
    catch (UnsupportedFlavorException e)
    {
      logger.error(e.getMessage());
      return false;
    }
    catch (IOException e)
    {
      logger.error(e.getMessage());
      return false;
    }
    catch (Exception ex)
    {
      StatusNotifier.getInstance().fireStatusEvent(this, ex.getMessage());
      logger.error(ex.getMessage());
      return false;
    }

    return true;
  }
}
