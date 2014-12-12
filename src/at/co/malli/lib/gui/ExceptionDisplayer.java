/*
 * Created: 2014-11-20
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
package at.co.malli.lib.gui;

import javax.swing.JOptionPane;


/**
 * @author Dietmar Malli
 */
public class ExceptionDisplayer
{
  /**
   * Displays the content of an exception in a new window.
   * @param ex The exception to be displayed.
   */
  public static void showErrorMessage (Exception ex)
  {
    ex.printStackTrace();
    JOptionPane.showMessageDialog(null,ex.getMessage(),"An error occured...",JOptionPane.ERROR_MESSAGE);
  }
}
