CREATE TABLE IF NOT EXISTS migration_env_profile (
  profile_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Profile ID',
  env_key VARCHAR(64) NOT NULL COMMENT 'Unique environment key',
  display_name VARCHAR(128) NOT NULL COMMENT 'Display name',
  region VARCHAR(64) NOT NULL COMMENT 'Genesys Cloud region',
  client_id VARCHAR(256) NOT NULL COMMENT 'Genesys Cloud client id',
  client_secret_enc VARCHAR(1024) NOT NULL COMMENT 'Encrypted client secret',
  enabled CHAR(1) NOT NULL DEFAULT 'Y' COMMENT 'Y=enabled, N=disabled',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_by VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  update_by VARCHAR(64) DEFAULT '' COMMENT 'Updater',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (profile_id),
  UNIQUE KEY uk_migration_env_profile_env_key (env_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Genesys Cloud environment profiles';

CREATE TABLE IF NOT EXISTS migration_env_mapping (
  mapping_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mapping ID',
  source_env_key VARCHAR(64) NOT NULL COMMENT 'Source environment key',
  target_env_key VARCHAR(64) NOT NULL COMMENT 'Target environment key',
  sync_mode VARCHAR(64) NOT NULL DEFAULT 'WITH_DEPENDENCIES' COMMENT 'Sync mode',
  enabled CHAR(1) NOT NULL DEFAULT 'Y' COMMENT 'Y=enabled, N=disabled',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_by VARCHAR(64) DEFAULT '' COMMENT 'Creator',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  update_by VARCHAR(64) DEFAULT '' COMMENT 'Updater',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (mapping_id),
  UNIQUE KEY uk_migration_env_mapping_pair (source_env_key, target_env_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Genesys Cloud environment mapping';