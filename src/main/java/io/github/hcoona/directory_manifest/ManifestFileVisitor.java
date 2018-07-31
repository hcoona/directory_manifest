package io.github.hcoona.directory_manifest;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Stack;
import java.util.concurrent.ScheduledExecutorService;

class ManifestFileVisitor extends SimpleFileVisitor<Path> {
  private static final Logger LOG =
      LoggerFactory.getLogger(ManifestFileVisitor.class);

  private final DigestUtils digest;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Stack<ComputeDirectoryChecksumTask> computeDirectoryChecksumTaskStack =
      new Stack<>();

  public ManifestFileVisitor(DigestUtils digest,
      ScheduledExecutorService scheduledExecutorService) {
    this.digest = digest;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Override
  public FileVisitResult preVisitDirectory(
      Path dir, BasicFileAttributes attrs) throws IOException {
    LOG.info(dir.toString());

    ComputeDirectoryChecksumTask parentTask;
    if (computeDirectoryChecksumTaskStack.empty()) {
      parentTask = null;
    } else {
      parentTask = computeDirectoryChecksumTaskStack.peek();
    }
    ComputeDirectoryChecksumTask task = new ComputeDirectoryChecksumTask(
        scheduledExecutorService, digest,
        dir, attrs, new HashSet<>(), parentTask);
    computeDirectoryChecksumTaskStack.push(task);

    return super.preVisitDirectory(dir, attrs);
  }

  @Override
  public FileVisitResult postVisitDirectory(
      Path dir, IOException exc) throws IOException {
    if (exc == null) {
      LOG.info(dir.toString());
    }
    return super.postVisitDirectory(dir, exc);
  }

  @Override
  public FileVisitResult visitFile(
      Path file, BasicFileAttributes attrs) throws IOException {
    LOG.info(file.toString());

    ComputeDirectoryChecksumTask parentTask =
        computeDirectoryChecksumTaskStack.peek();
    ComputeFileChecksumTask task = new ComputeFileChecksumTask(
        scheduledExecutorService, digest, file, attrs, parentTask);
    scheduledExecutorService.submit(task);

    return super.visitFile(file, attrs);
  }
}
