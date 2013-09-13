package org.mnsoft.pdfocr;

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Iterates over all non-directory files contained in some sub
 * directory of the current directory.
 *
 * The code is based on code from David R. MacIver.
 *
 * This program is a free software available under the GNU
 * Lesser General Public License.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * @author (c) 2010, David R. MacIver; adapted by Matthias Nott (a.k.a. David did all the work, Matthias just abuses it.)
 */
public class RecursiveFileListIterator implements Iterator<File> {
  private final FlatteningIterator flatteningIterator;

  public RecursiveFileListIterator(File file, FileFilter filter) {
    this.flatteningIterator = new FlatteningIterator(new FileIterator(file, filter));
  }


  public RecursiveFileListIterator(File file) {
    this(file, null);
  }

  public void remove() {}


  public boolean hasNext() {
    return flatteningIterator.hasNext();
  }


  public File next() {
    return (File) flatteningIterator.next();
  }


  /**
   * Iterator to iterate over all the files contained in a directory. It returns
   * a File object for non directories or a new FileIterator object for directories.
   */
  private static class FileIterator implements Iterator<Object> {
    private final Iterator<File> files;
    private  FileFilter     filter = null;

    FileIterator(File file, FileFilter filter) {
      if (file.isFile()) {
        final ArrayList<File> l = new ArrayList<File>();
        l.add(file);
        this.files = l.iterator();
      } else {
        this.files  = Arrays.asList(file.listFiles(filter)).iterator();
        this.filter = filter;
      }
    }

    public void remove() {}


    public Object next() {
      File next = this.files.next();

      if (next.isDirectory()) {
        return new FileIterator(next, this.filter);
      } else {
        return next;
      }
    }


    public boolean hasNext() {
      return this.files.hasNext();
    }
  }
}
