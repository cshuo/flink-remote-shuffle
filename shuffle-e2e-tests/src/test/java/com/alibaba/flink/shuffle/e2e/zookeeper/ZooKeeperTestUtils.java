/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.flink.shuffle.e2e.zookeeper;

import com.alibaba.flink.shuffle.common.config.Configuration;
import com.alibaba.flink.shuffle.coordinator.highavailability.zookeeper.ZooKeeperUtils;
import com.alibaba.flink.shuffle.core.config.HighAvailabilityOptions;
import com.alibaba.flink.shuffle.e2e.utils.LogErrorHandler;

import org.apache.flink.configuration.AkkaOptions;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.runtime.jobmanager.HighAvailabilityMode;

import org.apache.flink.shaded.curator4.org.apache.curator.framework.CuratorFramework;
import org.apache.flink.shaded.curator4.org.apache.curator.utils.ZKPaths;
import org.apache.flink.shaded.zookeeper3.org.apache.zookeeper.KeeperException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.alibaba.flink.shuffle.common.utils.CommonUtils.checkNotNull;

/** ZooKeeper test utilities. */
public class ZooKeeperTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperTestUtils.class);

    /**
     * Creates a configuration to operate in {@link HighAvailabilityMode#ZOOKEEPER}.
     *
     * @param zooKeeperQuorum ZooKeeper quorum to connect to
     * @param fsStateHandlePath Base path for file system state backend (for checkpoints and
     *     recovery)
     * @return A new configuration to operate in {@link HighAvailabilityMode#ZOOKEEPER}.
     */
    public static org.apache.flink.configuration.Configuration createZooKeeperHAConfigForFlink(
            String zooKeeperQuorum, String fsStateHandlePath) {

        return configureZooKeeperHAForFlink(
                new org.apache.flink.configuration.Configuration(),
                zooKeeperQuorum,
                fsStateHandlePath);
    }

    /**
     * Sets all necessary configuration keys to operate in {@link HighAvailabilityMode#ZOOKEEPER}.
     *
     * @param config Configuration to use
     * @param zooKeeperQuorum ZooKeeper quorum to connect to
     * @param fsStateHandlePath Base path for file system state backend (for checkpoints and
     *     recovery)
     * @return The modified configuration to operate in {@link HighAvailabilityMode#ZOOKEEPER}.
     */
    public static org.apache.flink.configuration.Configuration configureZooKeeperHAForFlink(
            org.apache.flink.configuration.Configuration config,
            String zooKeeperQuorum,
            String fsStateHandlePath) {

        checkNotNull(config);
        checkNotNull(zooKeeperQuorum);
        checkNotNull(fsStateHandlePath);

        // ZooKeeper recovery mode
        config.setString(
                org.apache.flink.configuration.HighAvailabilityOptions.HA_MODE, "ZOOKEEPER");
        config.setString(
                org.apache.flink.configuration.HighAvailabilityOptions.HA_ZOOKEEPER_QUORUM,
                zooKeeperQuorum);

        int connTimeout = 60000;
        config.setInteger(
                org.apache.flink.configuration.HighAvailabilityOptions.ZOOKEEPER_CONNECTION_TIMEOUT,
                connTimeout);
        config.setInteger(
                org.apache.flink.configuration.HighAvailabilityOptions.ZOOKEEPER_SESSION_TIMEOUT,
                connTimeout);

        // File system state backend
        config.setString(CheckpointingOptions.STATE_BACKEND, "FILESYSTEM");
        config.setString(
                CheckpointingOptions.CHECKPOINTS_DIRECTORY, fsStateHandlePath + "/checkpoints");
        config.setString(
                org.apache.flink.configuration.HighAvailabilityOptions.HA_STORAGE_PATH,
                fsStateHandlePath + "/recovery");

        config.setString(AkkaOptions.ASK_TIMEOUT, "100 s");

        return config;
    }

    public static Configuration createZooKeeperHAConfig(String zooKeeperQuorum) {
        return configureZooKeeperHA(new Configuration(), zooKeeperQuorum);
    }

    public static Configuration configureZooKeeperHA(Configuration config, String zooKeeperQuorum) {

        checkNotNull(config);
        checkNotNull(zooKeeperQuorum);

        // ZooKeeper recovery mode
        config.setString(HighAvailabilityOptions.HA_MODE, "ZOOKEEPER");
        config.setString(HighAvailabilityOptions.HA_ZOOKEEPER_QUORUM, zooKeeperQuorum);

        int connTimeout = 60;
        config.setDuration(
                HighAvailabilityOptions.ZOOKEEPER_CONNECTION_TIMEOUT,
                Duration.ofSeconds(connTimeout));
        config.setDuration(
                HighAvailabilityOptions.ZOOKEEPER_SESSION_TIMEOUT, Duration.ofSeconds(connTimeout));

        return config;
    }

    public static CuratorFramework createZKClientForFlink(
            org.apache.flink.configuration.Configuration configuration) {
        return org.apache.flink.runtime.util.ZooKeeperUtils.startCuratorFramework(
                configuration, LogErrorHandler.INSTANCE);
    }

    public static CuratorFramework createZKClientForRemoteShuffle(Configuration configuration) {
        return ZooKeeperUtils.startCuratorFramework(configuration);
    }

    /**
     * Deletes all ZNodes under the root node.
     *
     * @throws Exception If the ZooKeeper operation fails
     */
    public static void deleteAll(CuratorFramework client) throws Exception {
        final String path = "/" + client.getNamespace();

        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                ZKPaths.deleteChildren(client.getZookeeperClient().getZooKeeper(), path, false);
                return;
            } catch (org.apache.zookeeper.KeeperException.NoNodeException e) {
                // that seems all right. if one of the children we want to delete is
                // actually already deleted, that's fine.
                return;
            } catch (KeeperException.ConnectionLossException e) {
                // Keep retrying
                Thread.sleep(100);
            }
        }

        throw new Exception(
                "Could not clear the ZNodes under "
                        + path
                        + ". ZooKeeper is not in "
                        + "a clean state.");
    }
}