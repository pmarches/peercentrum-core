package org.peercentrum.core;

import java.io.File;

import org.peercentrum.core.TopLevelConfig;

public class NullConfig extends TopLevelConfig{
  @Override
  public File getFile(String fileName) {
    return null;
  }
}