PDFOCRWrapper
=============

This is a wrapper written in Java that allows to recursively iterate a directory structure and 
call an OCR engine on each found PDF on the condition that it hat not yet been called for that 
PDF. It works well with the ABBYY OCR Engine for Linux.

DESCRIPTION
=============

The PDF OCR Wrapper calls an external OCR engine recursively over the current
directory for all PDF files that it finds. When done, it sets the creator of
each PDF file to a configurable value. Files that have this creator set are
not sent to the OCR engine.

To install the program, download the binary distribution (the .jar file) as
well as the matching helpers distribution. The helpers distribution contains
jar files that are used, in particular the iText library.

The helpers distribution also contains the log4j.properties as well as the
pdfocr.properties files. These files have to be in the current directory.
Look into these files in order to understand them; the log4j.properties
file contains the logging definitions; more importantly, the file
pdfocr.properties contains the configuration parameters of the program.

All parameters that you find in pdfocr.properties can also be passed
to the program on the command line. In this case, the command line
parameters take precedence over the configuration file parameters.

Also, you can overwrite the name of the configuration file using the
command line parameter cmd=xyz.properties, pointing to another file.

If you have extracted all files into one directory, you should have

  pdfocrwrapper-1.1-201007011705.jar   (mandatory)
  pdfocr.properties                    (optional)
  log4j.jar                            (mandatory)
  log4j.properties                     (optional)
  iText.jar                            (mandatory)
  bcmail-jdk16-145.jar                 (optional)
  bcprov-jdk16-145.jar                 (optional)
  
The version numbers and time stamps of the files change of course.

Say, these files are in /tmp/pdfocrwrapper, and your pdf documents
are located somewhere under /data. Change to the /data directory
and run

  java -jar /tmp/pdfocrwrapper
  
In this case, the configuration file pdfocr.properties, if present,
from the directory /tmp/pdfocrwrapper is used. If you want to use
another configuration file, call like

  java -jar /tmp/pdfocrwrapper cmd=xyz.properties
  
If you have files that do not work with the wrapper - in particular 
files that have some DRM in them - you can try to use the Linux
tools pdf2ps and ps2pdf to convert these files to Postscript and
back:

  pdf2ps bad.pdf
  ps2pdf bad.ps
  rm bad.ps
  
If you want to manually set the creator of a given set of PDF files
so that they are not sent to the OCR engine at all, you can do like

for i in /tmp/pdfocrwrapper/*.jar ; do export CLASSPATH=$CLASSPATH:$i; done
java org.mnsoft.pdfocr.CreatorSetter ocr a.pdf b.pdf c.pdf

with any number of PDF files. In this case, the creator field of
the selected PDF files is going to be changed to "ocr". This process
is non recursive.

If you want to have less logging, edit log4j.properties, find the word
DEBUG and change it to any of INFO, WARN, ERROR, FATAL.

Finally, there is a more flexible PDF metadata tool in this package -
to use it, look up org.mnsoft.pdfocr.PDFTrans in the source distribution.


   