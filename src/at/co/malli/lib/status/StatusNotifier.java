/*
 * Created: 2013-11-13
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

import javax.swing.event.EventListenerList;

/**
 * @author Dietmar Malli
 */
public class StatusNotifier
{
  private static volatile StatusNotifier theInstance=null;
  public static StatusNotifier getInstance()
  {
    if(theInstance==null)
    {
      synchronized (StatusNotifier.class)
      {
        if (theInstance==null)
          theInstance = new StatusNotifier();
      }
    }
    
    return theInstance;
  }
  private final EventListenerList listeners = new EventListenerList();
  
  private StatusNotifier()
  {
  }
  
  /**
   * Creates a new status event and notifies all listeners about it.
   * @param source The source because of which the event was created.
   * @param message The message which should be delivered.
   */
  public void fireStatusEvent(Object source, String message)
  {
    notifyStatusListeners(new StatusEvent(source,message));
  }
  
  /**
   * Adds a new StatusListener to the list of "clients" which get notified of new events.
   * @param listener The listener to add to the list.
   */
  public void addStatusListener(StatusListenerInterface listener)
  {
    listeners.add(StatusListenerInterface.class, listener);
  }
  
  /**
   * Removes the referenced listener from the list of "clients" which get notified of new events.
   * @param listener The listener to remove from the list.
   */
  public void removeStatusListener(StatusListenerInterface listener)
  {
    listeners.remove(StatusListenerInterface.class, listener);
  }
  
  /**
   * Notifies all currently registered listeners of the supplied StatusEvent.
   * @param e The event which should be handled.
   */
  protected synchronized void notifyStatusListeners(StatusEvent e)
  {
    for(StatusListenerInterface listener : listeners.getListeners(StatusListenerInterface.class))
    {
      listener.handleNewStatus(e);
    }
  }

}
