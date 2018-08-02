package io.github.hcoona.directory_manifest;

import com.google.common.base.MoreObjects;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.concurrent.ScheduledExecutorService;

public abstract class ComputeChecksumTask
    implements Callable<String>, Comparable<ComputeChecksumTask> {

  public enum State {
    CREATED, INITED, SUBMITTED, RUNNING, FINISHED
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(ComputeChecksumTask.class);

  private static final DateTimeFormatter ISO_8601_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");

  private final ScheduledExecutorService scheduledExecutorService;
  protected final DigestUtils digestUtils;
  protected final Path path;
  protected final BasicFileAttributes attributes;
  private final ComputeDirectoryChecksumTask parent;

  private State state = State.CREATED;
  private String checksum = null;

  public ComputeChecksumTask(ScheduledExecutorService scheduledExecutorService,
      DigestUtils digestUtils,
      Path path, BasicFileAttributes attributes,
      ComputeDirectoryChecksumTask parent) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.digestUtils = digestUtils;
    this.path = path;
    this.attributes = attributes;
    this.parent = parent;

    if (parent != null) {
      synchronized (parent) {
        parent.addDependency(this);
      }
    }
  }

  public State getState() {
    return state;
  }

  void setState(State state) {
    this.state = state;
  }

  public boolean isFinished() {
    return state == State.FINISHED;
  }

  public String getChecksum() {
    return checksum;
  }

  @Override
  public final String call() throws Exception {
    state = State.RUNNING;
    checksum = invokeCall();
    afterCall();
    state = State.FINISHED;
    if (parent == null) {
      scheduledExecutorService.shutdown();
      LOG.warn("ROOT computed, SHUTTING DOWN: " + toString()
          + " dependencies: "
          + ((ComputeDirectoryChecksumTask) this).dependencies.toString());
    } else {
      // TODO: Could add dependency after submitted.
      synchronized (parent) {
        if (parent.readyCall() && parent.getState() == State.INITED) {
          parent.setState(State.SUBMITTED);
          scheduledExecutorService.submit(parent);
        }
      }
    }
    return checksum;
  }

  protected void afterCall() {
    LOG.warn(String.join(",", Arrays.asList(
        path.toString(),
        Files.isDirectory(path) ? "DIRECTORY" : "FILE",
        formatFileTime(attributes.creationTime()),
        formatFileTime(attributes.lastModifiedTime()),
        String.valueOf(attributes.size()),
        checksum)));
  }

  protected abstract String invokeCall() throws Exception;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("path", path)
        .add("digestAlgorithm", digestUtils.getMessageDigest().getAlgorithm())
        .add("state", state)
        .add("type", Files.isDirectory(path) ? "DIRECTORY" : "FILE")
        .add("isRoot", parent == null)
        .toString();
  }

  @Override
  public int compareTo(ComputeChecksumTask o) {
    return path.compareTo(o.path);
  }

  private static String formatFileTime(FileTime fileTime) {
    long cTime = fileTime.toMillis();
    ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
    return ISO_8601_FORMATTER.format(t);
  }
}
