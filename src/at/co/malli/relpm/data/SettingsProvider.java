/*
 * Created: 2014-11-23
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
package at.co.malli.relpm.data;

import at.co.malli.lib.gui.ExceptionDisplayer;
import java.net.URL;
import java.util.Iterator;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A small class implementing the singleton design pattern providing a unified interface to the applications settings.
 * @author Dietmar Malli
 */
public class SettingsProvider
{
  private static final Logger logger = LogManager.getLogger(SettingsProvider.class.getName());
  
  //Volatile to prevent VM from optimizing the Double-Checking of theInstance
  private static volatile SettingsProvider theInstance=null;
  /**
   * @return The only existing instance of this class.
   */
  public static SettingsProvider getInstance()
  {
    if(theInstance==null)
    {
      //If two threads would try to create a new instance...
      synchronized (SettingsProvider.class)
      {
        if (theInstance==null)
          theInstance = new SettingsProvider();
      }
    }
    
    return theInstance;
  }
  
  
  public static final URL defaultsFilename = SettingsProvider.class.getResource("/at/co/malli/relpm/RelPM-defaults.xml");
  public static final String userconfigFilename = System.getProperty("user.home")+"/.RelPM/RelPM-userconfig.xml";
  
  private CompositeConfiguration config = new CompositeConfiguration();
  private XMLConfiguration defaults;
  private XMLConfiguration user;

  /**
   * Sets up the object. This is only called one time when getInstance() is called the first time.
   */
  public SettingsProvider ()
  {
    try
    {
      defaults = new XMLConfiguration(defaultsFilename);
      user = new XMLConfiguration(userconfigFilename);
    }
    catch (ConfigurationException ex)
    {
      if(ex.getMessage().equals("Cannot locate configuration source " + userconfigFilename))
      {
        logger.trace(userconfigFilename + " not found... Creating default...");
        createDefaultConfig();
      }
      else
      {
        logger.error(ex.getMessage());
        ExceptionDisplayer.showErrorMessage(ex);
      }
    }
    config.addConfiguration(user, true);
    config.addConfiguration(defaults, false);
  }
  
  /**
   * Set up the default configuration in memory.
   */
  private void createDefaultConfig ()
  {
    user = new XMLConfiguration();
    
    Iterator<String> i = defaults.getKeys();
    while(i.hasNext())
    {
      String key = i.next();
      Object value = defaults.getProperty(key);
      user.setProperty(key,value);
    }
    try
    {
      user.save(userconfigFilename);
    }
    catch (ConfigurationException ex)
    {
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
  }
  
  /**
   * Writes the usermodified configuration into the user's homedirectory.
   * @throws ConfigurationException 
   */
  public void writeUserconfig () throws ConfigurationException
  {
    user.save(userconfigFilename);
  }
  
  /**
   * @param key String representing the name of the wanted setting from the xml structure. (For example "gui.lastfile")
   * @return The wanted value as an integer.
   */
  public int getInt (String key)
  {
    return config.getInt(key);
  }
  /**
   * @param key String representing the name of the wanted setting from the xml structure. (For example "gui.lastfile")
   * @return The wanted value as a long.
   */
  public long getLong (String key)
  {
    return config.getLong(key);
  }
  /**
   * @param key String representing the name of the wanted setting from the xml structure. (For example "gui.lastfile")
   * @return The wanted value as a String.
   */
  public String getString (String key)
  {
    return config.getString(key);
  }
  /**
   * @param key String representing the name of the wanted setting from the xml structure. (For example "gui.lastfile")
   * @return The wanted value as a boolean.
   */
  public boolean getBoolean (String key)
  {
    return config.getBoolean(key);
  }
  /**
   * Writes a new value into the usermodified configuration in memory.
   * @param key String representing the name of the wanted setting from the xml structure. (For example "gui.lastfile")
   * @param value The value to set. (Will usually be saved as Sring on the persistent storage.)
   */
  public void set (String key, Object value)
  {
    config.setProperty(key, value);
  }
}
