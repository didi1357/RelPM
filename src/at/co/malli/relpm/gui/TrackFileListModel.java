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
package at.co.malli.relpm.gui;

import at.co.malli.relpm.data.audiofiles.TrackFile;
import at.co.malli.relpm.data.audiofiles.TrackFileList;
import java.io.File;
import java.util.ArrayList;
import javax.swing.AbstractListModel;

/**
 * @author Dietmar Malli
 */
public class TrackFileListModel extends AbstractListModel<TrackFile>
{
  private TrackFileList list;

  /**
   * A reference to the list which should be used in other methods.
   * @param list 
   */
  public void setList (TrackFileList list)
  {
    this.list = list;
  }
  
  /**
   * @return The current size of the list or null if no list is set yet.
   */
  @Override
  public int getSize ()
  {
    if(list==null)
      return 0;
    else
      return list.getSize();
  }
  
  /**
   * Getter method for TrackFile objects contained in the list.
   * @param index Index of the wanted file.
   * @return The TrackFile contained in the list
   */
  @Override
  public TrackFile getElementAt (int index)
  {
    return list.get(index);
  }
  /**
   * This method removes a TrackFile from the list.
   * @param val Index of the wanted file.
   * @return The removed TrackFile from the ArrayList.
   */
  public TrackFile remove (int val)
  {
    TrackFile removed = list.remove(val);
    fireIntervalRemoved(this, val, val);
    return removed;
  }
  /**
   * Inserts elements from an ArrayList at the given position. elements.get(0) will be at this position once this method
   * is done.
   * @param index The position at which the files should be inserted.
   * @param elements An ArrayList containing elements which should be inserted.
   */
  public void add (int index, ArrayList<TrackFile> elements)
  {
    int size = elements.size();
    list.add(index, elements);
    fireIntervalAdded(this, index,index+size-1);
  }

  /**
   * @param list The list which should be modified/displayed by the GUI.
   */
  public void addInitial (TrackFileList list)
  {
    this.list = list;
    if(list.getSize() > 0)
      fireIntervalAdded(this, 0, list.getSize() - 1);
  }
  /**
   * Seraches for the index of an element in the list.
   * @param reference The reference of the object to be searched.
   * @return Integer value representing the current index of the element.
   */
  public int indexOf (TrackFile reference)
  {
    return list.indexOf(reference);
  }
  /**
   * @return The File object representing the playlist in the list.
   */
  public File getPlaylist ()
  {
    return list.getPlaylist();
  }
  /**
   * @return The TrackFileList referenced in this object.
   */
  public TrackFileList getList ()
  {
    return list;
  }
}
