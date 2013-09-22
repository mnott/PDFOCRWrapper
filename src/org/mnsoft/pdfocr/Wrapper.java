package org.mnsoft.pdfocr;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import org.apache.commons.io.FileUtils;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;


/**
 * Provides the wrapper for the OCR engine.<p>
 *
 * This program is a free software available under the GNU
 * Lesser General Public License.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * @author (c) 2010, Matthias Nott
 */
public class Wrapper {
  /**
   * Logger for this class
   */
  private static final Logger     log              = Logger.getLogger(Wrapper.class);

  /**
   * Default config file name.
   */
  public static final String      CONFIG_FILE_NAME = "pdfocr.properties";

  /**
   * The command to run the OCR Engine,
   * with place holders for the Input and Output file names.
   */
  public String                   OCR_COMMAND      = "abbyyocr -ic Uncompressed -if ###IF### -f PDF -pem ImageOnText -pfq 100% -pfc ###CREATOR### -rtn -of ###OF###";


  /**
   * The creator field entry. If detected,
   * file is not treated (again).
   */
  public String                   OCR_CREATOR      = "ocr";


  /**
   * Temporary extension added to the file
   * name when running the OCR.
   */
  public String                   TMP_EXTENSION    = ".ocr";


  /**
   * Directory for temporary files
   */
  public String                   TMP_DIR          = ".";


  /**
   * The program parameters.
   */
  private HashMap<String, String> parameters       = new HashMap<String, String>();


  /**
   * The configuration file name, if any.
   */
  private String                  propertiesFile   = null;


  /**
   * Working directory.
   */
  private String                  wd               = ".";


  /**
   * Constructor.
   */
  public Wrapper(String wd) {
    this.wd = wd;
  }

  /**
   * The main loop.
   * @param args
   */
  public static void main(String[] args) throws Exception {
    final Wrapper w = new Wrapper((args.length == 0) ? "." : args[0]);

    w.init(args);

    w.run();
  }


  /**
   * Run the Wrapper.
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws DocumentException
   */
  @SuppressWarnings("rawtypes")
  public void run() throws IOException, InterruptedException, DocumentException {
    RecursiveFileListIterator it = new RecursiveFileListIterator(new File(wd), new FileFilter(".pdf"));

    while (it.hasNext()) {
      final File   originalFile         = it.next();
      final String originalFilePath     = originalFile.getAbsolutePath();

      /*
       * Open the reader on the original File
       */
      PdfReader    readerOnOriginalFile;

      try {
        readerOnOriginalFile = new PdfReader(originalFilePath);
      } catch (Exception e) {
        log.error("! ERROR: " + e.getMessage() + " File: " + originalFilePath);

        continue;
      }

      /*
       * Get the document information
       */
      Map    info        = readerOnOriginalFile.getInfo();

      /*
       * Get the document creator. If the document
       * has already been worked on, continue with
       * the next document.
       */
      String doc_creator = (String) info.get("Creator");

      if (this.OCR_CREATOR.equals(doc_creator)) {
        log.debug("+ INFO: File " + originalFilePath + " had already been run trough OCR engine. Skipping.");

        continue;
      }

      /*
       * Get the document time stamp so that we can set it later.
       */
      final Date doc_timestamp = new Date(originalFile.lastModified());

      /*
       * Get the number of pages in the original file
       */
      int        nOri          = readerOnOriginalFile.getNumberOfPages();

      log.debug("+ Working on: " + originalFilePath + " (" + nOri + " pages).");

      final StringBuffer sb = new StringBuffer();

      sb.append(originalFilePath + " ... ");

      /*
       * Get the remaining meta data
       */
      String doc_title    = ((String) info.get("Title") == null) ? "" : (String) info.get("Title");
      String doc_subject  = ((String) info.get("Subject") == null) ? "" : (String) info.get("Subject");
      String doc_keywords = ((String) info.get("Keywords") == null) ? "" : (String) info.get("Keywords");
      String doc_author   = ((String) info.get("Author") == null) ? "" : (String) info.get("Author");

      readerOnOriginalFile.close();

      /*
       * Set the creator to our marker
       */
      doc_creator = this.OCR_CREATOR;

      /*
       * Run the OCR Engine
       */
      File outputFileFromOCR = null;
      try {
        outputFileFromOCR = ocr(originalFile);
      } catch (Exception e) {
        log.error("! ERROR: " + e.getMessage());

        continue;
      }

      /*
       * Check for the result of the OCR Engine
       */
      if ((outputFileFromOCR == null) || !outputFileFromOCR.exists()) {
        continue;
      }

      log.debug("+ " + outputFileFromOCR.getAbsolutePath() + " has come out of the OCR engine.");

      /*
       * Create final output
       */

      /*
       * Create a temporary file and copy the source
       * file to it, to avoid UTF-8 encoding problems
       * on the filename confusing the OCR engine
       */
      final File temp = File.createTempFile("ocr", ".pdf", new File(this.TMP_DIR));
      temp.deleteOnExit();

      mergePDFs(originalFile, outputFileFromOCR, temp, doc_title, doc_subject, doc_keywords, doc_author, doc_creator);

      FileUtils.deleteQuietly(originalFile);

      FileUtils.moveFile(temp, new File(originalFilePath));

      /*
       * Set the file access time
       */
      if ("true".equals(getAttribute("KEEPTS"))) {
        if (originalFile.exists()) {
          originalFile.setLastModified(doc_timestamp.getTime() + 1000);
        }
      }

      /*
       * Finally, remove the temporary document
       */
      FileUtils.deleteQuietly(temp);
      FileUtils.deleteQuietly(outputFileFromOCR);
    }
  }


