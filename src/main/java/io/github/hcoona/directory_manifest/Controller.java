package io.github.hcoona.directory_manifest;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

class Controller {
  private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
  private static final DigestUtils digest = new DigestUtils("MD5");

  private final Path dirPath;
  private final ScheduledExecutorService scheduledExecutorService;
  private final ManualResetEvent event = new ManualResetEvent(false);

  Controller(FileSystem fs, String dir) {
    this.dirPath = fs.getPath(dir);
    if (!Files.isDirectory(dirPath)) {
      throw new IllegalArgumentException("dir " + dir + " is not a directory");
    }

    ThreadFactory tf = new ThreadFactoryBuilder()
        .setNameFormat("File hash calculate thread #%d")
        .setDaemon(true)
        .build();
    this.scheduledExecutorService =
        new LogErrorScheduledThreadPoolExecutor(6, tf);
  }

  public void run() throws IOException, InterruptedException {
    ManifestFileVisitor manifestFileVisitor =
        new ManifestFileVisitor(digest, scheduledExecutorService, event);
    Files.walkFileTree(
        dirPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE, manifestFileVisitor);
    event.waitOne();
    LOG.warn("Signal received.");
    if (scheduledExecutorService.awaitTermination(15, TimeUnit.SECONDS)) {
      LOG.warn("Finished successfully");
    } else {
      LOG.error("Timed out before all calculation finished");
      LOG.info("Not finished tasks: " + scheduledExecutorService.shutdownNow());
    }
  }
}
