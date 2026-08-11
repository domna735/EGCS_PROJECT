package com.ruoyi.migration.mapper;

import java.util.List;

import com.ruoyi.migration.domain.MigrationEnvironmentProfileEntity;

public interface MigrationEnvironmentProfileMapper {

    MigrationEnvironmentProfileEntity selectById(Long profileId);

    MigrationEnvironmentProfileEntity selectByEnvKey(String envKey);

    List<MigrationEnvironmentProfileEntity> selectList(MigrationEnvironmentProfileEntity query);

    int insert(MigrationEnvironmentProfileEntity profile);

    int update(MigrationEnvironmentProfileEntity profile);

    int deleteById(Long profileId);
}