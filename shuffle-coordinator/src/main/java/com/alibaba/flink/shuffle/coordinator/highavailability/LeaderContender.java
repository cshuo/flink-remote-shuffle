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

package com.alibaba.flink.shuffle.coordinator.highavailability;

import java.util.UUID;

/**
 * Interface which has to be implemented to take part in the leader election process of the {@link
 * LeaderElectionService}.
 *
 * <p>This class is copied from Apache Flink
 * (org.apache.flink.runtime.leaderelection.LeaderContender).
 */
public interface LeaderContender {

    /**
     * Callback method which is called by the {@link LeaderElectionService} upon selecting this
     * instance as the new leader. The method is called with the new leader session ID.
     *
     * @param leaderSessionID New leader session ID.
     */
    void grantLeadership(UUID leaderSessionID);

    /**
     * Callback method which is called by the {@link LeaderElectionService} upon revoking the
     * leadership of a former leader. This might happen in case that multiple contenders have been
     * granted leadership.
     */
    void revokeLeadership();

    /**
     * Callback method which is called by {@link LeaderElectionService} in case of an error in the
     * service thread.
     */
    void handleError(Throwable throwable);

    /**
     * Returns the description of the {@link LeaderContender} for logging purposes.
     *
     * @return Description of this contender.
     */
    default String getDescription() {
        return "LeaderContender: " + getClass().getSimpleName();
    }
}
