package org.mnsoft.pdfocr;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BadPdfFormatException;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;
import java.util.Map;


/**
 * Creator Setter.
 *
 * This can be used to set the creator of a given PDF document.
 * It helps to avoid the OCR engine to run on files where we
 * don't want it to run.
 *
 * This program is a free software available under the GNU
 * Lesser General Public License.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *
 * @author (c) 2010, Matthias Nott
 */
public class CreatorSetter {
  /**
   * @param args
   * @throws DocumentException
   * @throws IOException
   * @throws IOException
   * @throws BadPdfFormatException
   */
  @SuppressWarnings("rawtypes")
  public static void main(String[] args) throws DocumentException, IOException {
    /*
     * Verify arguments
     */
    if ((args == null) || (args.length < 2)) {
      System.err.println("Usage: first parameter: Creator to set, following parameters: Files to work on.");
      System.exit(1);
    }

    final String creator = args[0];

    for (int i = 1; i < args.length; i++) {
      final File f = new File(args[i]);

      if ((f == null) || !f.exists() || !f.isFile() || !f.getName().endsWith(".pdf")) {
        System.err.println("! ERROR: Could not read " + args[i] + " or this is not a .pdf");

        continue;
      }

      final String p      = f.getAbsolutePath();

      /*
       * Open the reader
       */
      PdfReader    reader;

      try {
        reader = new PdfReader(p);
      } catch (Exception e) {
        System.err.println("! ERROR: " + e.getMessage() + " File: " + p);

        continue;
      }

      /*
       * Get the document information
       */
      Map    info        = reader.getInfo();

      /*
       * Get the document creator. If the document
       * has already been worked on, continue with
       * the next document.
       */
      String doc_creator = (String) info.get("Creator");

      if (creator.equals(doc_creator)) {
        System.out.println("+ INFO: File " + p + " had already the right creator.");

        continue;
      }

      /*
       * Get the document time stamp so that we can set it later.
       */
      final Date doc_timestamp = new Date(f.lastModified());

      /*
       * Get the number of pages in the original file
       */
      int        nOri          = reader.getNumberOfPages();

      System.out.print("+ INFO: Working on: " + p + " (" + nOri + " pages) ... ");

      /*
       * Get the remaining meta data
       */
      String doc_title    = ((String) info.get("Title") == null) ? "" : (String) info.get("Title");
      String doc_subject  = ((String) info.get("Subject") == null) ? "" : (String) info.get("Subject");
      String doc_keywords = ((String) info.get("Keywords") == null) ? "" : (String) info.get("Keywords");
      String doc_author   = ((String) info.get("Author") == null) ? "" : (String) info.get("Author");

      reader.close();

      /*
       * Set the creator to our marker
       */
      doc_creator = creator;

      /*
       * Merge the new document with the meta
       * data from the original document
       */
      try {
        reader = new PdfReader(p);
      } catch (Exception e) {
        System.err.println("! ERROR: " + e.getMessage() + " File: " + p);

        continue;
      }

      /*
       * Get the document information
       */
      info = reader.getInfo();

      /*
       * Get the document creator. If the document
       * has already been worked on, we assume we
       * have had a successful output from the OCR
       * engine
       */
      String doc_creator_copy = (String) info.get("Creator");

      if (creator.equals(doc_creator_copy)) {
        System.out.println();

        continue;
      }

      /*
       * Step 1: creation of a document object
       */
      final Document document = new Document(reader.getPageSizeWithRotation(1));

      /*
       * Step 2: we create a writer that listens to the document
       */
      PdfCopy        writer   = new PdfCopy(document, new FileOutputStream(p + ".tmp"));

      /*
       * Step 3: we add the meta data
       */
      document.addTitle(doc_title);
      document.addSubject(doc_subject);
      document.addKeywords(doc_keywords);
      document.addCreator(creator);
      document.addAuthor(doc_author);

      /*
       * Step 4: we open the document
       */
      document.open();

      PdfImportedPage page;

      int             j = 0;

      /*
       * Step 5: we add content
       */
      while (j < nOri) {
        j++;
        page = writer.getImportedPage(reader, j);
        writer.addPage(page);

        System.out.print("[" + j + "] ");
      }

      PRAcroForm form = reader.getAcroForm();
      if (form != null) {
        writer.copyAcroForm(reader);
      }

      System.out.println();

      /*
       * Step 6: we close the document
       */
      document.close();
      reader.close();

      /*
       * Set the file access time and
       * rename the file.
       */
      File file = new File(p + ".tmp");

      if (file.exists()) {
        deleteFile(p);
        file.setLastModified(doc_timestamp.getTime());
        file.renameTo(new File(p));
      }
    }
  }


  /**
   * Delete a file.
   *
   * @param file The path of the file to delete
   */
  private static void deleteFile(String file) {
    File f = new File(file);

    /*
     *  Make sure the file or directory exists and isn't write protected
     */
    if (!f.exists()) {
      throw (new IllegalArgumentException("Delete: no such file or directory: " + file));
    }

    if (!f.canWrite()) {
      throw (new IllegalArgumentException("Delete: write protected: " + file));
    }

    /*
     *  If it is a directory, make sure it is empty
     */
    if (f.isDirectory()) {
      String[] files = f.list();
      if (files.length > 0) {
        throw (new IllegalArgumentException("Delete: directory not empty: " + file));
      }
    }

    /*
     *  Attempt to delete it
     */
    boolean success = f.delete();

    if (!success) {
      throw (new IllegalArgumentException("Delete: deletion failed"));
    }
  }
}
