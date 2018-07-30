package io.github.hcoona.directory_manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

class Controller {
  private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

  private final FileSystem fs;
  private final Path dirPath;

  Controller(FileSystem fs, String dir) {
    this.fs = fs;
    this.dirPath = fs.getPath(dir);
  }

  public void run() throws IOException {
    Files.walkFileTree(
        dirPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE, new ManifestFileVisitor());
  }

  class ManifestFileVisitor extends SimpleFileVisitor<Path> {
    @Override
    public FileVisitResult preVisitDirectory(
        Path dir, BasicFileAttributes attrs) throws IOException {
      LOG.info(dir.toString());
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
      return super.visitFile(file, attrs);
    }
  }
}
