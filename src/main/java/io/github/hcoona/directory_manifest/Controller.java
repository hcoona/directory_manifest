package io.github.hcoona.directory_manifest;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.nio.file.attribute.FileTime;
import java.security.Security;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;

class Controller {
  private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

  private static final DateTimeFormatter ISO_8601_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'");
  private static final DigestUtils digest;

  static {
    Security.addProvider(new BouncyCastleProvider());
    digest = new DigestUtils("WHIRLPOOL");
  }

  private final Path dirPath;

  Controller(FileSystem fs, String dir) {
    this.dirPath = fs.getPath(dir);
  }

  public void run() throws IOException {
    ManifestFileVisitor manifestFileVisitor = new ManifestFileVisitor();
    Files.walkFileTree(
        dirPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE, manifestFileVisitor);
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
      String checksum = digest.digestAsHex(file.toFile());
      LOG.info(String.join(",", Arrays.asList(
          file.toString(),
          formatFileTime(attrs.creationTime()),
          formatFileTime(attrs.lastModifiedTime()),
          String.valueOf(attrs.size()),
          checksum)));
      return super.visitFile(file, attrs);
    }
  }

  private static String formatFileTime(FileTime fileTime) {
    long cTime = fileTime.toMillis();
    ZonedDateTime t = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC"));
    return ISO_8601_FORMATTER.format(t);
  }
}
