CREATE TABLE IF NOT EXISTS migration_resource_mapping (
  mapping_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mapping ID',
  source_env_key VARCHAR(64) NOT NULL COMMENT 'Source environment key',
  target_env_key VARCHAR(64) NOT NULL COMMENT 'Target environment key',
  resource_type VARCHAR(64) NOT NULL COMMENT 'Resource type',
  resource_name VARCHAR(255) NOT NULL COMMENT 'Resource name',
  source_guid VARCHAR(64) NOT NULL COMMENT 'Source GUID',
  target_guid VARCHAR(64) DEFAULT NULL COMMENT 'Target GUID',
  sync_status VARCHAR(32) NOT NULL DEFAULT 'MAPPED' COMMENT 'Sync status',
  last_sync_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Last sync time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_by VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  update_by VARCHAR(64) DEFAULT '' COMMENT 'Updater',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (mapping_id),
  UNIQUE KEY uk_mapping_pair_name (source_env_key, target_env_key, resource_type, resource_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EGCS cross org resource mapping table';

CREATE TABLE IF NOT EXISTS migration_snapshot (
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Snapshot ID',
  job_id VARCHAR(64) NOT NULL COMMENT 'Job ID',
  env_key VARCHAR(64) NOT NULL COMMENT 'Environment key',
  snapshot_type VARCHAR(32) NOT NULL COMMENT 'backup/snapshot/restore/merge',
  snapshot_path VARCHAR(500) NOT NULL COMMENT 'Snapshot path',
  snapshot_payload LONGTEXT DEFAULT NULL COMMENT 'Snapshot payload',
  create_by VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (snapshot_id),
  KEY idx_snapshot_job_env (job_id, env_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EGCS snapshot history';

CREATE TABLE IF NOT EXISTS migration_version_record (
  version_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Version ID',
  job_id VARCHAR(64) NOT NULL COMMENT 'Job ID',
  version_tag VARCHAR(128) NOT NULL COMMENT 'Version tag',
  source_commit VARCHAR(128) DEFAULT NULL COMMENT 'Source commit',
  target_commit VARCHAR(128) DEFAULT NULL COMMENT 'Target commit',
  version_payload LONGTEXT DEFAULT NULL COMMENT 'Version payload',
  create_by VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (version_id),
  UNIQUE KEY uk_version_tag (version_tag),
  KEY idx_version_job (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EGCS version trace records';