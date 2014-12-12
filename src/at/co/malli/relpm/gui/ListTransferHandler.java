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


import at.co.malli.lib.data.FileHelpers;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import at.co.malli.lib.status.StatusNotifier;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Dietmar Malli
 */
public class ListTransferHandler extends TransferHandler
{
  private static final Logger logger = LogManager.getLogger(ListTransferHandler.class.getName());
  private final JList list;
  private final TrackFileListModel model;

  /**
   * This class makes it possible to drag a new file into the GUI or to move files with drag and drop.
   * @param list Reference to the jList which should be modified.
   */
  public ListTransferHandler (JList list)
  {
    this.list = list;
    this.model = (TrackFileListModel) list.getModel();
  }

  /**
   * Tells java which icon to use when dragging an element, or wheter it is possible to press Ctrl to copy stuff or not.
   * @param c The dragged JComponent.
   * @return An integer constant from TransferHandler.
   */
  @Override
  public int getSourceActions (JComponent c)
  {
    return TransferHandler.MOVE;
  }
  
  /**
   * Creates an instance of DraggedIndices if the user begins to drag an element from the list.
   * @param c The dragged element from the list.
   * @return The newly created instance of DraggedIndices.
   */
  @Override
  protected Transferable createTransferable(JComponent c)
  {
    ArrayList<Integer> indices = new ArrayList<>();
    int[] selectedIndices = list.getSelectedIndices();
    for(int val : selectedIndices)
      indices.add(val);
    return new DraggedIndices(indices);
  }
  
  /**
   * This limits the importable stuff to files and elements from the jList...
   * @param info An instance of TransferHandler.TransferSupport. This is usually created by Java.
   * @return This method should return true if the dragged thing is a file or a jList element.
   */
  public boolean canImport (TransferHandler.TransferSupport info)
  {
    if ((!info.isDataFlavorSupported(DraggedIndices.dataFlavor)) && (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)))
      return false;
    JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
    if (dl.getIndex() == -1)
      return false;
    else
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
    if (!support.isDrop())
      return false;
    if (!canImport(support))
      return false;

    Transferable t = support.getTransferable();
    try
    {
      JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
      DataFlavor[] transferDataFlavors = t.getTransferDataFlavors();
      if(transferDataFlavors[0]==DraggedIndices.dataFlavor) //A list object was dragged...
      {
        ArrayList<Integer> indices = (ArrayList<Integer>) t.getTransferData(DraggedIndices.dataFlavor);
        if(indices.isEmpty())
          System.out.println("EMPTY");

        boolean isLast = false;
        TrackFile beforeWhichToInsert = null; //maybe not needed...
        if(dl.getIndex() >= list.getModel().getSize())
        {
          isLast=true;
        }
        else
        {
          TrackFile objectAfterDropIndex = model.getElementAt(dl.getIndex());
          int indexOfObjectAfterDropIndex = dl.getIndex();

          int i=indexOfObjectAfterDropIndex;
          while(indices.contains(i))
            i++;
          beforeWhichToInsert = model.getElementAt(i);
        }

        Collections.sort(indices);
        Collections.reverse(indices); //Biggest to smallest.. everything else would destroy the indices while removing...
        ArrayList<TrackFile> elements = new ArrayList<>();
        for(int val : indices)
          elements.add(model.remove(val));
        Collections.reverse(elements); //old ordering...

        //Find the new index of the object which should be after the inserted elements:
        int newIndex;
        if(isLast)
          newIndex = model.getSize();
        else
          newIndex = model.indexOf(beforeWhichToInsert);
        model.add(newIndex, elements);
      }
      else //A File was dragged onto the list...
            {
        List<File> list = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        ArrayList<TrackFile> elements = new ArrayList<>();
        for(File item : list)
        {
          if(FileHelpers.getExtension(item).toLowerCase().equals("mp3"))
          {
            TrackFile current = new TrackFile(model.getPlaylist(), item.getAbsolutePath());
            current.parseTagsFromFile();
            elements.add(current);
          }
          else
            StatusNotifier.getInstance().fireStatusEvent(this, "Warning. Up to now only MP3 files are supported.");
        }
        if(!elements.isEmpty())
          model.add(dl.getIndex(), elements);
        else
          return false;
      }
      
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
      StatusNotifier.getInstance().fireStatusEvent(this, "An Error occured:"+ ex.getMessage());
      logger.error(ex.getMessage());
      return false;
    }

    return true;
  }
  
  private static class DraggedIndices implements Transferable
  {
    private final ArrayList<Integer> values;
    private static final DataFlavor dataFlavor = new DataFlavor(DraggedIndices.class,"DraggedIndices");

    /**
     * Instances of this class represent the selected elements from the jList which are being dragged.
     * @param values ArrayList of Integer values.
     */
    public DraggedIndices (ArrayList<Integer> values)
    {
      this.values = values;
    }

    /**
     * @return The dataFlavors which can be represented by this class.
     */
    @Override
    public DataFlavor[] getTransferDataFlavors ()
    {
      DataFlavor[] arr = new DataFlavor[1];
      arr[0] = DraggedIndices.dataFlavor;
      return arr;
    }

    /**
     * @param flavor The dataFlavor to compare with the supported dataFlavor
     * @return True if the dataFlavor supplied by the caller is the same as this class supports.
     */
    @Override
    public boolean isDataFlavorSupported (DataFlavor flavor)
    {
      return flavor == dataFlavor;
    }

    /**
     * @param flavor The type of data requested by the caller. Note: Only "DraggedIndices" is supported by this class...
     * @return ArrayList of Integer values which are being dragged.
     * @throws UnsupportedFlavorException if the supplied dataFlavor isn't supported by this class.
     * @throws IOException in no case. This is only required by the Interface.
     */
    @Override
    public Object getTransferData (DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
      if(flavor!=dataFlavor)
        throw new UnsupportedFlavorException(flavor);
      return values;
    }
  }
}
