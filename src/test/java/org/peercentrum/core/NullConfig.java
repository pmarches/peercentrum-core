package org.peercentrum.core;

import java.io.File;

public class NullConfig extends TopLevelConfig{
  @Override
  public File getFileRelativeFromConfigDirectory(String fileName) {
    return null;
  }
}