package io.github.hcoona.directory_manifest;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public class ComputeFileChecksumTask extends ComputeChecksumTask {
  public ComputeFileChecksumTask(
      ManifestFileVisitor manifestFileVisitor,
      ComputeDirectoryChecksumTask parent,
      Path path, BasicFileAttributes attrs) {
    super(manifestFileVisitor, parent, path, attrs);
  }

  @Override
  protected String doCall() throws Exception {
    try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
      return manifestFileVisitor.getDigest().digestAsHex(is);
    }
  }
}
