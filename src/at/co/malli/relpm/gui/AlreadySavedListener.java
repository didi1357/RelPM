/*
 * Created: 2014-12-08
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

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * @author Dietmar Malli
 */
public class AlreadySavedListener implements ListDataListener{
  private final MainGUI reference;

  public AlreadySavedListener (MainGUI reference)
  {
    this.reference = reference;
  }

  @Override
  public void intervalAdded (ListDataEvent e)
  {
    reference.handleWriteWorkerOutput(false);
  }


  @Override
  public void intervalRemoved (ListDataEvent e)
  {
    reference.handleWriteWorkerOutput(false);
  }


  @Override
  public void contentsChanged (ListDataEvent e)
  {
    reference.handleWriteWorkerOutput(false);
  }
  
}
