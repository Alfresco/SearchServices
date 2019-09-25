package org.alfresco.solr.handler;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class OldBackupDirectory implements Comparable<OldBackupDirectory> {
  private static final Pattern dirNamePattern = Pattern.compile("^snapshot[.](.*)$");

  private URI basePath;
  private String dirName;
  private Optional<Date> timestamp = Optional.empty();

  public OldBackupDirectory(URI basePath, String dirName) {
    this.dirName = Objects.requireNonNull(dirName);
    this.basePath = Objects.requireNonNull(basePath);
    Matcher m = dirNamePattern.matcher(dirName);
    if (m.find()) {
      try {
        this.timestamp = Optional.of(new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT).parse(m.group(1)));
      } catch (ParseException e) {
        this.timestamp = Optional.empty();
      }
    }
  }

  public URI getPath() {
    return this.basePath.resolve(dirName);
  }

  public String getDirName() {
    return dirName;
  }

  public Optional<Date> getTimestamp() {
    return timestamp;
  }

  @Override
  public int compareTo(OldBackupDirectory that) {
    if(this.timestamp.isPresent() && that.timestamp.isPresent()) {
      return that.timestamp.get().compareTo(this.timestamp.get());
    }
    // Use absolute value of path in case the time-stamp is missing on either side.
    return that.getPath().compareTo(this.getPath());
  }
}
