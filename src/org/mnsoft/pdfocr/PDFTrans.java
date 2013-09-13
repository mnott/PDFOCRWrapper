package org.mnsoft.pdfocr;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * PDFTrans.java (v1.2), Copyright (C)2002-2004 Sebastien Aperghis-Tramoni
 * based on the file Encrypt.java (v1.3), Copyright (C)2002 Bruno Lowagie
 *
 * This program is a free software available under the GNU
 * Lesser General Public License.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * History
 * -------
 * v1.3 - 2010.07.01
 *   - Matthias Nott made some mindboggingly irrelevant modifications
 * v1.2 - 2004.04.19
 *   - merged modifications and corrections by Robin Houston
 *     -> added option --encryption-bits
 *     -> added option --print-info
 *     -> reuse document metadata
 *     -> replaced all StringBuffer's with String's
 *     -> added error checking and reporting
 *
 * v1.1 - 2002.12.13
 *   - updated using CopyPdf.java example from Paulo Soares in order
 *     to keep PDF links (but not the bookmarks) during the conversion
 *     -> uses PdfCopy instead of PdfWriter
 *     -> removed the PdfContentBytes and pages rotation stuff
 *     -> uses copyAcroForm()
 *
 * v1.0 - 2002.10.06
 *   - creation of this program
 */
public class PDFTrans extends java.lang.Object {
  /**
   * @param error the warning string
   */
  public static void warning(String warning) {
    System.err.println("PDFTrans: warning: " + warning);
  }


  /**
   * @param error the error string
   */
  public static void error(String error) {
    System.err.println("PDFTrans: error: " + error);
    System.exit(1);
  }


  public static void usage() {
    System.err.print("usage: pdftrans [options] srcfile destfile\n" + "\n" + "metadata options:\n" + "  --title <string>    sets the title of the document\n" + "  --subject <string>  sets the subject of the document\n" + "  --keywords <string> sets the keywords field of the document\n" + "  --creator <string>  sets the creator field of the document\n" + "  --author <string>   sets the author field of the document\n" + "  --print-info        prints document information before processing\n" + "  --print-keywords    prints document keywords before processing\n" + "\n" + "protection/encryption options:\n" + "  --user-password <string>    sets the user password\n" + "  --master-password <string>  sets the master password\n" + "  --encryption-bits <number>  number of encryption bits (40 or 128)\n" + "  --permissions <list>        comma-separated list of the allowed actions\n" + "      available actions: print, degraded-print, copy, modify-contents,\n" + "                         modify-annotations, assembly, fill-in, screen-readers\n");
    System.exit(1);
  }