  /**
   * Run the OCR command
   *
   * @param originalFile The file to run the command on
   * @return The file that was created
   * @throws IOException
   * @throws InterruptedException
   */
  private File ocr(File originalFile) throws IOException, InterruptedException {
    /*
     * Create a temporary file and copy the source
     * file to it, to avoid UTF-8 encoding problems
     * on the filename confusing the OCR engine
     */
    log.debug("> Creating Temporary Source File");

    final File sourceFileForOCR = File.createTempFile("ocr", ".pdf", new File(this.TMP_DIR));
    sourceFileForOCR.deleteOnExit();

    log.debug("< Created Temporary Source File: " + sourceFileForOCR.getAbsolutePath());

    FileUtils.copyFile(originalFile, sourceFileForOCR, true);

    log.debug("+ Copied " + originalFile.getAbsolutePath() + " to " + sourceFileForOCR.getAbsolutePath());

    /*
     * Create the command line
     */
    String[] cmd = StringUtility.split(getAttribute("cmd"), " ");
    for (int i = 0; i < cmd.length; i++) {
      if ("###IF###".equals(cmd[i])) {
        cmd[i] = sourceFileForOCR.getAbsolutePath();
      } else if ("###OF###".equals(cmd[i])) {
        cmd[i] = sourceFileForOCR.getAbsolutePath() + this.TMP_EXTENSION;
      } else if ("###CREATOR###".equals(cmd[i])) {
        cmd[i] = getAttribute("creator");
      }
    }

    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < cmd.length; i++) {
      sb.append(cmd[i]);
      sb.append(" ");
    }

    log.debug("> Calling OCR Engine: " + sb);

    callOCREngine(cmd);

    /*
     * Copy temporary output file to output file
     */
    final File targetFile = new File(this.TMP_DIR + "/" + originalFile.getName() + this.TMP_EXTENSION);
    FileUtils.deleteQuietly(targetFile);

    FileUtils.moveFile(new File(sourceFileForOCR.getAbsolutePath() + this.TMP_EXTENSION), targetFile);

    /*
     * Delete temporary file
     */
    FileUtils.deleteQuietly(new File(sourceFileForOCR.getAbsolutePath() + this.TMP_EXTENSION));

    log.debug("< Calling OCR Engine. Output file is: " + targetFile.getAbsolutePath());

