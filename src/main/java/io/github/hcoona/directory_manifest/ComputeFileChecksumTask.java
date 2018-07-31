package io.github.hcoona.directory_manifest;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ScheduledExecutorService;

public class ComputeFileChecksumTask extends ComputeChecksumTask {
  public ComputeFileChecksumTask(
      ScheduledExecutorService scheduledExecutorService,
      DigestUtils digestUtils,
      Path path, BasicFileAttributes basicFileAttributes,
      ComputeDirectoryChecksumTask parent) {
    super(scheduledExecutorService, digestUtils,
        path, basicFileAttributes, parent);
  }

  @Override
  protected String invokeCall() throws IllegalStateException, IOException {
    if (Files.isDirectory(path)) {
      throw new IllegalStateException("Cannot deal with directory");
    } else {
      try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
        return digestUtils.digestAsHex(is);
      }
    }
  }
}