  /**
   * @param args the command line arguments
   */
  @SuppressWarnings({ "deprecation", "rawtypes" })
  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
    }

    String  input_file      = null, output_file = null, doc_title = null, doc_subject = null, doc_keywords = null, doc_creator = null, doc_author = null, user_passwd = null, owner_passwd = null;

    boolean encrypt         = false;
    boolean encryption_bits = PdfWriter.STRENGTH128BITS;
    int     permissions     = 0;
    boolean print_info      = false;
    boolean print_keywords  = false;

    /*
     *  parse options
     */
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--title")) {
        doc_title = args[++i];
      } else if (args[i].equals("--subject")) {
        doc_subject = args[++i];
      } else if (args[i].equals("--keywords")) {
        doc_keywords = args[++i];
      } else if (args[i].equals("--creator")) {
        doc_creator = args[++i];
      } else if (args[i].equals("--author")) {
        doc_author = args[++i];
      } else if (args[i].equals("--print-info")) {
        print_info = true;
      } else if (args[i].equals("--print-keywords")) {
        print_keywords = true;
      } else if (args[i].equals("--user-password")) {
        encrypt     = true;
        user_passwd = args[++i];
      } else if (args[i].equals("--master-password")) {
        encrypt      = true;
        owner_passwd = args[++i];
      } else if (args[i].equals("--encryption-bits")) {
        i++;
        encrypt = true;

        if (args[i].equals("128")) {
          encryption_bits = PdfWriter.STRENGTH128BITS;
        } else if (args[i].equals("40")) {
          encryption_bits = PdfWriter.STRENGTH40BITS;
        } else {
          usage();
        }

        continue;
      } else if (args[i].equals("--permissions")) {
        i++;

        StringTokenizer st = new StringTokenizer(args[i], ",");
        while (st.hasMoreTokens()) {
          String s = st.nextToken();
          if (s.equals("print")) {
            permissions |= PdfWriter.AllowPrinting;
          } else if (s.equals("degraded-print")) {
            permissions |= PdfWriter.AllowDegradedPrinting;
          } else if (s.equals("copy")) {
            permissions |= PdfWriter.AllowCopy;
          } else if (s.equals("modify-contents")) {
            permissions |= PdfWriter.AllowModifyContents;
          } else if (s.equals("modify-annotations")) {
            permissions |= PdfWriter.AllowModifyAnnotations;
          } else if (s.equals("assembly")) {
            permissions |= PdfWriter.AllowAssembly;
          } else if (s.equals("fill-in")) {
            permissions |= PdfWriter.AllowFillIn;
          } else if (s.equals("screen-readers")) {
            permissions |= PdfWriter.AllowScreenReaders;
          } else {
            warning("Unknown permission '" + s + "' ignored");
          }
        }

        continue;
      } else if (args[i].startsWith("--")) {
        error("Unknown option '" + args[i] + "'");
      } else if (input_file == null) {
        input_file = args[i];
      } else if (output_file == null) {
        output_file = args[i];
      } else {
        usage();
      }
    }

    if (!print_keywords) {
      if ((input_file == null) || (output_file == null)) {
        usage();
      }

      if (input_file.equals(output_file)) {
        error("Input and output files must be different");
      }
    }

    try {
      /*
       *  we create a reader for the input file
       */
      if (!print_keywords) {
        System.out.println("Reading " + input_file + "...");
      }

      PdfReader reader = new PdfReader(input_file);

      /*
       *  we retrieve the total number of pages
       */
      final int n      = reader.getNumberOfPages();
      if (!print_keywords) {
        System.out.println("There are " + n + " pages in the original file.");
      }

      /*
       *  get the document information
       */
      final Map info = reader.getInfo();

      /*
       *  print the document information if asked to do so
       */
      if (print_info) {
        System.out.println("Document information:");

        final Iterator it = info.entrySet().iterator();
        while (it.hasNext()) {
          final Map.Entry entry = (Map.Entry) it.next();
          System.out.println(entry.getKey() + " = \"" + entry.getValue() + "\"");
        }
      }

      if (print_keywords) {
        String keywords = "" + info.get("Keywords");
        if ((null == keywords) || "null".equals(keywords)) {
          keywords = "";
        }

        System.out.println(keywords);
        System.exit(0);
      }

      /*
       *  if any meta data field is unspecified,
       *  copy the value from the input document
       */
      if (doc_title == null) {
        doc_title = (String) info.get("Title");
      }

      if (doc_subject == null) {
        doc_subject = (String) info.get("Subject");
      }

      if (doc_keywords == null) {
        doc_keywords = (String) info.get("Keywords");
      }

      if (doc_creator == null) {
        doc_creator = (String) info.get("Creator");
      }

      if (doc_author == null) {
        doc_author = (String) info.get("Author");
      }

      // null metadata field are simply set to the empty string
      if (doc_title == null) {
        doc_title = "";
      }

      if (doc_subject == null) {
        doc_subject = "";
      }

      if (doc_keywords == null) {
        doc_keywords = "";
      }

      if (doc_creator == null) {
        doc_creator = "";
      }

      if (doc_author == null) {
        doc_author = "";
      }

      /*
       *  step 1: creation of a document-object
       */
      final Document document = new Document(reader.getPageSizeWithRotation(1));

      /*
       *  step 2: we create a writer that listens to the document
       */
      final PdfCopy  writer   = new PdfCopy(document, new FileOutputStream(output_file));

      /*
       *  step 3.1: we add the meta data
       */
      document.addTitle(doc_title);
      document.addSubject(doc_subject);
      document.addKeywords(doc_keywords);
      document.addCreator(doc_creator);
      document.addAuthor(doc_author);

      /*
       *  step 3.2: we set up the protection and encryption parameters
       */
      if (encrypt) {
        writer.setEncryption(encryption_bits, user_passwd, owner_passwd, permissions);
      }

      /*
       *  step 4: we open the document
       */
      System.out.print("Writing " + output_file + "... ");
      document.open();

      PdfImportedPage page;

      int             i = 0;

      // step 5: we add content
      while (i < n) {
        i++;
        page = writer.getImportedPage(reader, i);
        writer.addPage(page);

        System.out.print("[" + i + "] ");
      }

      final PRAcroForm form = reader.getAcroForm();
      if (form != null) {
        writer.copyAcroForm(reader);
      }

      System.out.println();

      // step 6: we close the document
      document.close();
    } catch (Exception e) {
      error(e.getClass().getName() + ": " + e.getMessage());
    }
  }
}
