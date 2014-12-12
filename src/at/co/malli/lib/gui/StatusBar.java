/*
 * Created: 2013-08-11
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


import at.co.malli.relpm.data.SettingsProvider;
import at.co.malli.lib.status.StatusEvent;
import at.co.malli.lib.status.StatusListenerInterface;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JTextField;

/**
 * @author Dietmar Malli
 */
public class StatusBar extends JTextField implements StatusListenerInterface
{
  private ArrayList<Future<Object>> futureList = new ArrayList<>();
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  
  private class TextChangeThread implements Callable
  {
    private final StatusBar instance;
    private final String nextText;
    private final long timeout;
    
    /**
     * Instances of this thread change the text, wait a given time an remove the set text after waiting.
     * @param reference A reference to the StatusBar instance which should be modified.
     * @param nextText The text which should be displayed.
     * @param timeout The time in milliseconds during which the text will be displayed.
     */
    public TextChangeThread (StatusBar reference, String nextText, long timeout)
    {
      this.instance = reference;
      this.nextText = nextText;
      this.timeout = timeout;
    }

    /**
     * @return new Object(); because the Callable Interface requires to return something.
     * @throws Exception if something goes wrong.
     */
    @Override
    public Object call () throws Exception
    {
      instance.setText(nextText);
      try
      {
        Thread.sleep(timeout);
      }
      catch (InterruptedException ex) { }
      finally
      {
        instance.setText("");
      }
      
      return new Object();
    }
  }
  
  /**
   * This method is called by the StatusNotifier. 
   * @param e The event which occured. It's text will be displayed.
   */
  @Override
  public void handleNewStatus (StatusEvent e)
  {
    this.setInfo(e.getMessage());
  }
  
  /**
   * This will just call setInfo() with the default notificationTime.
   * @param information The Information to be displayed.
   */
  private void setInfo (String information)
  {
    setInfo(information,SettingsProvider.getInstance().getLong("gui.statusBar.notificationTime"));
  }
  
  /**
   * This method adds a new TextChangeThread to the executor. This way it's guaranteed that every text will be displayed
   * as long as it should be.
   * @param information The text to be displayed.
   * @param timeout The displaying duration.
   */
  private void setInfo (String information, long timeout)
  {
    synchronized(StatusBar.class)
    {
      if(futureList.size()==5)
        futureList.clear(); //keep it small..
      futureList.add(executor.submit(new TextChangeThread(this,information,timeout)));
    }
  }
  
  /**
   * This method will interrupt any given TextChangeThread and shutdown the executor.
   */
  public void shutdown ()
  {
    executor.shutdownNow();
  }
  
  /**
   * ToDo: Is exception handling even needed here? What could actually go wrong..
   *       Maybe use Runnable instead of Callable...
   * @return An ArrayList of Exceptions which occured in a TextChangeThread.
   */
  public ArrayList<Exception> getExceptions ()
  {
    ArrayList<Exception> exceptions = new ArrayList<>();
    Iterator<Future<Object>> iterator = futureList.iterator();
    while(iterator.hasNext())
    {
      Future<Object> future = iterator.next();
      if(!future.isDone())
        future.cancel(true);
      try
      {
        future.get();
      }
      catch(InterruptedException | ExecutionException | CancellationException ex)
      {
        exceptions.add(ex);
      }
    }
    return exceptions;
  }
}
