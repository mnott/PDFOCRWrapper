package org.mnsoft.pdfocr;

import org.apache.log4j.Category;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Properties;


/**
 * Provides a wrapper to load properties files.<p>
 *
 * Loading files from a location should be done using
 * the class loader of the current thread.
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
public class FileLoader {
  /**
   * The Log4J Logger.
   */
  static private Category log = Category.getInstance(FileLoader.class.getName());


  /**
   * Load a file as an InputStream.
   * @param name The file path, relative to the working directory. For web
   *        applications, this is WEB-INF/classes. The file may as well be
   *        in the CLASSPATH.
   * @return An InputStream pointing to the file. Null in case of any error.
   */
  @SuppressWarnings("rawtypes")
  public static InputStream load(String name) {
    /*
     * First we check whether there are multiple
     * occurrences of the resource in the class
     * path. If so, we complain but load the first
     * occurrence.
     */
    Enumeration e = null;
    try {
      e = Thread.currentThread().getContextClassLoader().getResources(name);
    } catch (IOException ioe) {
      log.fatal("! Cannot find resource " + name);

      return null;
    }

    /*
     * We load the resource and return the
     * InputStream.
     */
    try {
      URL u = (URL) e.nextElement();

      if (e.hasMoreElements()) {
        log.error("! Found multiple occurrences of " + name + ". Returning the first one.");
      }

      InputStream is = u.openConnection().getInputStream();

      return is;
    } catch (NoSuchElementException nse) {
      /*
       * Try to load the file as-is from the specified
       * location, where ever that is...
       */
      try {
        InputStream is = new FileInputStream(name);

        return is;
      } catch (IOException ioe) {
        /* Will be handled in outer catch block */
      }

      log.fatal("! Did not find the resource " + name);

      return null;
    } catch (IOException ioe) {
      log.fatal("! Did not find the resource " + name);

      return null;
    }
  }


  /**
   * Load a file as an InputStream.
   * @param name The file path, relative to the working directory. For web
   *        applications, this is WEB-INF/classes. The file may as well be
   *        in the CLASSPATH.
   * @return An byte array containing the file; an empty byte array in
   * case of an error.
   */
  public static byte[] loadBytes(String name) {
    return getBytesToEndOfStream(load(name));
  }


  /**
   * Load a properties file.
   *
   * @param name The file name or location.
   * @return The Properties object. Null in case of any error.
   */
  public static Properties loadProperties(String name) {
    final Properties p = new Properties();

    try {
      final InputStream is = FileLoader.load(name);
      if (is == null) {
        log.fatal("! Error reading file: " + name);

        return null;
      }

      p.load(is);
      is.close();
    } catch (IOException e) {
      log.fatal("! Error reading file " + name + ": " + e.getMessage());

      return null;
    }

    return p;
  }


  /**
   * Get all the bytes from an input stream.
   * @param in The input stream.
   * @return The bytes from that input stream. An empty byte
   * array in case of an error.
   */
  private static byte[] getBytesToEndOfStream( /*Buffered*/InputStream in) {
    if (in == null) {
      return new byte[0];
    }

    try {
      final int             chunkSize  = 2048;
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream(chunkSize);
      int                   val;

      while ((val = in.read()) != -1)
        byteStream.write(val);

      return byteStream.toByteArray();
    } catch (Exception e) {
      log.error("! Problem reading binary report content.");
    }

    return new byte[0];
  }
}
