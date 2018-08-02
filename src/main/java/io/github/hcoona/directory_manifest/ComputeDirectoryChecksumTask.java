package io.github.hcoona.directory_manifest;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class ComputeDirectoryChecksumTask extends ComputeChecksumTask {
  final Set<ComputeChecksumTask> dependencies;

  public ComputeDirectoryChecksumTask(
      ScheduledExecutorService scheduledExecutorService,
      DigestUtils digestUtils,
      Path path, BasicFileAttributes basicFileAttributes,
      Set<ComputeChecksumTask> dependencies,
      ComputeDirectoryChecksumTask parent) {
    super(scheduledExecutorService, digestUtils,
        path, basicFileAttributes, parent);
    this.dependencies = dependencies;
  }

  public void addDependency(ComputeChecksumTask task) {
    dependencies.add(task);
  }

  public boolean readyCall() {
    return dependencies.stream().allMatch(ComputeChecksumTask::isFinished);
  }

  @Override
  protected String invokeCall() throws Exception {
    if (!Files.isDirectory(path)) {
      throw new IllegalStateException("Cannot deal with non-directory");
    } else {
      return digestUtils.digestAsHex(dependencies.stream()
          .map(ComputeChecksumTask::getChecksum)
          .collect(Collectors.joining("|")));
    }
  }
}
