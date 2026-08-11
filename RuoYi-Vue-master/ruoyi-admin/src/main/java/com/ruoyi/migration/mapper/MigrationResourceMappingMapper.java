package com.ruoyi.migration.mapper;

import java.util.List;

import com.ruoyi.migration.domain.MigrationResourceMappingEntity;

public interface MigrationResourceMappingMapper {

    MigrationResourceMappingEntity selectById(Long mappingId);

    MigrationResourceMappingEntity selectByPairAndName(MigrationResourceMappingEntity query);

    List<MigrationResourceMappingEntity> selectList(MigrationResourceMappingEntity query);

    int insert(MigrationResourceMappingEntity mapping);

    int update(MigrationResourceMappingEntity mapping);

    int deleteById(Long mappingId);
}