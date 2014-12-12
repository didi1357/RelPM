/*
 * Created: 2014-09-09
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
package at.co.malli.relpm;

import at.co.malli.relpm.data.SettingsProvider;
import at.co.malli.lib.gui.ExceptionDisplayer;
import at.co.malli.relpm.gui.MainGUI;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.UIManager;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Dietmar Malli
 */
public class RelPM
{
  private static final Logger logger = LogManager.getLogger(RelPM.class.getName());
  /**
   * @param args the command line arguments
   */
  public static void main (String[] args)
  {
    //<editor-fold defaultstate="collapsed" desc=" Create config & log directory ">
    String userHome = System.getProperty("user.home");
    File relPm = new File(userHome + "/.RelPM");
    if(!relPm.exists())
    {
      boolean worked = relPm.mkdir();
      if(!worked)
      {
        ExceptionDisplayer.showErrorMessage(new Exception("Could not create directory " + relPm.getAbsolutePath() +
                                                          " to store user-settings and logs"));
        System.exit(-1);
      }
    }
    File userConfig = new File(relPm.getAbsolutePath()+"/RelPM-userconfig.xml"); //should be created...
    if(!userConfig.exists())
    {
      try
      {
        URL resource = RelPM.class.getResource("/at/co/malli/relpm/RelPM-defaults.xml");
        FileUtils.copyURLToFile(resource, userConfig);
      }
      catch (IOException ex)
      {
        ExceptionDisplayer.showErrorMessage(new Exception("Could not copy default config. Reason:\n" + 
                                                          ex.getClass().getName() + ": " + ex.getMessage()));
        System.exit(-1);
      }
    }    
    if(!userConfig.canWrite() || !userConfig.canRead())
    {
      ExceptionDisplayer.showErrorMessage(new Exception("Can not read or write " + userConfig.getAbsolutePath() + 
                                                        "to store user-settings"));
      System.exit(-1);
    }
    if(System.getProperty("os.name").toLowerCase().contains("win"))
    {
      Path relPmPath = Paths.get(relPm.toURI());
      try
      {
        Files.setAttribute(relPmPath, "dos:hidden", true);
      }
      catch (IOException ex)
      {
        ExceptionDisplayer.showErrorMessage(new Exception("Could not set " + relPm.getAbsolutePath() + " hidden. " +
                                                          "Reason:\n" + ex.getClass().getName() + ": " + 
                                                          ex.getMessage()));
      System.exit(-1);
      }
    }
    //</editor-fold>
    
    logger.trace("Environment setup sucessfull");
    
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code ">
    try
    {
      String wantedLookAndFeel = SettingsProvider.getInstance().getString("gui.lookAndFeel");
      UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
      boolean found=false;
      for(UIManager.LookAndFeelInfo info : installed)
      {
        if(info.getClassName().equals(wantedLookAndFeel))
          found=true;
      }
      if(found)
        UIManager.setLookAndFeel(wantedLookAndFeel);
      else
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (ClassNotFoundException ex)
    {
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
    catch (InstantiationException ex)
    {
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
    catch (IllegalAccessException ex)
    {
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
    catch (javax.swing.UnsupportedLookAndFeelException ex)
    {
      logger.error(ex.getMessage());
      ExceptionDisplayer.showErrorMessage(ex);
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc=" Add GUI start to awt EventQue ">
    java.awt.EventQueue.invokeLater(new Runnable()
    {
      public void run ()
      {
        new MainGUI().setVisible(true);
      }
    });
    //</editor-fold>
  }
  
  /**
   * This method will save the userconfig and shutdown the application.
   */
  public static void shutdown ()
  {
    logger.trace("Entering shutdown()");
    try
    {
      SettingsProvider.getInstance().writeUserconfig();
    }
    catch (ConfigurationException ex)
    {
      logger.error(ex);
      ExceptionDisplayer.showErrorMessage(ex);
    }
    logger.trace("Exiting application");
    System.exit(0);
  }
}
