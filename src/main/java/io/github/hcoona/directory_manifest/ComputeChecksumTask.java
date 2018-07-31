package io.github.hcoona.directory_manifest;

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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ComputeChecksumTask implements Callable<String> {

  public enum State {
    CREATED, SUBMITTED, RUNNING, FINISHED
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
      parent.getDependencies().add(this);
    }
  }

  public State getState() {
    return state;
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
    } else {
      synchronized (parent) {
        if (parent.readyCall() && parent.getState() == State.CREATED) {
          Future<?> future = scheduledExecutorService.submit(parent);
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

  protected boolean readyCall() {
    return true;
  }

  private static String formatFileTime(FileTime fileTime) {
    long cTime = fileTime.toMillis();
    ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
    return ISO_8601_FORMATTER.format(t);
  }
}
