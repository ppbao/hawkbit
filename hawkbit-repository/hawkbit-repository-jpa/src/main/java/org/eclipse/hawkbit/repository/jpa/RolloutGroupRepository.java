/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The repository interface for the {@link RolloutGroup} model.
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface RolloutGroupRepository
        extends BaseEntityRepository<JpaRolloutGroup, Long>, JpaSpecificationExecutor<JpaRolloutGroup> {

    /**
     * Retrieves all {@link RolloutGroup} referring a specific rollout in the
     * order of creating them. ID ASC.
     * 
     * @param rollout
     *            the rollout the rolloutgroups belong to
     * @return the rollout groups belonging to a rollout ordered by ID ASC.
     */
    List<JpaRolloutGroup> findByRolloutOrderByIdAsc(final JpaRollout rollout);

    /**
     * Retrieves all {@link RolloutGroup} referring a specific rollout in a
     * specific {@link RolloutGroupStatus}.
     * 
     * @param rollout
     *            the rollout the rolloutgroup belong to
     * @param status
     *            the status of the rollout groups
     * @return the rollout groups belonging to a rollout in a specific status
     */
    List<JpaRolloutGroup> findByRolloutAndStatus(final Rollout rollout, final RolloutGroupStatus status);

    /**
     * Counts all {@link RolloutGroup} referring a specific rollout in specific
     * {@link RolloutGroupStatus}s. An in-clause statement does not work with
     * the spring-data, so this is specific usecase regarding to the
     * rollout-management to find out rolloutgroups which are in specific
     * states.
     * 
     * @param rollout
     *            the rollout the rolloutgroup belong to
     * @param rolloutGroupStatus1
     *            the status of the rollout groups
     * @param rolloutGroupStatus2
     *            the status of the rollout groups
     * @return the count of rollout groups belonging to a rollout in specific
     *         status
     */
    @Query("SELECT COUNT(r.id) FROM JpaRolloutGroup r WHERE r.rollout = :rollout and (r.status = :status1 or r.status = :status2)")
    Long countByRolloutAndStatusOrStatus(@Param("rollout") JpaRollout rollout,
            @Param("status1") RolloutGroupStatus rolloutGroupStatus1,
            @Param("status2") RolloutGroupStatus rolloutGroupStatus2);

    /**
     * Retrieves all {@link RolloutGroup} for a specific parent in a specific
     * status. Retrieves the child rolloutgroup for a specific status.
     * 
     * @param rolloutGroup
     *            the parent rolloutgroup
     * @param status
     *            the status of the rolloutgroups
     * @return The child {@link RolloutGroup}s in a specific status
     */
    List<JpaRolloutGroup> findByParentAndStatus(JpaRolloutGroup rolloutGroup, RolloutGroupStatus status);

    /**
     * Updates all {@link RolloutGroup#getStatus()} of children for given
     * parent.
     * 
     * @param parent
     *            the parent rolloutgroup
     * @param status
     *            the status of the rolloutgroups
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Query("UPDATE JpaRolloutGroup g SET g.status = :status WHERE g.parent = :parent")
    void setStatusForCildren(@Param("status") RolloutGroupStatus status, @Param("parent") RolloutGroup parent);

    /**
     * Retrieves all {@link RolloutGroup} for a specific rollout and status not
     * having ordered by ID DESC, latest top.
     * 
     * @param rollout
     *            the rollout the rolloutgroup belong to
     * @param notStatus
     *            the status which the rolloutgroup should not have
     * @return rolloutgroup referring to a rollout and not having a specific
     *         status ordered by ID DESC.
     */
    List<JpaRolloutGroup> findByRolloutAndStatusNotOrderByIdDesc(JpaRollout rollout, RolloutGroupStatus notStatus);

    /**
     * Retrieves all {@link RolloutGroup} for a specific rollout.
     * 
     * @param rolloutId
     *            the ID of the rollout to find the rollout groups
     * @param page
     *            the page request to sort, limit the result
     * @return a page of found {@link RolloutGroup} or {@code empty}.
     */
    Page<JpaRolloutGroup> findByRolloutId(final Long rolloutId, Pageable page);

}
