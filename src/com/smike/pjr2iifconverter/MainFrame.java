package com.smike.pjr2iifconverter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

enum SettingsKey {
  DELETE_PJRS_ON_CONVERT("delete_pjrs_on_convert"),
  IIF_OUTPUT_FILE("iif_output_file"),
  ACCOUNT_ID_MAP_FILE("account_id_map_file"),
  PJR_LOCATION("pjr_location");

  private String key;
  private SettingsKey(String key) {
    this.key = key;
  }

  public String getKey() { return key; }
  public String toString() { return getKey(); }

  public static SettingsKey getEnum(String key) {
    for (SettingsKey settingsKey : SettingsKey.values()) {
      if (settingsKey.getKey().equals(key)) {
        return settingsKey;
      }
    }
    return null;
  }
}

public class MainFrame extends JFrame {
  private static Logger logger = Logger.getLogger(MainFrame.class.getName());

  private static final String CONFIG_LOCATION = "pjr2IifConverter.conf";

  private static final FilenameFilter PJR_FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(".xml") && name.startsWith("PJR");
    }
  };

  private Properties properties;

  private File accountIdMapFile;
  private List<File> pjrFiles = new LinkedList<File>();
  private File iifFile;

  private JTextField accountIdMapTextField;
  private JTextField iifFileTextField;
  private JList pjrFilesList;
  private JCheckBox deletePjrFilesCheckBox;

  public MainFrame() {
    super("PJR to IIF Converter");

    properties = new Properties();
    loadConfig(properties, CONFIG_LOCATION);

    JPanel mainPanel = new JPanel(); {
      BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
      mainPanel.setLayout(boxLayout);

      accountIdMapTextField = new JTextField();
      accountIdMapTextField.setEditable(false);
      JButton accountIdMapButton = new JButton("Choose Account ID Map File...");
      accountIdMapButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          File startDir = null;
          if (accountIdMapFile != null) {
            startDir = accountIdMapFile.getParentFile();
          }
          JFileChooser jFileChooser = new JFileChooser(startDir);
          jFileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
          int returnValue = jFileChooser.showOpenDialog(MainFrame.this);
          if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser.getSelectedFile();
            setAccountIdMapFile(file);
          }
        }
      });
      JPanel accountIdMapPanel = new JPanel(new GridBagLayout()); {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1;
        accountIdMapPanel.add(accountIdMapTextField, gridBagConstraints);
        gridBagConstraints.weightx = 0;
        accountIdMapPanel.add(accountIdMapButton, gridBagConstraints);
      }

      iifFileTextField = new JTextField();
      iifFileTextField.setEditable(false);
      JButton iifButton = new JButton("Choose the output IIF file...");
      iifButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          File startDir = null;
          if (iifFile != null) {
            startDir = iifFile.getParentFile();
          }
          JFileChooser jFileChooser = new JFileChooser(startDir);
          jFileChooser.setFileFilter(new FileNameExtensionFilter("IIF Files", "iif"));
          int returnValue = jFileChooser.showSaveDialog(MainFrame.this);
          if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser.getSelectedFile();
            setIifFile(file);
          }
        }
      });
      JPanel iifPanel = new JPanel(new GridBagLayout()); {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1;
        iifPanel.add(iifFileTextField, gridBagConstraints);
        gridBagConstraints.weightx = 0;
        iifPanel.add(iifButton, gridBagConstraints);
      }

      pjrFilesList = new JList();
      pjrFilesList.setMinimumSize(new Dimension(2000, 200));
      JButton pjrButton = new JButton("Choose the input PJR files...");
      pjrButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          File startDir = null;
          if (!pjrFiles.isEmpty()) {
            startDir = pjrFiles.get(0).getParentFile();
          }
          JFileChooser jFileChooser = new JFileChooser(startDir);
          jFileChooser.setMultiSelectionEnabled(true);
          jFileChooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
              return "PJR Files";
            }

            @Override
            public boolean accept(File file) {
              return file.isDirectory() ||
                  PJR_FILENAME_FILTER.accept(file.getParentFile(), file.getName());
            }
          });
          int returnValue = jFileChooser.showOpenDialog(MainFrame.this);
          if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] files = jFileChooser.getSelectedFiles();
            setPjrFiles(files);
          }
        }
      });

      deletePjrFilesCheckBox = new JCheckBox("Delete PJR files after conversion");

      JPanel pjrPanel = new JPanel(new GridBagLayout()); {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridy = 0;
        pjrPanel.add(new JScrollPane(pjrFilesList), gridBagConstraints);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.gridy++;
        JPanel controlPanel = new JPanel(); {
          controlPanel.add(pjrButton);
          controlPanel.add(deletePjrFilesCheckBox);
        }
        pjrPanel.add(controlPanel, gridBagConstraints);
      }

      JPanel actionPanel = new JPanel(); {
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        JButton convertButton = new JButton("Convert");
        convertButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            try {
              Pjr2IifConverter pjr2IifConverter =
                  new Pjr2IifConverter(getPjrFiles(), getAccountIdMapFile());
              String output = pjr2IifConverter.convert();
              logger.fine("IIF:\n" + output);

              FileWriter fileWriter = new FileWriter(getIifFile());
              fileWriter.write(output);
              fileWriter.flush();
              fileWriter.close();

              logger.info("Converted PJRs to " + getIifFile());

              if (isDeletePjrFiles()) {
                logger.info("Deleting converted PJR files.");
                for (File pjrFile : pjrFiles) {
                  pjrFile.delete();
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
              System.exit(1);
            }
          }
        });

        actionPanel.add(Box.createHorizontalGlue());
        actionPanel.add(convertButton);
      }

      mainPanel.add(accountIdMapPanel);
      mainPanel.add(iifPanel);
      mainPanel.add(pjrPanel);
      mainPanel.add(Box.createVerticalGlue());
      mainPanel.add(actionPanel);
    }

    initFromConfig();

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        saveConfig(properties, CONFIG_LOCATION);
      }
    });

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setContentPane(mainPanel);
    this.setSize(800, 300);
  }

  private void initFromConfig() {
    String iifFileLocation = properties.getProperty(SettingsKey.IIF_OUTPUT_FILE.getKey());
    if (iifFileLocation != null) {
      setIifFile(new File(iifFileLocation));
    }

    String accountIdMapFileLocation =
        properties.getProperty(SettingsKey.ACCOUNT_ID_MAP_FILE.getKey());
    if (accountIdMapFileLocation != null) {
      setAccountIdMapFile(new File(accountIdMapFileLocation));
    }

    String pjrLocation =
        properties.getProperty(SettingsKey.PJR_LOCATION.getKey());
    if (pjrLocation != null) {
      File pjrLocationFile = new File(pjrLocation);
      if (pjrLocationFile.isDirectory()) {
        setPjrFiles(pjrLocationFile.listFiles(PJR_FILENAME_FILTER));
      } else {
        setPjrFiles(new File[] { pjrLocationFile });
      }
    }

    String deletePjrFilesString =
        properties.getProperty(SettingsKey.DELETE_PJRS_ON_CONVERT.getKey());
    if (deletePjrFilesString != null) {
      setDeletePjrFiles(Boolean.parseBoolean(deletePjrFilesString));
    }
  }

  private void setAccountIdMapFile(File accountIdMapFile) {
    this.accountIdMapFile = accountIdMapFile;
    accountIdMapTextField.setText(accountIdMapFile.toString());
  }

  public File getAccountIdMapFile() {
    return accountIdMapFile;
  }

  public File getIifFile() {
    return iifFile;
  }

  public void setIifFile(File iifFile) {
    this.iifFile = iifFile;
    iifFileTextField.setText(iifFile.toString());
  }

  public List<File> getPjrFiles() {
    return pjrFiles;
  }

  public boolean isDeletePjrFiles() {
    return deletePjrFilesCheckBox.isSelected();
  }

  public void setDeletePjrFiles(boolean deletePjrFiles) {
    deletePjrFilesCheckBox.setSelected(deletePjrFiles);
  }

  public void setPjrFiles(File[] pjrFiles) {
    this.pjrFiles.clear();
    DefaultListModel listModel = new DefaultListModel();
    pjrFilesList.setModel(listModel);
    for (File pjrFile : pjrFiles) {
      this.pjrFiles.add(pjrFile);
      listModel.addElement(pjrFile);
    }
  }

  private void loadConfig(Properties properties, String configLocation) {
    File configFile = new File(configLocation);
    try {
      properties.load(new FileInputStream(configFile));
    } catch (FileNotFoundException e1) {
      logger.info("Config file not found at " + configFile.getAbsolutePath() + ". Using defaults.");
    } catch (IOException e1) {
      logger.warning("Unable to load config file " + configFile.getAbsolutePath() + ": " + e1);
    }

    if (!properties.isEmpty() && logger.isLoggable(Level.INFO)) {
      StringBuilder stringBuilder = new StringBuilder("Settings:\n");
      for (String configKey : properties.stringPropertyNames()) {
        stringBuilder.append(configKey + "=" + properties.getProperty(configKey) + "\n");
      }
      logger.info(stringBuilder.toString());
    }
  }

  private void saveConfig(Properties properties, String configLocation) {
    File accountIdMapFile = getAccountIdMapFile();
    if (accountIdMapFile != null) {
      properties.setProperty(SettingsKey.ACCOUNT_ID_MAP_FILE.getKey(),
                             accountIdMapFile.getAbsolutePath());
    }
    File iifFile = getIifFile();
    if (iifFile != null) {
      properties.setProperty(SettingsKey.IIF_OUTPUT_FILE.getKey(),
                             iifFile.getAbsolutePath());
    }
    List<File> pjrFiles = getPjrFiles();
    if (!pjrFiles.isEmpty()) {
      properties.setProperty(SettingsKey.PJR_LOCATION.getKey(),
                             pjrFiles.get(0).getParent());
    }

    properties.setProperty(SettingsKey.DELETE_PJRS_ON_CONVERT.getKey(),
                           Boolean.toString(isDeletePjrFiles()));

    File configFile = new File(CONFIG_LOCATION);
    try {
      if (!configFile.exists()) {
        configFile.createNewFile();
      }
      properties.store(new FileOutputStream(configFile), null);
      logger.info("Saved config to " + configFile.getAbsolutePath());
    } catch (IOException e1) {
      logger.severe("Unable to save configuration to " + configFile.getAbsolutePath() + ": " + e1);
      e1.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      MainFrame mainFrame = new MainFrame();
      mainFrame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
