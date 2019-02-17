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
package at.co.malli.relpm.gui;

import at.co.malli.lib.gui.ExceptionDisplayer;
import at.co.malli.lib.gui.StatusBar;
import at.co.malli.relpm.RelPM;
import at.co.malli.relpm.data.ResourceUtils;
import at.co.malli.relpm.data.SettingsProvider;
import at.co.malli.relpm.data.audiofiles.TrackFile;
import at.co.malli.relpm.data.audiofiles.TrackFileList;
import at.co.malli.relpm.data.playlist.PlaylistFileReadWorker;
import at.co.malli.lib.status.StatusListenerInterface;
import at.co.malli.lib.status.StatusNotifier;
import at.co.malli.relpm.data.playlist.PlaylistFileWriteWorker;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Dietmar Malli
 */
public class MainGUI extends javax.swing.JFrame
{
  private static final Logger logger = LogManager.getLogger(MainGUI.class.getName());
  
  private static final int CLOSE_CHECK_NOTHING_OPENED = -1;
  private static final int CLOSE_CHECK_USER_ABORT = -2;
  private static final int CLOSE_CHECK_USER_NOT_SAVING = -3;
  private static final int CLOSE_CHECK_SWINGWORKER_CLOSES = -4;
  private static final int CLOSE_CHECK_CLOSABLE_NOW = -5;
  
  private File lastFile = new File(SettingsProvider.getInstance().getString("gui.lastFile"));
  private final TrackFileListModel listModel;
  private final SongCountBar songCountBar;
  private File lastSave = null;
  private boolean alreadySaved = false;
  private boolean cameFromClose = false;
  private boolean cameFromShutdown = false;
  
