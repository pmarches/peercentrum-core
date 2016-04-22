package org.peercentrum.core;

import java.io.File;

public class NullConfig extends TopLevelConfig{
  @Override
  public File getFile(String fileName) {
    return null;
  }
}