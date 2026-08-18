package com.ruoyi.migration.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.migration.domain.MigrationResourceMappingEntity;
import com.ruoyi.migration.mapper.MigrationResourceMappingMapper;

@Service
public class MigrationResourceMappingService {

    private final MigrationResourceMappingMapper mappingMapper;

    public MigrationResourceMappingService(MigrationResourceMappingMapper mappingMapper) {
        this.mappingMapper = mappingMapper;
    }

    public List<MigrationResourceMappingEntity> list(MigrationResourceMappingEntity query) {
        return mappingMapper.selectList(query == null ? new MigrationResourceMappingEntity() : query);
    }

    public MigrationResourceMappingEntity save(MigrationResourceMappingEntity mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("mapping 不能為空");
        }
        if (StringUtils.isEmpty(mapping.getSourceEnvKey()) || StringUtils.isEmpty(mapping.getTargetEnvKey()) || StringUtils.isEmpty(mapping.getResourceType()) || StringUtils.isEmpty(mapping.getResourceName())) {
            throw new IllegalArgumentException("sourceEnvKey、targetEnvKey、resourceType、resourceName 不能為空");
        }

        MigrationResourceMappingEntity existing = mappingMapper.selectByPairAndName(mapping);
        if (existing == null) {
            mapping.setSyncStatus(StringUtils.isEmpty(mapping.getSyncStatus()) ? "MAPPED" : mapping.getSyncStatus());
            mappingMapper.insert(mapping);
            return mapping;
        }

        mapping.setMappingId(existing.getMappingId());
        if (StringUtils.isEmpty(mapping.getSyncStatus())) {
            mapping.setSyncStatus(existing.getSyncStatus());
        }
        mappingMapper.update(mapping);
        return mapping;
    }

    public int delete(Long mappingId) {
        if (mappingId == null) {
            return 0;
        }
        return mappingMapper.deleteById(mappingId);
    }
}