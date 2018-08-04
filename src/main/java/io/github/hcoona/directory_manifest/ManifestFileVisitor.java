package io.github.hcoona.directory_manifest;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

public class ManifestFileVisitor extends SimpleFileVisitor<Path> {
  private static final Logger LOG =
      LoggerFactory.getLogger(ManifestFileVisitor.class);
  private static final DateTimeFormatter ISO_8601_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");

  private final DigestUtils digest;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Stack<List<CompletableFuture<String>>> taskListStack =
      new Stack<>();
  private final ManualResetEvent event;

  public ManifestFileVisitor(DigestUtils digest,
      ScheduledExecutorService scheduledExecutorService,
      ManualResetEvent event) {
    this.digest = digest;
    this.scheduledExecutorService = scheduledExecutorService;
    this.event = event;
  }

  @Override
  public FileVisitResult preVisitDirectory(
      Path dir, BasicFileAttributes attrs) throws IOException {
    LOG.info(dir.toString());

    taskListStack.add(new ArrayList<>());

    return super.preVisitDirectory(dir, attrs);
  }

  @Override
  public FileVisitResult postVisitDirectory(
      Path dir, IOException exc) throws IOException {
    if (exc == null) {
      LOG.info(dir.toString());
    }

    CompletableFuture<String> task;
    List<CompletableFuture<String>> taskList = taskListStack.pop();
    if (taskList.isEmpty()) {
      task = CompletableFuture.supplyAsync(() -> digest.digestAsHex(""));
    } else {
      CompletableFuture<?>[] arr = new CompletableFuture<?>[taskList.size()];
      taskList.toArray(arr);
      task = CompletableFuture
          .allOf(arr)
          .thenApplyAsync(ignored -> {
            String checksum = digest.digestAsHex(
                Arrays.toString(taskList.stream().map(t -> {
                  try {
                    return t.get();
                  } catch (InterruptedException e) {
                    LOG.warn("Interrupted before the calculation finished");
                    return null;
                  } catch (ExecutionException e) {
                    LOG.error("Execution failed");
                    return null;
                  }
                }).toArray()));
            BasicFileAttributeView attrsView =
                Files.getFileAttributeView(dir, BasicFileAttributeView.class);
            BasicFileAttributes attrs = null;
            try {
              attrs = attrsView.readAttributes();
              LOG.info(String.join(",", Arrays.asList(
                  "\"" + dir.toString() + "\"",
                  "DIRECTORY",
                  formatFileTime(attrs.creationTime()),
                  formatFileTime(attrs.lastModifiedTime()),
                  String.valueOf(attrs.size()),
                  checksum)));
            } catch (IOException e) {
              LOG.error("Failed to read attributes for directory", e);
            }
            return checksum;
          }, scheduledExecutorService);
    }

    if (taskListStack.empty()) {
      task.thenRun(() -> {
        LOG.info("The last task finished.");
        scheduledExecutorService.shutdown();
        event.set();
      });
    } else {
      taskListStack.peek().add(task);
    }

    return super.postVisitDirectory(dir, exc);
  }

  @Override
  public FileVisitResult visitFile(
      Path file, BasicFileAttributes attrs) throws IOException {
    LOG.info(file.toString());

    CompletableFuture<String> task = CompletableFuture.supplyAsync(
        () -> {
          String checksum = null;
          try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
            checksum = digest.digestAsHex(is);
          } catch (IOException e) {
            LOG.error("Cannot read file " + file, e);
          }
          LOG.info(String.join(",", Arrays.asList(
              "\"" + file.toString() + "\"",
              "FILE",
              formatFileTime(attrs.creationTime()),
              formatFileTime(attrs.lastModifiedTime()),
              String.valueOf(attrs.size()),
              checksum)));
          return checksum;
        },
        scheduledExecutorService);
    taskListStack.peek().add(task);

    return super.visitFile(file, attrs);
  }

  private static String formatFileTime(FileTime fileTime) {
    long cTime = fileTime.toMillis();
    ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
    return ISO_8601_FORMATTER.format(t);
  }
}
