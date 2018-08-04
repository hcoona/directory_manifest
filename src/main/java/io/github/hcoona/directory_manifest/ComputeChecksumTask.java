package io.github.hcoona.directory_manifest;

import com.google.common.base.MoreObjects;
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
import java.util.concurrent.Callable;

public abstract class ComputeChecksumTask
    implements Callable<String>, Comparable<ComputeChecksumTask> {
  private static final Logger LOG =
      LoggerFactory.getLogger(ComputeChecksumTask.class);
  private static final DateTimeFormatter ISO_8601_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");

  protected final ManifestFileVisitor manifestFileVisitor;
  protected final ComputeDirectoryChecksumTask parent;
  protected final Path path;
  protected final BasicFileAttributes attrs;

  private final Object lockObject = new Object();
  protected boolean finished = false;
  protected String checksum = "";

  public ComputeChecksumTask(
      ManifestFileVisitor manifestFileVisitor,
      ComputeDirectoryChecksumTask parent,
      Path path, BasicFileAttributes attrs) {
    this.manifestFileVisitor = manifestFileVisitor;
    this.parent = parent;
    this.path = path;
    this.attrs = attrs;

    if (parent != null) {
      parent.addDependency(this);
    }
  }

  public Path getPath() {
    return path;
  }

  public boolean isFinished() {
    synchronized (lockObject) {
      return finished;
    }
  }

  @Override
  public int compareTo(ComputeChecksumTask o) {
    return path.compareTo(o.path);
  }

  @Override
  public synchronized String call() throws Exception {
    synchronized (lockObject) {
      checksum = doCall();
      finished = true;
    }
    LOG.info(String.join(",", Arrays.asList(
        "\"" + path.toString() + "\"",
        Files.isDirectory(path) ? "DIRECTORY" : "FILE",
        formatFileTime(attrs.creationTime()),
        formatFileTime(attrs.lastModifiedTime()),
        String.valueOf(attrs.size()),
        checksum)));
    return checksum;
  }

  protected abstract String doCall() throws Exception;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("path", path)
        .add("type", Files.isDirectory(path) ? "DIRECTORY" : "FILE")
        .add("isRoot", parent == null)
        .toString();
  }

  private static String formatFileTime(FileTime fileTime) {
    long cTime = fileTime.toMillis();
    ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
    return ISO_8601_FORMATTER.format(t);
  }
}
