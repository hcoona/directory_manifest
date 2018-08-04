package io.github.hcoona.directory_manifest;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ComputeDirectoryChecksumTask extends ComputeChecksumTask {
  private final Set<ComputeChecksumTask> dependencies = new HashSet<>();
  private boolean noFurtherDependency = false;

  public ComputeDirectoryChecksumTask(
      ManifestFileVisitor manifestFileVisitor,
      ComputeDirectoryChecksumTask parent,
      Path path, BasicFileAttributes attrs) {
    super(manifestFileVisitor, parent, path, attrs);
  }

  public void addDependency(ComputeChecksumTask computeChecksumTask) {
    synchronized (dependencies) {
      if (noFurtherDependency) {
        throw new IllegalStateException("Cannot add dependency " + getPath()
            + " -> " + computeChecksumTask.getPath());
      } else {
        dependencies.add(computeChecksumTask);
      }
    }
  }

  public void setNoFurtherDependency() {
    synchronized (dependencies) {
      noFurtherDependency = true;
    }
  }

  public boolean isReady() {
    synchronized (dependencies) {
      return noFurtherDependency
          && dependencies.stream().allMatch(ComputeChecksumTask::isFinished);
    }
  }

  @Override
  protected String doCall() throws Exception {
    synchronized (dependencies) {
      if (!noFurtherDependency) {
        throw new IllegalStateException("Cannot calculate checksum for "
            + "directory before all its dependencies added.");
      }
      if (!dependencies.stream().allMatch(ComputeChecksumTask::isFinished)) {
        throw new IllegalStateException("Cannot calculate checksum for "
            + "directory before all its dependencies calculated.");
      }

      return manifestFileVisitor.getDigest().digestAsHex(Arrays.toString(
          dependencies.stream().map(t -> t.checksum).toArray()));
    }
  }
}
