#!/bin/bash
#
# Some quickly hacked together script that can be called from 
# incron like this:
#
# /data/Files/OCR IN_CREATE /usr/local/bin/pdfocr-folderaction.sh $#
#
# It makes some assumptions, e.g. using mutt, but as it is pretty short,
# I'm not going to comment a lot. Use it as a sample, and if you don't
# like it, read the license file.

#
# Configuration
#
export wd=/data/Files/OCR
export owner=hans
export mailto=results@test.com
export log=/var/log/pdfocr-$RANDOM.log
export tolog=/var/log/pdfocr.log
export jar=/usr/local/bin/pdfocr.jar

#
# Give some time to finish copying
#
sleep 10

#
# Sleep some more random time to avoid races
#
sleep $[ ( $RANDOM % 10 )  + 1 ]s

#
# Make sure Abbyy Page Counter does not run out
#
# /var/lib/frengine has been backed up to
# /var/lib/frengine.new, and we restore it
# now so as to make sure that we don't happen
# to run out of pages at this point. Make sure
# you rest in the number of page licenses you've
# acquired from Abbyy.
#
(
  cd /var/lib
  rm -rf /var/lib/frengine
  cp -av /var/lib/frengine.new /var/lib/frengine
)

#
# Do work
#
(
  #
  # Create directory structures if not there
  #
  if [ ! -d "$wd" ]; then mkdir -p "$wd"; chown $owner "$wd"; fi 
  if [ ! -d "$wd/processing" ]; then mkdir "$wd/processing"; chown $owner "$wd/processing" ; fi
  if [ ! -d "$wd/output" ]; then mkdir "$wd/output"; chown $owner "$wd/output"; fi

  cd "$wd"

  #
  # If the file was moved already to the processing
  # directory, just return doing nothing: This will
  # be a race condition.
  #
  if [ -f "$wd/processing/$*" ]; then exit 0; fi

  #
  # Otherwise, move the file to the processing directory now
  #
  if [ ! -f "$wd/$*" ]; then echo "$wd/$*" not found | tee -a "$log"; fi 
  mv "$wd/$*" "$wd/processing" 2>&1 | tee -a "$log"
  
  #
  # Go to the processing directory
  #
  cd "$wd/processing"

  #
  # Call the OCR engine
  #
  java -jar "$jar" "$*" 2>&1 | tee -a "$log"

  #
  # Move the result to the output directory
  # and make sure to set the owner
  #
  mv "$wd/processing/$*" "$wd/output"

  chown $owner "$wd/output/$*"

  #
  # Send out a mail
  #
  mutt -s "Done processing: $*" $mailto <"$log" 

  cat "$log" >>"$tolog"

  rm "$log"
)