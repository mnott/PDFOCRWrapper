package org.mnsoft.pdfocr;

import java.io.File;


/**
 * File Filter.
 *
 * Used internally by the wrapper to filter file names
 * allowed to be passed to the OCR engine.
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
public class FileFilter implements java.io.FileFilter {
  private String ext = "";

  public FileFilter(String ext) {
    this.ext = ext;
  }

  @Override public boolean accept(File pathname) {
    if (pathname.isDirectory() || pathname.getName().endsWith(this.ext)) {
      if (pathname.getName().startsWith(".")) {
        return false;
      }

      return true;
    }

    return false;
  }
}
