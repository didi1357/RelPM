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

import java.util.EventObject;

/**
 * @author Dietmar Malli
 */
public class StatusEvent extends EventObject
{
  private String message = "NO EVENT MESSAGE WAS SET!";


  /**
   * @param source The source because of which the event was created.
   * @param message The message which should be delivered.
   */
  public StatusEvent(Object source, String message)
  {
    super(source);
    this.message = message;
  }

  /**
   * @return The message which should be consumed by the listeners.
   */
  public String getMessage()
  {
    return message;
  }
  
}
