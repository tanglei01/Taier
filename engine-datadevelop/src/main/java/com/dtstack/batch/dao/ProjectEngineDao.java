/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.batch.dao;

import com.dtstack.batch.domain.ProjectEngine;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Reason:
 * Date: 2019/6/1
 * Company: www.dtstack.com
 *
 * @author xuchao
 */

public interface ProjectEngineDao {

    List<ProjectEngine> getByProjectId(@Param("projectId") long projectId);

    boolean insert(ProjectEngine projectEngine);

    ProjectEngine getByProjectAndEngineType(@Param("projectId") long projectId, @Param("engineType") Integer engineType);

    ProjectEngine getDefaultByProjectAndEngineType(@Param("projectId") long projectId, @Param("engineType") Integer engineType);

    List<ProjectEngine> listByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    List<Integer> getUsedEngineTypeList(@Param("projectId") Long projectId);

    ProjectEngine getByIdentityAndEngineTypeAndTenantId(@Param("identity") String identity, @Param("engineType") Integer engineType, @Param("tenantId") Long tenantId);

    List<ProjectEngine> getByIdentitysAndEngineType(@Param("engineType") Integer engineType, @Param("tenantId") Long tenantId);

    ProjectEngine getProjectByDb(@Param("engineIdentity") String engineIdentity, @Param("engineType") Integer engineType, @Param("tenantId") Long tenantId);

    ProjectEngine getByTenantIdAndEngineIdentity(@Param("tenantId") Long tenantId, @Param("engineIdentity") String engineIdentity, @Param("engineType") Integer engineType);

    ProjectEngine getByTenantIdAndProjectIdAndEngineType(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId, @Param("engineType") Integer engineType);

    List<ProjectEngine> getProjectListByUserId(@Param("engineType") Integer engineType, @Param("projectIds") Set<Long> projectIds);

    Integer deleteByProjectId(@Param("projectId") Long projectId, @Param("modifyUserId") Long modifyUserId);

    /**
     * 查询项目标识，项目 ID
     * @param projectIds
     * @param engineType
     * @return
     */
    List<ProjectEngine> listIdentityByProjectIdAndType(@Param("projectIds") List<Long> projectIds, @Param("engineType") Integer engineType);

}