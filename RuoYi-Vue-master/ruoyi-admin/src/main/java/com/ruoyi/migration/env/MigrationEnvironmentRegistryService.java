package com.ruoyi.migration.env;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.migration.domain.MigrationEnvironmentProfileEntity;
import com.ruoyi.migration.mapper.MigrationEnvironmentProfileMapper;
import com.ruoyi.migration.support.MigrationSecretCrypto;

@Service
public class MigrationEnvironmentRegistryService {

    private final MigrationEnvironmentProfileMapper profileMapper;
    private final MigrationSecretCrypto secretCrypto;

    public MigrationEnvironmentRegistryService(MigrationEnvironmentProfileMapper profileMapper, @Value("${migration.crypto.secret:${token.secret:egcs-migration-default-secret}}") String secretKey) {
        this.profileMapper = profileMapper;
        this.secretCrypto = new MigrationSecretCrypto(secretKey);
    }

    public List<MigrationEnvironmentProfile> listProfiles() {
        List<MigrationEnvironmentProfileEntity> entities = profileMapper.selectList(new MigrationEnvironmentProfileEntity());
        List<MigrationEnvironmentProfile> profiles = new ArrayList<>();
        for (MigrationEnvironmentProfileEntity entity : entities) {
            profiles.add(toDto(entity, false));
        }
        return profiles;
    }

    public MigrationEnvironmentProfile getProfile(String envKey) {
        if (StringUtils.isEmpty(envKey)) {
            return null;
        }
        MigrationEnvironmentProfileEntity entity = profileMapper.selectByEnvKey(envKey);
        if (entity == null) {
            return null;
        }
        return toDto(entity, false);
    }

    public MigrationEnvironmentProfile requireProfile(String envKey) {
        MigrationEnvironmentProfile profile = getProfile(envKey);
        if (profile == null) {
            throw new IllegalArgumentException("找不到環境設定: " + envKey);
        }
        if (!profile.isEnabled()) {
            throw new IllegalStateException("環境設定已停用: " + envKey);
        }
        return profile;
    }

    public MigrationEnvironmentProfile saveProfile(MigrationEnvironmentProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile 不能為空");
        }
        if (StringUtils.isEmpty(profile.getEnvKey())) {
            throw new IllegalArgumentException("envKey 不能為空");
        }

        MigrationEnvironmentProfileEntity existing = profileMapper.selectByEnvKey(profile.getEnvKey());
        MigrationEnvironmentProfileEntity entity = existing == null ? new MigrationEnvironmentProfileEntity() : existing;
        entity.setEnvKey(profile.getEnvKey());
        entity.setDisplayName(profile.getDisplayName());
        entity.setRegion(profile.getRegion());
        entity.setClientId(profile.getClientId());
        entity.setEnabled(profile.isEnabled() ? "Y" : "N");
        entity.setRemark(profile.getDisplayName());
        if (StringUtils.isNotEmpty(profile.getClientSecret())) {
            entity.setClientSecretEnc(secretCrypto.encrypt(profile.getClientSecret()));
        }
        if (existing == null) {
            profileMapper.insert(entity);
        } else {
            profile.setProfileId(existing.getProfileId());
            entity.setProfileId(existing.getProfileId());
            if (StringUtils.isEmpty(profile.getClientSecret())) {
                entity.setClientSecretEnc(existing.getClientSecretEnc());
            }
            profileMapper.update(entity);
        }
        profile.setProfileId(entity.getProfileId());
        profile.setClientSecret("");
        return profile;
    }

    public int deleteProfile(String envKey) {
        MigrationEnvironmentProfileEntity existing = profileMapper.selectByEnvKey(envKey);
        if (existing == null) {
            return 0;
        }
        return profileMapper.deleteById(existing.getProfileId());
    }

    public List<String> listEnabledEnvKeys() {
        List<String> envKeys = new ArrayList<>();
        for (MigrationEnvironmentProfile profile : listProfiles()) {
            if (profile.isEnabled()) {
                envKeys.add(profile.getEnvKey());
            }
        }
        return envKeys;
    }

    private MigrationEnvironmentProfile toDto(MigrationEnvironmentProfileEntity entity, boolean includeSecret) {
        MigrationEnvironmentProfile profile = new MigrationEnvironmentProfile();
        BeanUtils.copyProperties(entity, profile);
        profile.setEnabled("Y".equalsIgnoreCase(entity.getEnabled()));
        profile.setClientSecret(includeSecret ? secretCrypto.decrypt(entity.getClientSecretEnc()) : "");
        return profile;
    }
}