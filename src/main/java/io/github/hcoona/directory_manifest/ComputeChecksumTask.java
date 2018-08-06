package io.github.hcoona.directory_manifest;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public abstract class ComputeChecksumTask
    implements Supplier<String>, Comparable<ComputeChecksumTask> {
  private static final Logger LOG =
      LoggerFactory.getLogger(ComputeChecksumTask.class);
  private static final DateTimeFormatter ISO_8601_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");

  protected final ManifestFileVisitor manifestFileVisitor;
  protected final Path path;
  protected final BasicFileAttributes attrs;

  public ComputeChecksumTask(
      ManifestFileVisitor manifestFileVisitor,
      Path path, BasicFileAttributes attrs) {
    this.manifestFileVisitor = manifestFileVisitor;
    this.path = path;
    this.attrs = attrs;
  }

  @Override
  public int compareTo(ComputeChecksumTask o) {
    return path.compareTo(o.path);
  }

  @Override
  public String get() {
    try {
      final String checksum = doGet();
      LOG.info(String.join(",", Arrays.asList(
          "\"" + path.toString() + "\"",
          Files.isDirectory(path) ? "DIRECTORY" : "FILE",
          formatFileTime(attrs.creationTime()),
          formatFileTime(attrs.lastModifiedTime()),
          String.valueOf(attrs.size()),
          checksum)));
      return checksum;
    } catch (Exception e) {
      LOG.error("Failed to calculate checksum for " + path, e);
      return null;
    }
  }

  protected abstract String doGet() throws Exception;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("path", path)
        .add("type", Files.isDirectory(path) ? "DIRECTORY" : "FILE")
        .toString();
  }

  private static String formatFileTime(FileTime fileTime) {
    long cTime = fileTime.toMillis();
    ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
    return ISO_8601_FORMATTER.format(t);
  }
}
