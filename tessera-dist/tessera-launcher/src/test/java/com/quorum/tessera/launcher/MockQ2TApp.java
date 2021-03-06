package com.quorum.tessera.launcher;

import com.quorum.tessera.config.AppType;
import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.apps.TesseraApp;

public class MockQ2TApp implements TesseraApp {

  @Override
  public CommunicationType getCommunicationType() {
    return CommunicationType.REST;
  }

  @Override
  public AppType getAppType() {
    return AppType.Q2T;
  }
}
