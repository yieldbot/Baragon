package com.hubspot.baragon.managers;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonServiceStatus;

@Singleton
public class StatusManager {
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final LeaderLatch leaderLatch;
  private final AtomicLong workerLastStart;
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public StatusManager(BaragonRequestDatastore requestDatastore,
                       BaragonStateDatastore stateDatastore,
                       @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                       @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStart,
                       @Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState) {
    this.requestDatastore = requestDatastore;
    this.stateDatastore = stateDatastore;
    this.leaderLatch = leaderLatch;
    this.workerLastStart = workerLastStart;
    this.connectionState = connectionState;
  }

  public BaragonServiceStatus getServiceStatus() {
    final ConnectionState currentConnectionState = connectionState.get();
    final String connectionStateString = currentConnectionState == null ? "UNKNOWN" : currentConnectionState.name();
    final int globalStateNodeSize = stateDatastore.getGlobalStateSize();
    final long workerLagMs = System.currentTimeMillis() - workerLastStart.get();

    return new BaragonServiceStatus(leaderLatch.hasLeadership(), requestDatastore.getQueuedRequestCount(), workerLagMs, connectionStateString, globalStateNodeSize);
  }
}