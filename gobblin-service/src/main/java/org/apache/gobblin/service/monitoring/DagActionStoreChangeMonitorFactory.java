/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gobblin.service.monitoring;

import java.util.Objects;

import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;

import org.apache.gobblin.runtime.api.DagActionStore;
import org.apache.gobblin.runtime.spec_catalog.FlowCatalog;
import org.apache.gobblin.runtime.util.InjectionNames;
import org.apache.gobblin.service.modules.orchestration.DagManager;
import org.apache.gobblin.service.modules.orchestration.Orchestrator;
import org.apache.gobblin.util.ConfigUtils;


/**
 * A factory implementation that returns a {@link DagActionStoreChangeMonitor} instance.
 */
@Slf4j
public class DagActionStoreChangeMonitorFactory implements Provider<DagActionStoreChangeMonitor> {
  static final String DAG_ACTION_STORE_CHANGE_MONITOR_NUM_THREADS_KEY = "numThreads";

  private final Config config;
  private DagManager dagManager;
  private FlowCatalog flowCatalog;
  private Orchestrator orchestrator;
  private boolean isMultiActiveSchedulerEnabled;

  @Inject
  public DagActionStoreChangeMonitorFactory(Config config, DagActionStore dagActionStore, DagManager dagManager,
      FlowCatalog flowCatalog, Orchestrator orchestrator,
      @Named(InjectionNames.MULTI_ACTIVE_SCHEDULER_ENABLED) boolean isMultiActiveSchedulerEnabled) {
    this.config = Objects.requireNonNull(config);
    this.dagManager = dagManager;
    this.flowCatalog = flowCatalog;
    this.orchestrator = orchestrator;
    this.isMultiActiveSchedulerEnabled = isMultiActiveSchedulerEnabled;
  }

  private DagActionStoreChangeMonitor createDagActionStoreMonitor()
    throws ReflectiveOperationException {
    Config dagActionStoreChangeConfig = this.config.getConfig(DagActionStoreChangeMonitor.DAG_ACTION_CHANGE_MONITOR_PREFIX);
    log.info("DagActionStore will be initialized with config {}", dagActionStoreChangeConfig);

    String topic = ""; // Pass empty string because we expect underlying client to dynamically determine the Kafka topic
    int numThreads = ConfigUtils.getInt(dagActionStoreChangeConfig, DAG_ACTION_STORE_CHANGE_MONITOR_NUM_THREADS_KEY, 5);

    return new DagActionStoreChangeMonitor(topic, dagActionStoreChangeConfig, this.dagManager, numThreads, flowCatalog,
        orchestrator, isMultiActiveSchedulerEnabled);
  }

  @Override
  public DagActionStoreChangeMonitor get() {
    try {
      return createDagActionStoreMonitor();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to initialize DagActionStoreMonitor due to ", e);
    }
  }
}
