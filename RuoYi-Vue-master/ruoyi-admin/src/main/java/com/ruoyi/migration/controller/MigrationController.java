package com.ruoyi.migration.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.migration.domain.MigrationResourceMappingEntity;
import com.ruoyi.migration.env.MigrationEnvironmentProfile;
import com.ruoyi.migration.env.MigrationEnvironmentRegistryService;
import com.ruoyi.migration.model.JobDescriptor;
import com.ruoyi.migration.model.ResourceDescriptor;
import com.ruoyi.migration.service.MigrationResourceMappingService;
import com.ruoyi.migration.service.MigrationService;

@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

    private final MigrationService migrationService;
    private final MigrationEnvironmentRegistryService environmentRegistryService;
    private final MigrationResourceMappingService mappingService;

    public MigrationController(MigrationService migrationService, MigrationEnvironmentRegistryService environmentRegistryService, MigrationResourceMappingService mappingService) {
        this.migrationService = migrationService;
        this.environmentRegistryService = environmentRegistryService;
        this.mappingService = mappingService;
    }

    /**
     * 建立 Plan Job（Phase 2）
     * 前端按下「Plan」後會呼叫這個 API
     */
    @PostMapping("/job/plan")
    public AjaxResult createPlanJob(@RequestBody PlanRequest request) {
        try {
            JobDescriptor job = migrationService.createPlanJob(
                    request.getSourceEnv(),
                    request.getTargetEnv(),
                    request.getSelectedResources()
            );

            return AjaxResult.success("PLAN_READY", job);
        } catch (Exception e) {
            return AjaxResult.error("建立 Plan Job 失敗：" + e.getMessage());
        }
    }

    @PostMapping("/resources/compare")
    public AjaxResult compareResources(@RequestBody CompareRequest request) {
        try {
            return AjaxResult.success("COMPARE_READY", migrationService.compareResources(
                    request.getSourceEnv(),
                    request.getTargetEnv(),
                    request.getSelectedResources()));
        } catch (Exception e) {
            return AjaxResult.error("比較資源失敗：" + e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}/progress")
    public AjaxResult getProgress(@PathVariable String jobId) {
        try {
            return AjaxResult.success(migrationService.getProgress(jobId));
        } catch (Exception e) {
            return AjaxResult.error("查詢進度失敗：" + e.getMessage());
        }
    }

    @PostMapping("/job/send-passcode")
    public AjaxResult sendPasscode(@RequestBody PasscodeRequest request) {
        try {
            return AjaxResult.success("PASSCODE_SENT", migrationService.sendPasscode(request.getJobId(), request.getEmail()));
        } catch (Exception e) {
            return AjaxResult.error("發送驗證碼失敗：" + e.getMessage());
        }
    }

    @PostMapping("/job/verify-passcode")
    public AjaxResult verifyPasscode(@RequestBody VerifyPasscodeRequest request) {
        try {
            return AjaxResult.success("PASSCODE_VERIFIED", migrationService.verifyPasscode(request.getJobId(), request.getPasscode()));
        } catch (Exception e) {
            return AjaxResult.error("驗證驗證碼失敗：" + e.getMessage());
        }
    }

    @PostMapping("/job/apply")
    public AjaxResult runApply(@RequestBody ApplyRequest request) {
        try {
            return AjaxResult.success("APPLY_STARTED", migrationService.runApply(request.getJobId()));
        } catch (Exception e) {
            return AjaxResult.error("啟動部署失敗：" + e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}/audit-log")
    public AjaxResult listAuditLog(@PathVariable String jobId) {
        try {
            return AjaxResult.success(migrationService.listAuditLog(jobId));
        } catch (Exception e) {
            return AjaxResult.error("查詢審計紀錄失敗：" + e.getMessage());
        }
    }

    @GetMapping("/env/profiles")
    public AjaxResult listEnvironmentProfiles() {
        try {
            return AjaxResult.success(environmentRegistryService.listProfiles());
        } catch (Exception e) {
            return AjaxResult.error("查詢環境設定失敗：" + e.getMessage());
        }
    }

    @GetMapping("/env/profiles/{envKey}")
    public AjaxResult getEnvironmentProfile(@PathVariable String envKey) {
        try {
            return AjaxResult.success(environmentRegistryService.requireProfile(envKey));
        } catch (Exception e) {
            return AjaxResult.error("查詢環境設定失敗：" + e.getMessage());
        }
    }

    @DeleteMapping("/env/profiles/{envKey}")
    public AjaxResult deleteEnvironmentProfile(@PathVariable String envKey) {
        try {
            return AjaxResult.success("PROFILE_DELETED", environmentRegistryService.deleteProfile(envKey));
        } catch (Exception e) {
            return AjaxResult.error("刪除環境設定失敗：" + e.getMessage());
        }
    }

    @PostMapping("/env/profiles")
    public AjaxResult saveEnvironmentProfile(@RequestBody MigrationEnvironmentProfile profile) {
        try {
            return AjaxResult.success("PROFILE_SAVED", environmentRegistryService.saveProfile(profile));
        } catch (Exception e) {
            return AjaxResult.error("儲存環境設定失敗：" + e.getMessage());
        }
    }

    @GetMapping("/resource-mappings")
    public AjaxResult listResourceMappings(MigrationResourceMappingEntity query) {
        try {
            return AjaxResult.success(mappingService.list(query));
        } catch (Exception e) {
            return AjaxResult.error("查詢資源映射失敗：" + e.getMessage());
        }
    }

    @PostMapping("/resource-mappings")
    public AjaxResult saveResourceMapping(@RequestBody MigrationResourceMappingEntity mapping) {
        try {
            return AjaxResult.success("MAPPING_SAVED", mappingService.save(mapping));
        } catch (Exception e) {
            return AjaxResult.error("儲存資源映射失敗：" + e.getMessage());
        }
    }

    @DeleteMapping("/resource-mappings/{mappingId}")
    public AjaxResult deleteResourceMapping(@PathVariable Long mappingId) {
        try {
            return AjaxResult.success("MAPPING_DELETED", mappingService.delete(mappingId));
        } catch (Exception e) {
            return AjaxResult.error("刪除資源映射失敗：" + e.getMessage());
        }
    }

    /**
     * Request Body 專用 DTO
     */
    public static class PlanRequest {
        private String sourceEnv;
        private String targetEnv;
        private List<ResourceDescriptor> selectedResources;

        public String getSourceEnv() {
            return sourceEnv;
        }

        public void setSourceEnv(String sourceEnv) {
            this.sourceEnv = sourceEnv;
        }

        public String getTargetEnv() {
            return targetEnv;
        }

        public void setTargetEnv(String targetEnv) {
            this.targetEnv = targetEnv;
        }

        public List<ResourceDescriptor> getSelectedResources() {
            return selectedResources;
        }

        public void setSelectedResources(List<ResourceDescriptor> selectedResources) {
            this.selectedResources = selectedResources;
        }
    }

    public static class CompareRequest {
        private String sourceEnv;
        private String targetEnv;
        private List<ResourceDescriptor> selectedResources;

        public String getSourceEnv() {
            return sourceEnv;
        }

        public void setSourceEnv(String sourceEnv) {
            this.sourceEnv = sourceEnv;
        }

        public String getTargetEnv() {
            return targetEnv;
        }

        public void setTargetEnv(String targetEnv) {
            this.targetEnv = targetEnv;
        }

        public List<ResourceDescriptor> getSelectedResources() {
            return selectedResources;
        }

        public void setSelectedResources(List<ResourceDescriptor> selectedResources) {
            this.selectedResources = selectedResources;
        }
    }

    public static class PasscodeRequest {
        private String jobId;
        private String email;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class VerifyPasscodeRequest {
        private String jobId;
        private String passcode;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getPasscode() {
            return passcode;
        }

        public void setPasscode(String passcode) {
            this.passcode = passcode;
        }
    }

    public static class ApplyRequest {
        private String jobId;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }
    }
}