    return targetFile;
  }


  /**
   * Call an external program.
   *
   * @param cmd
   * @throws IOException
   * @throws InterruptedException
   */
  private void callOCREngine(String[] cmd) throws IOException, InterruptedException {
    Runtime run = Runtime.getRuntime();
    Process pr  = run.exec(cmd);
    pr.waitFor();

    BufferedReader buf  = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    String         line = "";
    while ((line = buf.readLine()) != null) {
      log.info(line);
    }
  }


  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void mergePDFs(File foreground, File background, File newFile, String title, String subject, String keywords, String author, String creator) {
    log.debug("Merge " + foreground + " (FG) and " + background + " (BG) to " + newFile);

    final double threshold = ((Integer) StringUtility.StringToInteger(getAttribute("THRESHOLD"), 2)).doubleValue();

    try {
      /*
       * Foreground: Original Image.
       * Background: OCR'd Text
       */
      final PdfReader fg           = new PdfReader(foreground.getAbsolutePath());
      final PdfReader bg           = new PdfReader(background.getAbsolutePath());

      /*
       * Count pages for foreground and background
       */
      final int       fg_num_pages = fg.getNumberOfPages();
      final int       bg_num_pages = bg.getNumberOfPages();

      if (fg_num_pages != bg_num_pages) {
        log.error("! Foreground and background have different number of pages. This should really not happen.");
      }

      /*
       *  The output document
       */
      final PdfStamper fg_writer = new PdfStamper(fg, new FileOutputStream(newFile));

      /*
       * Create a PdfTemplate from the first page of mark
       * (PdfImportedPage is derived from PdfTemplate)
       */
      PdfImportedPage  bg_page   = null;
      for (int i = 0; i < fg_num_pages;) {
        ++i;
        System.out.print(" [" + i + "]");

        final byte[] fg_page_content = fg.getPageContent(i);
        final byte[] bg_page_content = bg.getPageContent(i);

        final int    bg_size         = bg_page_content.length;
        final int    fg_size         = fg_page_content.length;

        /*
         * If we're not explicitly merging, we're merging
         * the document with itself only anyway.
         */
        if (!"true".equals(getAttribute("mergefiles"))) {
          continue;
        }

        /*
         * Modification 20130904
         *
         * We want to scan only what's not been generated by a number of
         * generators. So, until now, the generator of whom we wanted to
         * ignore files was ocr, i.e. the one we set ourselves. Now, we
         * have seen that when we run an OCR on a "pdf+text" file, as we
         * collate in post the file with its image, we get an overlapping
         * text which is not pixel correct, i.e. which makes the PDF appear
         * not nicely.
         *
         * If the background image is not at least threshold times as large as
         * the foreground image, we assume we've been working on a
         * page that was plain text already, and don't add the image
         * to the background.
         */
        if ((bg_size / fg_size) <= threshold) {
          log.debug("! Not adding background for page " + i + " since background size (" + bg_size + ") not different enough from foreground size (" + fg_size + ").");

          continue;
        }

        bg_page = fg_writer.getImportedPage(bg, i);

        final PdfContentByte contentByte = fg_writer.getUnderContent(i);

        contentByte.addTemplate(bg_page, 0, 0);
      }

      HashMap map = fg_writer.getMoreInfo();
      if (map == null) {
        map = new HashMap();
      }

      if (title != null) {
        map.put("Title", title);
      }

      if (subject != null) {
        map.put("Subject", subject);
      }

      if (keywords != null) {
        map.put("Keywords", keywords);
      }

      if (author != null) {
        map.put("Author", author);
      }

      if (creator != null) {
        map.put("Creator", creator);
      }

      fg_writer.setMoreInfo(map);

      fg_writer.close();

      System.out.println("");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Initialize the program.
   *
   * @param args The command line parameters.
   */
  private void init(String[] args) {
    /*
     * Get the command line Parameters.
     */
    getCommandLineParameters(args);

    /*
     * Even if the constructor had set the name
     * of the properties file, we are able to
     * overwrite that name by the "cfg" command
     * line parameter.
     */
    this.propertiesFile = (String) getAttribute("cfg");
    if ((this.propertiesFile == null) || "".equals(this.propertiesFile)) {
      this.propertiesFile = Wrapper.CONFIG_FILE_NAME;
    }

    readConfiguration(this.propertiesFile);
  }


  /**
   * Read the configuration
   * properties file.
   *
   * @param filename The configuration file name.
   */
  @SuppressWarnings("rawtypes")
  private void readConfiguration(String filename) {
    /*
     * Read the configuration file
     */
    log.debug("> readConfiguration");
    log.debug("> Loading configuration from " + filename);

    final Properties p = FileLoader.loadProperties(filename);
    if (p == null) {
      log.info("! No configuration file specified. Using default values.");
      setAttribute("cmd", this.OCR_COMMAND);
      setAttribute("creator", this.OCR_CREATOR);
      setAttribute("tmpext", this.TMP_EXTENSION);
      log.debug("+ Configuration Parameter CMD=" + this.OCR_COMMAND);
      log.debug("+ Configuration Parameter CREATOR=" + this.OCR_CREATOR);
      log.debug("+ Configuration Parameter TMPEXT=" + this.TMP_EXTENSION);

      log.debug("< readConfiguration");

      return;
    }

    /*
     * Convert property names to upper case
     */
    final TreeMap<String, String> uppercaseConfigurationParameters = new TreeMap<String, String>();
    for (final Iterator it = p.keySet().iterator(); it.hasNext();) {
      final String pName  = (String) it.next();
      final String pValue = (String) p.get(pName);
      uppercaseConfigurationParameters.put(pName.toUpperCase(), pValue);
    }

    /*
     * We save the parameters that already are in the
     * HashMap, i.e. which have been passed in as command
     * line parameters
     */
    final TreeMap<String, String> uppercaseCommandLineParameters = new TreeMap<String, String>();
    for (final Iterator it = this.parameters.keySet().iterator(); it.hasNext();) {
      final String pName  = (String) it.next();
      final String pValue = (String) this.parameters.get(pName);
      uppercaseCommandLineParameters.put(pName.toUpperCase(), pValue);
    }

    final HashMap<String, String> upperCaseParameters = new HashMap<String, String>();

    for (final String key : uppercaseConfigurationParameters.keySet()) {
      final String value = uppercaseConfigurationParameters.get(key);
      upperCaseParameters.put(key, value);
      log.debug("+ Configuration Parameter " + key + "=" + value);
      if ((key.length() > 4) && key.substring(0, 4).equalsIgnoreCase("SYS.")) {
        System.setProperty(key.substring(4), value);
        log.debug("+ Java System   Parameter " + key.substring(4) + "=" + value);
      }
    }

    for (final String key : uppercaseCommandLineParameters.keySet()) {
      final String value = uppercaseCommandLineParameters.get(key);
      upperCaseParameters.put(key, value);
      log.debug("+ Command Line  Parameter " + key + "=" + value);
      if ((key.length() > 4) && key.substring(0, 4).equalsIgnoreCase("SYS.")) {
        System.setProperty(key.substring(4), value);
        log.debug("+ Java System   Parameter " + key.substring(4) + "=" + value);
      }
    }

    this.parameters = upperCaseParameters;

    if (getAttribute("cmd") != null) {
      this.OCR_COMMAND = getAttribute("cmd");
      log.debug("+ Configuration Parameter CMD=" + this.OCR_COMMAND);
    }

    if (getAttribute("creator") != null) {
      this.OCR_CREATOR = getAttribute("CREATOR");
      log.debug("+ Configuration Parameter CREATOR=" + this.OCR_CREATOR);
    }

    if (getAttribute("tmpext") != null) {
      this.TMP_EXTENSION = getAttribute("tmpext");
      log.debug("+ Configuration Parameter TMPEXT=" + this.TMP_EXTENSION);
    }

    if (getAttribute("tmpdir") != null) {
      this.TMP_DIR = getAttribute("tmpdir");
      log.debug("+ Configuration Parameter TMPDIR=" + this.TMP_DIR);
    }

    log.debug("< readConfiguration");
  }


  /**
   * Get the command line parameters into the
   * parameters HashMap.
   *
   * @param args The command line parameters.
   */
  private void getCommandLineParameters(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String parametername  = StringUtility.getParameter(args[i], true, false);
      String parametervalue = StringUtility.getParameter(args[i], false, false);
      if ((parametername != null) && (parametervalue != null)) {
        setAttribute(parametername, parametervalue);
      }
    }
  }


  /**
   * Get an Application Attribute.
   * @param par The Attribute parameter name, not case sensitive.
   * @return The Attribute parameter value.
   */
  private String getAttribute(String par) {
    final String value = this.parameters.get(par.toUpperCase());

    return value;
  }


  /**
   * Set an Application Attribute.
   *
   * Use with great care. This can open a can of worms
   * in terms of concurrency.
   *
   * @param par The Attribute parameter name, not case sensitive.
   * @param val The Attribute parameter value.
   */
  private void setAttribute(String par, String val) {
    if ((null != par) && (null != val)) {
      this.parameters.put(par.toUpperCase(), val);
    }
  }
}