  public MainGUI ()
  {
    //<editor-fold defaultstate="collapsed" desc=" Icon setting code... ">
    ArrayList<Image> imageList = new ArrayList<>();
    URL resource = this.getClass().getResource("/at/co/malli/relpm/gui/icons/RelPM/RelPM_64x64.png");
    imageList.add(new ImageIcon(resource).getImage());
    resource = this.getClass().getResource("/at/co/malli/relpm/gui/icons/RelPM/RelPM_48x48.png");
    imageList.add(new ImageIcon(resource).getImage());
    resource = this.getClass().getResource("/at/co/malli/relpm/gui/icons/RelPM/RelPM_32x32.png");
    imageList.add(new ImageIcon(resource).getImage());
    resource = this.getClass().getResource("/at/co/malli/relpm/gui/icons/RelPM/RelPM_16x16.png");
    imageList.add(new ImageIcon(resource).getImage());
    setIconImages(imageList);
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc=" GUI position and size stuff which needs to be done before pack() ">
    Dimension dMin = Toolkit.getDefaultToolkit().getScreenSize();
    dMin.setSize(500, 420); //420 because of buttons...
    this.setMinimumSize(dMin);
    
    int maximized = SettingsProvider.getInstance().getInt("gui.maximized");
    if(maximized!=JFrame.MAXIMIZED_BOTH)
    {
      int lastWidth = SettingsProvider.getInstance().getInt("gui.lastWidth");
      int lastHeight = SettingsProvider.getInstance().getInt("gui.lastHeight");
      if(lastWidth<0 || lastHeight<0)
      {
        Dimension dInit = Toolkit.getDefaultToolkit().getScreenSize();
        dInit.setSize(dInit.getWidth()*0.8, dInit.getHeight()*0.8);
        this.setPreferredSize(dInit);
      }
      else
        this.setPreferredSize(new Dimension(lastWidth,lastHeight));
    }
    else
      this.setExtendedState(maximized);
    //</editor-fold>
    
    listModel = new TrackFileListModel();
    songCountBar = new SongCountBar(listModel);
    initComponents(); //Run the generated code (including pack())
    
    //<editor-fold defaultstate="collapsed" desc=" GUI-component dependent or pack()-dependent stuff (position and size) ">
    if(maximized!=JFrame.MAXIMIZED_BOTH)
    {
      int posX = SettingsProvider.getInstance().getInt("gui.posX");
      int posY = SettingsProvider.getInstance().getInt("gui.posY");
      if(posX<0 || posY<0)
        this.setLocationRelativeTo(null);
      else
        this.setLocation(posX, posY);
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc=" Maybe add a sound playing engine in a future release ">
    getContentPane().remove(jPanEast);
    pack();
    //</editor-fold>
    
    StatusNotifier.getInstance().addStatusListener((StatusListenerInterface) jTFstatus);
    jPanCenter.setTransferHandler(new CenterTransferHandler(this));
    jScroll.setVisible(false);
    jList.setVisible(false);
    listModel.addListDataListener(songCountBar);
    listModel.addListDataListener(new AlreadySavedListener(this));
    jList.setModel(listModel);
    jList.setTransferHandler(new ListTransferHandler(jList));
    updateButtonStates();
    
    logger.trace("initialized");
  }

  /**
   * Opens a playlist.
   */
  private void openPlaylist()
  {
    JFileChooser chooser = new JFileChooser();
    if(lastFile!=null)
      chooser.setSelectedFile(lastFile);
    FileNameExtensionFilter m3u = new FileNameExtensionFilter("M3U-Playlists (.m3u, .m3u8)", "m3u", "m3u8");
    chooser.setFileFilter(m3u);
    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    int returnVal = chooser.showOpenDialog(this);
    if(returnVal == JFileChooser.APPROVE_OPTION)
    {
      lastFile = chooser.getSelectedFile();
      lastSave = null; //Don't write a newly opened file... Let the user decide the save location...
    }
    else
    {
      StatusNotifier.getInstance().fireStatusEvent(this,"Open dialog canceled by user!");
      return;
    }
    SettingsProvider.getInstance().set("gui.lastFile", lastFile.getAbsoluteFile());
    PlaylistFileReadWorker readWorker = new PlaylistFileReadWorker(this, lastFile);
    readWorker.execute();
  }
  
  /**
   * This method is intended to be called by the done() method of the swing worker which parses playlist files.
   * @param output The parsed data as TrackFileList.
   */
  public void handleReadWorkerOutput (TrackFileList output)
  {
    //<editor-fold defaultstate="collapsed" desc="Maybe implement multi-tabbing here later some time by adding to a tabbed pane in the center..">
//    TrackFileListModel listModel = new TrackFileListModel(output);
//    JList<TrackFile> jList = new JList<>(listModel);
//    JScrollPane jScroll = new JScrollPane(jList);
//    jList.setDragEnabled(true);
//    jPanCenter.add(jScroll, BorderLayout.CENTER);
//    pack();
    //</editor-fold>
    
    listModel.addInitial(output);
    jScroll.setVisible(true);
    jList.setVisible(true);
    updateButtonStates();
    alreadySaved = true;
  }
  
  /**
   * This method creates a new empty list.
   */
  private void createList ()
  {
    JFileChooser chooser = new JFileChooser();
    if(lastSave!=null)
      chooser.setSelectedFile(lastSave);
    else
    {
      if(lastFile!=null)
        chooser.setSelectedFile(lastFile);
    }
    FileNameExtensionFilter m3u = new FileNameExtensionFilter("M3U-Playlists (.m3u, .m3u8)", "m3u", "m3u8");
    chooser.setFileFilter(m3u);
    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    int returnVal = chooser.showSaveDialog(this);
    if(returnVal == JFileChooser.APPROVE_OPTION)
    {
      try
      {
        chooser.getSelectedFile().createNewFile();
      }
      catch (IOException e)
      {
        StatusNotifier.getInstance().fireStatusEvent(this, "Could not create empty playlist file in desired location!");
        return;
      }
      lastFile = chooser.getSelectedFile();
      lastSave = chooser.getSelectedFile();
    }
    else
    {
      StatusNotifier.getInstance().fireStatusEvent(this, "Create dialog canceled by user! Location is currently needed to calculate relative paths!");
      return;
    }
    SettingsProvider.getInstance().set("gui.lastFile", lastSave.getAbsoluteFile());
    
    PlaylistFileReadWorker readWorker = new PlaylistFileReadWorker(this, lastFile);
    readWorker.execute();
  }
  
  /**
   * Inserts one or more songs at the current selected index in the jList if possible...
   */
  private void insertSong ()
  {
    if(jList.isSelectionEmpty())
    {
      StatusNotifier.getInstance().fireStatusEvent(this, "No position in list selected...");
      return;
    }
    JFileChooser chooser = new JFileChooser();
    if(lastFile != null)
      chooser.setSelectedFile(lastFile);
    FileNameExtensionFilter mp3 = new FileNameExtensionFilter("MP3-Files (.mp3)", "mp3");
    FileNameExtensionFilter audio = new FileNameExtensionFilter("Audio-Files (.mp3, .wav, .ogg)", "mp3", "wav", "ogg");
    chooser.setFileFilter(mp3);
    chooser.addChoosableFileFilter(audio);
    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    chooser.setMultiSelectionEnabled(true);
    int returnVal = chooser.showOpenDialog(null);
    File[] selectedFiles;
    if(returnVal == JFileChooser.APPROVE_OPTION)
      selectedFiles = chooser.getSelectedFiles();
    else
    {
      StatusNotifier.getInstance().fireStatusEvent(this,"File selection canceled by user!");
      return;
    }
    SettingsProvider.getInstance().set("gui.lastFile", selectedFiles[0]);
    try
    {
      ArrayList<TrackFile> elements = new ArrayList<>();
      for(File item : selectedFiles)
        elements.add(new TrackFile(listModel.getPlaylist(),item.getAbsolutePath()));
      listModel.add(jList.getSelectedIndex(),elements);
    }
    catch(FileNotFoundException | ResourceUtils.PathResolutionException e)
    {
      logger.error(e.getMessage());
      ExceptionDisplayer.showErrorMessage(e);
    }
  }
  
  /**
   * Removes the currently selected song(s). If no song is selected it will fire a StatusEvent notifying the user.
   */
  private void removeSong ()
  {
    if(jList.isSelectionEmpty())
    {
      StatusNotifier.getInstance().fireStatusEvent(this, "No File from list selected...");
      return;
    }
    int[] selectedIndices = jList.getSelectedIndices();
    ArrayList<Integer> indices = new ArrayList<>();
    for(int val : selectedIndices)
      indices.add(val);
    Collections.sort(indices);
    Collections.reverse(indices);
    for(int val : indices)
      listModel.remove(val);
  }
  
  /**
   * Saves the playlist to lastSave.
   * @param saveAs Will show a JFileChooser Dialog to request a new save location.
   * @return 0 If everything worked or -1 if the user aborted the procedure.
   */
  private int savePlaylist (boolean saveAs)
  {
    if(saveAs || lastSave==null) //User didn't provide a location yet or call to saveAs will show the dialogue
    {
      JFileChooser chooser = new JFileChooser();
      if(lastSave!=null)
        chooser.setSelectedFile(lastSave);
      else
      {
        if(lastFile!=null)
          chooser.setSelectedFile(lastFile);
      }
      FileNameExtensionFilter m3u = new FileNameExtensionFilter("M3U-Playlists (.m3u, .m3u8)", "m3u", "m3u8");
      chooser.setFileFilter(m3u);
      chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
      int returnVal = chooser.showSaveDialog(this);
      if(returnVal == JFileChooser.APPROVE_OPTION)
      {
        lastFile = chooser.getSelectedFile();
        lastSave = chooser.getSelectedFile();
      }
      else
      {
        StatusNotifier.getInstance().fireStatusEvent(this,"Save dialog canceled by user!");
        return -1;
      }
      SettingsProvider.getInstance().set("gui.lastFile", lastSave.getAbsoluteFile());
    }
    PlaylistFileWriteWorker writeWorker = new PlaylistFileWriteWorker(this, listModel.getList(), lastSave);
    writeWorker.execute();
    return 0;
  }
  
  /**
   * This method will be called by the PlaylistFileWriteWorker... It will inform the GUI wheter writing worked or not...
   * @param b True if everything worked fine.
   */
  public void handleWriteWorkerOutput (boolean b)
  {
    synchronized(this)
    {
      alreadySaved = b;
    }
    if(cameFromClose)
    {
      closeListFinally();
      cameFromClose=false;
    }
    if(cameFromShutdown)
      shutdown();
  }
  
  /**
   * This method is called by UI-elements. It does the interaction with the closeListCheck() and closeListFinally()
   * methods.
   */
  private void closeList ()
  {
    cameFromClose = true;
    int check = closeListCheck();
    switch(check)
    {
      case CLOSE_CHECK_NOTHING_OPENED:
        cameFromClose = false;
      break;
      case CLOSE_CHECK_USER_NOT_SAVING:
      case CLOSE_CHECK_CLOSABLE_NOW:
        closeListFinally();
        cameFromClose = false;
      break;
      case CLOSE_CHECK_USER_ABORT:
        cameFromClose = false;
      break;
      case CLOSE_CHECK_SWINGWORKER_CLOSES:
      break;
    }
  }
  
  /**
   * Checks wheter the currently opened list is already saved or not and in turn wheter the user actually wants to save
   * the changes or not.
   * @return Integer values defined as constants in this class:<br>
   *         CLOSE_CHECK_NOTHING_OPENED if no list is currently opened.<br>
   *         CLOSE_CHECK_USER_ABORT if the user aborted closing the list.<br>
   *         CLOSE_CHECK_USER_NOT_SAVING if the user doesn't want to save the changes.<br>
   *         CLOSE_CHECK_SWINGWORKER_CLOSES if a PlaylistFileWriteWorker was called which will in turn call the
   *         closeListFinally() method.<br>
   *         CLOSE_CHECK_CLOSABLE_NOW if the calling method can safely call closeListFinally() on it's own.
   */
  private int closeListCheck ()
  {
    if(!jList.isVisible()) //no list is currently opened..
      return CLOSE_CHECK_NOTHING_OPENED;
    if(!alreadySaved)
    {
      int user = JOptionPane.showConfirmDialog(null, "There are unsaved changes. Would you like to save them now?");
      switch(user)
      {
        case JOptionPane.CLOSED_OPTION:
        case JOptionPane.CANCEL_OPTION:
          return CLOSE_CHECK_USER_ABORT;
        case JOptionPane.NO_OPTION:
          alreadySaved = true;
          return CLOSE_CHECK_USER_NOT_SAVING;
        case JOptionPane.YES_OPTION:
        default:
          user = savePlaylist(false);
          if(user != 0)
            return CLOSE_CHECK_USER_ABORT;
          else
            return CLOSE_CHECK_SWINGWORKER_CLOSES;
      }
    }
    return CLOSE_CHECK_CLOSABLE_NOW;
  }
  
  /**
   * This method will finally close the list. It is called by the PlaylistFileWriteWorker or directly.
   */
  private void closeListFinally ()
  {
    jList.setVisible(false);
    jScroll.setVisible(false);
    updateButtonStates();
  }
  
  /**
   * This method enables and disables buttons depending on wheter a list is currently opened or not.
   */
  private void updateButtonStates ()
  {
    if(jList.isVisible())
    {
      jButCreate.setEnabled(false);
      jButOpen.setEnabled(false);
      jButSave.setEnabled(true);
      jButCloseList.setEnabled(true);
      jButInsertSong.setEnabled(true);
      jButRemoveSong.setEnabled(true);
      jButPlay.setEnabled(true);
      jButPause.setEnabled(true);
      jButStop.setEnabled(true);
      jButNext.setEnabled(true);
      jButPrevious.setEnabled(true);
      jButRandom.setEnabled(true);
      jMenuItemCreate.setEnabled(false);
      jMenuItemOpen.setEnabled(false);
      jMenuItemSave.setEnabled(true);
      jMenuItemSaveAs.setEnabled(true);
      jMenuItemCloseList.setEnabled(true);
      jMenuItemInsertSong.setEnabled(true);
      jMenuItemRemoveSong.setEnabled(true);
    }
    else
    {
      jButCreate.setEnabled(true);
      jButOpen.setEnabled(true);
      jButSave.setEnabled(false);
      jButCloseList.setEnabled(false);
      jButInsertSong.setEnabled(false);
      jButRemoveSong.setEnabled(false);
      jButPlay.setEnabled(false);
      jButPause.setEnabled(false);
      jButStop.setEnabled(false);
      jButNext.setEnabled(false);
      jButPrevious.setEnabled(false);
      jButRandom.setEnabled(false);
      jMenuItemCreate.setEnabled(true);
      jMenuItemOpen.setEnabled(true);
      jMenuItemSave.setEnabled(false);
      jMenuItemSaveAs.setEnabled(false);
      jMenuItemCloseList.setEnabled(false);
      jMenuItemInsertSong.setEnabled(false);
      jMenuItemRemoveSong.setEnabled(false);
    }
  }
  
  /**
   * Shuts down all GUI relevant parts of the programm with shutdown routines and calls the application shutdown
   * routine.
   */
  private void shutdown ()
  {
    logger.trace("Entering method shutdown()");
    
    if(cameFromShutdown==false)
    {
      cameFromShutdown = true;
      int check = closeListCheck();
      switch(check)
      {
        case CLOSE_CHECK_NOTHING_OPENED:
        case CLOSE_CHECK_USER_NOT_SAVING:
        break;
        case CLOSE_CHECK_CLOSABLE_NOW:
          closeListFinally();
        break;
        case CLOSE_CHECK_USER_ABORT:
          cameFromShutdown=false;
          return;
        case CLOSE_CHECK_SWINGWORKER_CLOSES:
          return;
      }
    }
    
    //<editor-fold defaultstate="collapsed" desc=" Save gui state ">
    if(this.getExtendedState()!=JFrame.MAXIMIZED_BOTH)
    {
      SettingsProvider.getInstance().set("gui.lastWidth", this.getWidth());
      SettingsProvider.getInstance().set("gui.lastHeight", this.getHeight());
      SettingsProvider.getInstance().set("gui.posX", this.getLocation().x);
      SettingsProvider.getInstance().set("gui.posY", this.getLocation().y);
      SettingsProvider.getInstance().set("gui.maximized", this.getExtendedState());
    }
    else
      SettingsProvider.getInstance().set("gui.maximized", this.getExtendedState());
    //</editor-fold>
    
    StatusBar statusBarInstance = (StatusBar) jTFstatus;
    statusBarInstance.shutdown();
    ArrayList<Exception> exceptions = statusBarInstance.getExceptions();
    for(Exception ex : exceptions)
    {
      logger.error("Exception from StatusBar:" + ex.getMessage());
    }
    RelPM.shutdown();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
   * content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    jPanCenter = new javax.swing.JPanel();
    jScroll = new javax.swing.JScrollPane();
    jList = new javax.swing.JList();
    jPanStatus = new javax.swing.JPanel();
    jPanStatusEast = new javax.swing.JPanel();
    jLabSongs = new javax.swing.JLabel();
    jTFsongCount = songCountBar;
    jPanStatusCenter = new javax.swing.JPanel();
    jTFstatus = new StatusBar();
    jPanNorth = new javax.swing.JPanel();
    jButCreate = new javax.swing.JButton();
    jButOpen = new javax.swing.JButton();
    jButSave = new javax.swing.JButton();
    jButCloseList = new javax.swing.JButton();
    jButInsertSong = new javax.swing.JButton();
    jButRemoveSong = new javax.swing.JButton();
    jPanEast = new javax.swing.JPanel();
    jButPlay = new javax.swing.JButton();
    jButPause = new javax.swing.JButton();
    jButStop = new javax.swing.JButton();
    jButNext = new javax.swing.JButton();
    jButPrevious = new javax.swing.JButton();
    jButRandom = new javax.swing.JButton();
    jMenuBar = new javax.swing.JMenuBar();
    jMenuFile = new javax.swing.JMenu();
    jMenuItemCreate = new javax.swing.JMenuItem();
    jMenuItemOpen = new javax.swing.JMenuItem();
    jMenuItemSave = new javax.swing.JMenuItem();
    jMenuItemSaveAs = new javax.swing.JMenuItem();
    jMenuItemCloseList = new javax.swing.JMenuItem();
    jMenuItemCloseApp = new javax.swing.JMenuItem();
    jMenuEdit = new javax.swing.JMenu();
    jMenuItemInsertSong = new javax.swing.JMenuItem();
    jMenuItemRemoveSong = new javax.swing.JMenuItem();
    jMenuOptions = new javax.swing.JMenu();
    jMenuItemSettings = new javax.swing.JMenuItem();
    jMenuItemAbout = new javax.swing.JMenuItem();

    setTitle("Relative Playlist Manager");
    addComponentListener(new java.awt.event.ComponentAdapter()
    {
      public void componentHidden(java.awt.event.ComponentEvent evt)
      {
        componentHiddenHandler(evt);
      }
    });

    jPanCenter.setLayout(new java.awt.BorderLayout());

    jScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    jList.setDragEnabled(true);
    jList.setDropMode(javax.swing.DropMode.INSERT);
    jScroll.setViewportView(jList);

    jPanCenter.add(jScroll, java.awt.BorderLayout.CENTER);

    getContentPane().add(jPanCenter, java.awt.BorderLayout.CENTER);

    jPanStatus.setLayout(new java.awt.BorderLayout());

    jPanStatusEast.setLayout(new java.awt.BorderLayout());

    jLabSongs.setText("Songs:");
    jPanStatusEast.add(jLabSongs, java.awt.BorderLayout.CENTER);

    jTFsongCount.setEditable(false);
    jTFsongCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    jTFsongCount.setToolTipText("Songcount");
    jTFsongCount.setFocusable(false);
    jTFsongCount.setPreferredSize(new java.awt.Dimension(80, 27));
    jPanStatusEast.add(jTFsongCount, java.awt.BorderLayout.EAST);

    jPanStatus.add(jPanStatusEast, java.awt.BorderLayout.EAST);

    jPanStatusCenter.setLayout(new java.awt.BorderLayout());

    jTFstatus.setEditable(false);
    jTFstatus.setToolTipText("Status Bar");
    jTFstatus.setFocusable(false);
    jPanStatusCenter.add(jTFstatus, java.awt.BorderLayout.CENTER);

    jPanStatus.add(jPanStatusCenter, java.awt.BorderLayout.CENTER);

    getContentPane().add(jPanStatus, java.awt.BorderLayout.SOUTH);

    jPanNorth.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    jButCreate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/buttons/newList.png"))); // NOI18N
    jButCreate.setToolTipText("Create a new empty list");
    jButCreate.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButCreateActionPerformed(evt);
      }
    });
    jPanNorth.add(jButCreate);

    jButOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/buttons/open.png"))); // NOI18N
    jButOpen.setToolTipText("Open");
    jButOpen.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButOpenActionPerformed(evt);
      }
    });
    jPanNorth.add(jButOpen);

    jButSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/buttons/save.png"))); // NOI18N
    jButSave.setToolTipText("Save");
    jButSave.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButSaveActionPerformed(evt);
      }
    });
    jPanNorth.add(jButSave);

    jButCloseList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/buttons/closeList.png"))); // NOI18N
    jButCloseList.setToolTipText("Close list");
    jButCloseList.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButCloseListActionPerformed(evt);
      }
    });
    jPanNorth.add(jButCloseList);

    jButInsertSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/buttons/insert.png"))); // NOI18N
    jButInsertSong.setToolTipText("Insert a new song at the selected position");
    jButInsertSong.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButInsertSongActionPerformed(evt);
      }
    });
    jPanNorth.add(jButInsertSong);

    jButRemoveSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/buttons/remove.png"))); // NOI18N
    jButRemoveSong.setToolTipText("Remove the selected song(s)");
    jButRemoveSong.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButRemoveSongActionPerformed(evt);
      }
    });
    jPanNorth.add(jButRemoveSong);

    getContentPane().add(jPanNorth, java.awt.BorderLayout.NORTH);

    jPanEast.setToolTipText("Sound Control");
    jPanEast.setLayout(new javax.swing.BoxLayout(jPanEast, javax.swing.BoxLayout.PAGE_AXIS));

    jButPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/IconEdenGlossyButton/buttons/play.png"))); // NOI18N
    jButPlay.setToolTipText("Play");
    jButPlay.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButPlayActionPerformed(evt);
      }
    });
    jPanEast.add(jButPlay);

    jButPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/IconEdenGlossyButton/buttons/pause.png"))); // NOI18N
    jButPause.setToolTipText("Pause");
    jButPause.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButPauseActionPerformed(evt);
      }
    });
    jPanEast.add(jButPause);

    jButStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/IconEdenGlossyButton/buttons/stop.png"))); // NOI18N
    jButStop.setToolTipText("Stop");
    jButStop.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButStopActionPerformed(evt);
      }
    });
    jPanEast.add(jButStop);

    jButNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/IconEdenGlossyButton/buttons/next.png"))); // NOI18N
    jButNext.setToolTipText("Next");
    jButNext.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButNextActionPerformed(evt);
      }
    });
    jPanEast.add(jButNext);

    jButPrevious.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/IconEdenGlossyButton/buttons/previous.png"))); // NOI18N
    jButPrevious.setToolTipText("Previous");
    jButPrevious.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButPreviousActionPerformed(evt);
      }
    });
    jPanEast.add(jButPrevious);

    jButRandom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/IconEdenGlossyButton/buttons/random.png"))); // NOI18N
    jButRandom.setToolTipText("Play random (This will not shuffle the actual playlist.)");
    jButRandom.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButRandomActionPerformed(evt);
      }
    });
    jPanEast.add(jButRandom);

    getContentPane().add(jPanEast, java.awt.BorderLayout.EAST);

    jMenuFile.setText("File");

    jMenuItemCreate.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemCreate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/newList.png"))); // NOI18N
    jMenuItemCreate.setText("New");
    jMenuItemCreate.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemCreateActionPerformed(evt);
      }
    });
    jMenuFile.add(jMenuItemCreate);

    jMenuItemOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/open.png"))); // NOI18N
    jMenuItemOpen.setText("Open");
    jMenuItemOpen.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemOpenActionPerformed(evt);
      }
    });
    jMenuFile.add(jMenuItemOpen);

    jMenuItemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/save.png"))); // NOI18N
    jMenuItemSave.setText("Save");
    jMenuItemSave.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemSaveActionPerformed(evt);
      }
    });
    jMenuFile.add(jMenuItemSave);

    jMenuItemSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/save.png"))); // NOI18N
    jMenuItemSaveAs.setText("Save as");
    jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemSaveAsActionPerformed(evt);
      }
    });
    jMenuFile.add(jMenuItemSaveAs);

    jMenuItemCloseList.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemCloseList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/closeList.png"))); // NOI18N
    jMenuItemCloseList.setText("Close current list");
    jMenuItemCloseList.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemCloseListActionPerformed(evt);
      }
    });
    jMenuFile.add(jMenuItemCloseList);

    jMenuItemCloseApp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
    jMenuItemCloseApp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/Any/close.png"))); // NOI18N
    jMenuItemCloseApp.setText("Close application");
    jMenuItemCloseApp.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemCloseAppActionPerformed(evt);
      }
    });
    jMenuFile.add(jMenuItemCloseApp);

    jMenuBar.add(jMenuFile);

    jMenuEdit.setText("Edit");

    jMenuItemInsertSong.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
    jMenuItemInsertSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/insert.png"))); // NOI18N
    jMenuItemInsertSong.setText("Insert song(s)");
    jMenuItemInsertSong.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemInsertSongActionPerformed(evt);
      }
    });
    jMenuEdit.add(jMenuItemInsertSong);

    jMenuItemRemoveSong.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
    jMenuItemRemoveSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/remove.png"))); // NOI18N
    jMenuItemRemoveSong.setText("Remove song(s)");
    jMenuItemRemoveSong.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemRemoveSongActionPerformed(evt);
      }
    });
    jMenuEdit.add(jMenuItemRemoveSong);

    jMenuBar.add(jMenuEdit);

    jMenuOptions.setText("Options");

    jMenuItemSettings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/settings.png"))); // NOI18N
    jMenuItemSettings.setText("Settings");
    jMenuItemSettings.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemSettingsActionPerformed(evt);
      }
    });
    jMenuOptions.add(jMenuItemSettings);

    jMenuItemAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/at/co/malli/relpm/gui/icons/MeBazeBunchOfBluish/menuItems/about.png"))); // NOI18N
    jMenuItemAbout.setText("About");
    jMenuItemAbout.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jMenuItemAboutActionPerformed(evt);
      }
    });
    jMenuOptions.add(jMenuItemAbout);

    jMenuBar.add(jMenuOptions);

    setJMenuBar(jMenuBar);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemOpenActionPerformed
  {//GEN-HEADEREND:event_jMenuItemOpenActionPerformed
    openPlaylist();
  }//GEN-LAST:event_jMenuItemOpenActionPerformed
  
  private void componentHiddenHandler(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_componentHiddenHandler
  {//GEN-HEADEREND:event_componentHiddenHandler
    shutdown();
  }//GEN-LAST:event_componentHiddenHandler

  private void jMenuItemCloseAppActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemCloseAppActionPerformed
  {//GEN-HEADEREND:event_jMenuItemCloseAppActionPerformed
    shutdown();
  }//GEN-LAST:event_jMenuItemCloseAppActionPerformed

  private void jMenuItemCloseListActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemCloseListActionPerformed
  {//GEN-HEADEREND:event_jMenuItemCloseListActionPerformed
    closeList();
  }//GEN-LAST:event_jMenuItemCloseListActionPerformed

  private void jMenuItemInsertSongActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemInsertSongActionPerformed
  {//GEN-HEADEREND:event_jMenuItemInsertSongActionPerformed
    insertSong();
  }//GEN-LAST:event_jMenuItemInsertSongActionPerformed

  private void jMenuItemRemoveSongActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemRemoveSongActionPerformed
  {//GEN-HEADEREND:event_jMenuItemRemoveSongActionPerformed
    removeSong();
  }//GEN-LAST:event_jMenuItemRemoveSongActionPerformed
  
  private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemAboutActionPerformed
  {//GEN-HEADEREND:event_jMenuItemAboutActionPerformed
    new AboutDialog(this, true).setVisible(true); //Modal dialog.. no need to create an instance an do something..
  }//GEN-LAST:event_jMenuItemAboutActionPerformed

  private void jMenuItemSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemSettingsActionPerformed
  {//GEN-HEADEREND:event_jMenuItemSettingsActionPerformed
    new SettingsDialog(this, true).setVisible(true); //Modal dialog will set values during press OK...
  }//GEN-LAST:event_jMenuItemSettingsActionPerformed

  private void jButRemoveSongActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButRemoveSongActionPerformed
  {//GEN-HEADEREND:event_jButRemoveSongActionPerformed
    removeSong();
  }//GEN-LAST:event_jButRemoveSongActionPerformed

  private void jButOpenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButOpenActionPerformed
  {//GEN-HEADEREND:event_jButOpenActionPerformed
    openPlaylist();
  }//GEN-LAST:event_jButOpenActionPerformed

  private void jButCloseListActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButCloseListActionPerformed
  {//GEN-HEADEREND:event_jButCloseListActionPerformed
    closeList();
  }//GEN-LAST:event_jButCloseListActionPerformed

  private void jButSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButSaveActionPerformed
  {//GEN-HEADEREND:event_jButSaveActionPerformed
    savePlaylist(false);
  }//GEN-LAST:event_jButSaveActionPerformed

  private void jButPlayActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButPlayActionPerformed
  {//GEN-HEADEREND:event_jButPlayActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jButPlayActionPerformed

  private void jButPauseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButPauseActionPerformed
  {//GEN-HEADEREND:event_jButPauseActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jButPauseActionPerformed

  private void jButStopActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButStopActionPerformed
  {//GEN-HEADEREND:event_jButStopActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jButStopActionPerformed

  private void jButRandomActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButRandomActionPerformed
  {//GEN-HEADEREND:event_jButRandomActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jButRandomActionPerformed

  private void jButNextActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButNextActionPerformed
  {//GEN-HEADEREND:event_jButNextActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jButNextActionPerformed

  private void jButPreviousActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButPreviousActionPerformed
  {//GEN-HEADEREND:event_jButPreviousActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_jButPreviousActionPerformed

  private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemSaveActionPerformed
  {//GEN-HEADEREND:event_jMenuItemSaveActionPerformed
    savePlaylist(false);
  }//GEN-LAST:event_jMenuItemSaveActionPerformed

  private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemSaveAsActionPerformed
  {//GEN-HEADEREND:event_jMenuItemSaveAsActionPerformed
    savePlaylist(true);
  }//GEN-LAST:event_jMenuItemSaveAsActionPerformed

  private void jButInsertSongActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButInsertSongActionPerformed
  {//GEN-HEADEREND:event_jButInsertSongActionPerformed
    insertSong();
  }//GEN-LAST:event_jButInsertSongActionPerformed

  private void jButCreateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButCreateActionPerformed
  {//GEN-HEADEREND:event_jButCreateActionPerformed
    createList();
  }//GEN-LAST:event_jButCreateActionPerformed

  private void jMenuItemCreateActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemCreateActionPerformed
  {//GEN-HEADEREND:event_jMenuItemCreateActionPerformed
    createList();
  }//GEN-LAST:event_jMenuItemCreateActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButCloseList;
  private javax.swing.JButton jButCreate;
  private javax.swing.JButton jButInsertSong;
  private javax.swing.JButton jButNext;
  private javax.swing.JButton jButOpen;
  private javax.swing.JButton jButPause;
  private javax.swing.JButton jButPlay;
  private javax.swing.JButton jButPrevious;
  private javax.swing.JButton jButRandom;
  private javax.swing.JButton jButRemoveSong;
  private javax.swing.JButton jButSave;
  private javax.swing.JButton jButStop;
  private javax.swing.JLabel jLabSongs;
  private javax.swing.JList jList;
  private javax.swing.JMenuBar jMenuBar;
  private javax.swing.JMenu jMenuEdit;
  private javax.swing.JMenu jMenuFile;
  private javax.swing.JMenuItem jMenuItemAbout;
  private javax.swing.JMenuItem jMenuItemCloseApp;
  private javax.swing.JMenuItem jMenuItemCloseList;
  private javax.swing.JMenuItem jMenuItemCreate;
  private javax.swing.JMenuItem jMenuItemInsertSong;
  private javax.swing.JMenuItem jMenuItemOpen;
  private javax.swing.JMenuItem jMenuItemRemoveSong;
  private javax.swing.JMenuItem jMenuItemSave;
  private javax.swing.JMenuItem jMenuItemSaveAs;
  private javax.swing.JMenuItem jMenuItemSettings;
  private javax.swing.JMenu jMenuOptions;
  private javax.swing.JPanel jPanCenter;
  private javax.swing.JPanel jPanEast;
  private javax.swing.JPanel jPanNorth;
  private javax.swing.JPanel jPanStatus;
  private javax.swing.JPanel jPanStatusCenter;
  private javax.swing.JPanel jPanStatusEast;
  private javax.swing.JScrollPane jScroll;
  private javax.swing.JTextField jTFsongCount;
  private javax.swing.JTextField jTFstatus;
  // End of variables declaration//GEN-END:variables

}
