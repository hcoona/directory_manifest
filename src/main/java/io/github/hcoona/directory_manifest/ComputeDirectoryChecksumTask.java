package io.github.hcoona.directory_manifest;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ComputeDirectoryChecksumTask extends ComputeChecksumTask {
  private final Set<CompletableFuture<String>> dependencies = new HashSet<>();

  public ComputeDirectoryChecksumTask(
      ManifestFileVisitor manifestFileVisitor,
      Path path, BasicFileAttributes attrs) {
    super(manifestFileVisitor, path, attrs);
  }

  public void addDependency(CompletableFuture<String> future) {
    synchronized (dependencies) {
      dependencies.add(future);
    }
  }

  @Override
  protected String doGet() throws Exception {
    synchronized (dependencies) {
      CompletableFuture<?>[] tasks = new CompletableFuture<?>[dependencies.size()];
      CompletableFuture
          .allOf(dependencies.toArray(tasks))
          .thenApplyAsync(ignored -> {

          })
      manifestFileVisitor.getScheduledExecutorService()
      return manifestFileVisitor.getDigest().digestAsHex(Arrays.toString(
          dependencies.stream().map(t -> t.checksum).toArray()));
    }
  }
}
