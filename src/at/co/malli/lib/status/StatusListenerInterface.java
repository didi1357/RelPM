/*
 * Created: 2013-11-12
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
package at.co.malli.lib.status;

import java.util.EventListener;

/**
 * @author Dietmar Malli
 */
public interface StatusListenerInterface extends EventListener
{
  /**
   * Implementations of this listener interface should implement this method and consume the message contained in
   * e.getMessage();
   * @param e The transported StatusEvent.
   */
  void handleNewStatus(StatusEvent e);
}
