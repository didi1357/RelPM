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
package at.co.malli.relpm.data.audiofiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Dietmar Malli
 */
public class TrackFileList implements Iterable<TrackFile>
{
  private final File playlist;
  private final ArrayList<TrackFile> list;

  /**
   * Instances of this class can hold a reference to a playlist file and instances of TrackFiles relative to that
   * playlist in an ArrayList.
   * @param list The ArrayList with TrackFiles in the playlist.
   * @param playlist The corresponding playlist.
   */
  public TrackFileList (ArrayList<TrackFile> list, File playlist)
  {
    this.list = list;
    this.playlist = playlist;
  }

  /**
   * @return The current size of the list.
   */
  public int getSize ()
  {
    return list.size();
  }
  /**
   * Getter method for TrackFile objects contained in the ArrayList.
   * @param index Index of the wanted file.
   * @return The TrackFile contained in the ArrayList
   */
  public TrackFile get (int index)
  {
    return list.get(index);
  }
  /**
   * This method removes a TrackFile from the ArrayList.
   * @param val Index of the wanted file.
   * @return The removed TrackFile from the ArrayList.
   */
  public TrackFile remove (int val)
  {
    return list.remove(val);
  }
  /**
   * Inserts elements from an ArrayList at the given position. elements.get(0) will be at this position once this method
   * is done.
   * @param index The position at which the files should be inserted.
   * @param elements An ArrayList containing elements which should be inserted.
   */
  public void add (int index, ArrayList<TrackFile> elements)
  {
    int count = elements.size();
    for(int i = index; i<index+count; i++)
      list.add(i, elements.remove(0));
  }
  /**
   * Seraches for the index of an element in the ArrayList.
   * @param reference The reference of the object to be searched.
   * @return Integer value representing the current index of the element.
   */
  public int indexOf (TrackFile reference)
  {
    return list.indexOf(reference);
  }
  /**
   * @return The File object representing the playlist.
   */
  public File getPlaylist ()
  {
    return playlist;
  }
  /**
   * @return An instance of Iterator<TrackFile> of the ArrayList contained in this class. This is needed to be able to 
   * "foreach" the TrackFileList.
   */
  @Override
  public Iterator<TrackFile> iterator ()
  {
    return list.iterator();
  }
  
  
}
