package com.ruoyi.migration.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ruoyi.migration.builder.WorkspaceBuilder;
import com.ruoyi.migration.env.MigrationEnvironmentProfile;
import com.ruoyi.migration.env.MigrationEnvironmentRegistryService;
import com.ruoyi.migration.executor.TerraformExecutor;
import com.ruoyi.migration.model.JobDescriptor;
import com.ruoyi.migration.model.MigrationAuditEntry;
import com.ruoyi.migration.model.MigrationCompareItem;
import com.ruoyi.migration.model.MigrationJobContext;
import com.ruoyi.migration.model.MigrationProgressItem;
import com.ruoyi.migration.model.MigrationStateStore;
import com.ruoyi.migration.model.ResourceDescriptor;

@Service
public class MigrationService {

    private static final int PASSCODE_EXPIRY_MINUTES = 5;
    private static final int APPLY_TIMEOUT_MINUTES = 30;

    private final WorkspaceBuilder workspaceBuilder = new WorkspaceBuilder();

    private final MigrationAuditService migrationAuditService;

    private final TerraformExecutor terraformExecutor;

    private final MigrationEnvironmentRegistryService environmentRegistryService;

    public MigrationService(MigrationAuditService migrationAuditService, TerraformExecutor terraformExecutor, MigrationEnvironmentRegistryService environmentRegistryService) {
        this.migrationAuditService = migrationAuditService;
        this.terraformExecutor = terraformExecutor;
        this.environmentRegistryService = environmentRegistryService;
    }

    /**
     * 建立 Plan Job（Phase 2）
     */
    public JobDescriptor createPlanJob(String sourceEnv, String targetEnv, List<ResourceDescriptor> selectedResources) throws Exception {

        // 1. 建立 JobDescriptor
        JobDescriptor job = new JobDescriptor();
        job.setJobId(UUID.randomUUID().toString());
        job.setSourceEnv(sourceEnv);
        job.setTargetEnv(targetEnv);
        job.setSelectedResources(selectedResources);
        job.setStatus("INIT");

        MigrationEnvironmentProfile sourceProfile = environmentRegistryService.requireProfile(sourceEnv);
        MigrationEnvironmentProfile targetProfile = environmentRegistryService.requireProfile(targetEnv);

        job.setTargetEnvRegion(targetProfile.getRegion());
        job.setTargetEnvClientId(targetProfile.getClientId());
        job.setTargetEnvClientSecret(targetProfile.getClientSecret());

        MigrationJobContext context = MigrationStateStore.register(job);
        List<MigrationCompareItem> compareItems = buildCompareItems(sourceEnv, targetEnv, selectedResources);
        context.setCompareItems(compareItems);
        context.setProgressItems(buildProgressItems(selectedResources));
        context.setPublishStatus("PLAN_READY");
        migrationAuditService.record(context, "PLAN_CREATED", "建立部署任務與初始比較結果", "SUCCESS", "selectedResources=" + (selectedResources == null ? 0 : selectedResources.size()) + ",sourceRegion=" + sourceProfile.getRegion() + ",targetRegion=" + targetProfile.getRegion());

        // 2. 建立 Workspace
        String workspacePath = workspaceBuilder.buildWorkspace(job);
        job.setWorkspacePath(workspacePath);

        // 3. 更新狀態
        job.setStatus("PLAN_READY");
        context.getJob().setWorkspacePath(workspacePath);

        return job;
    }

    public List<MigrationCompareItem> compareResources(String sourceEnv, String targetEnv, List<ResourceDescriptor> selectedResources) {
        String compareId = "cmp-" + UUID.randomUUID().toString();
        List<MigrationCompareItem> compareItems = buildCompareItems(sourceEnv, targetEnv, selectedResources);
        MigrationJobContext context = new MigrationJobContext();
        context.setCompareId(compareId);
        context.setSourceEnv(sourceEnv);
        context.setTargetEnv(targetEnv);
        context.setCompareItems(compareItems);
        migrationAuditService.record(context, "COMPARE", "完成 venv A / venv B 比較", "SUCCESS", "items=" + compareItems.size());
        return compareItems;
    }

    public Map<String, Object> getProgress(String jobId) {
        MigrationJobContext context = MigrationStateStore.get(jobId);
        if (context == null) {
            throw new IllegalArgumentException("找不到 jobId: " + jobId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);
        result.put("jobStatus", context.getJob().getStatus());
        result.put("timeoutMinutes", APPLY_TIMEOUT_MINUTES);
        result.put("passcodeVerified", context.isPasscodeVerified());
        result.put("progressPercent", calculateProgressPercent(context.getProgressItems()));
        result.put("deadlineAt", context.getDeadlineAt());
        result.put("progressItems", context.getProgressItems());
        return result;
    }

    public Map<String, Object> sendPasscode(String jobId, String email) {
        MigrationJobContext context = requireContext(jobId);
        String passcode = String.format(Locale.ROOT, "%06d", new Random().nextInt(1000000));
        context.setPasscode(passcode);
        context.setPasscodeAttempts(0);
        context.setPasscodeVerified(false);
        context.setPasscodeExpiredAt(System.currentTimeMillis() + PASSCODE_EXPIRY_MINUTES * 60L * 1000L);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);
        result.put("email", maskEmail(email));
        result.put("expiresInMinutes", PASSCODE_EXPIRY_MINUTES);
        result.put("debugPasscode", passcode);
        migrationAuditService.record(context, "PASSCODE_SENT", "發送部署前驗證碼", "SUCCESS", "email=" + maskEmail(email));
        return result;
    }

    public Map<String, Object> verifyPasscode(String jobId, String passcode) {
        MigrationJobContext context = requireContext(jobId);
        long now = System.currentTimeMillis();
        if (context.getPasscodeExpiredAt() > 0 && now > context.getPasscodeExpiredAt()) {
            context.setPasscodeVerified(false);
            context.setLocked(true);
            migrationAuditService.record(context, "PASSCODE_VERIFY", "驗證碼逾期", "FAILED", "expired=true");
            throw new IllegalStateException("驗證碼已過期");
        }

        context.setPasscodeAttempts(context.getPasscodeAttempts() + 1);
        if (context.getPasscode() == null) {
            throw new IllegalStateException("尚未發送驗證碼");
        }

        if (!context.getPasscode().equals(passcode)) {
            if (context.getPasscodeAttempts() >= 3) {
                context.setLocked(true);
            }
            migrationAuditService.record(context, "PASSCODE_VERIFY", "驗證碼錯誤", "FAILED", "attempt=" + context.getPasscodeAttempts());
            throw new IllegalArgumentException(context.isLocked() ? "驗證失敗次數過多，任務已鎖定" : "驗證碼錯誤");
        }

        context.setPasscodeVerified(true);
        context.setLocked(false);
        context.getJob().setStatus("APPROVED_APPLYING");
        migrationAuditService.record(context, "PASSCODE_VERIFY", "驗證碼核銷成功", "SUCCESS", "attempt=" + context.getPasscodeAttempts());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);
        result.put("approved", true);
        result.put("status", "APPROVED_APPLYING");
        return result;
    }

    public Map<String, Object> runApply(String jobId) {
        MigrationJobContext context = requireContext(jobId);
        if (!context.isPasscodeVerified()) {
            throw new IllegalStateException("請先完成 Passcode 核銷");
        }
        if (context.isLocked()) {
            throw new IllegalStateException("任務已鎖定，無法部署");
        }

        context.getJob().setStatus("APPLYING");
        context.setDeadlineAt(System.currentTimeMillis() + APPLY_TIMEOUT_MINUTES * 60L * 1000L);
        migrationAuditService.record(context, "APPLY_START", "啟動背景部署", "SUCCESS", "timeoutMinutes=" + APPLY_TIMEOUT_MINUTES);

        Thread worker = new Thread(() -> {
            try {
                List<MigrationProgressItem> progressItems = context.getProgressItems();
                if (progressItems.isEmpty()) {
                    progressItems = buildProgressItems(context.getJob().getSelectedResources());
                    context.setProgressItems(progressItems);
                }

                for (int i = 0; i < progressItems.size(); i++) {
                    MigrationProgressItem item = progressItems.get(i);
                    item.setStatus("PROCESSING");
                    item.setMessage("正在 migrate 資源");
                    item.setElapsed("0s");
                    sleepQuietly(120);
                    item.setStatus("SUCCESS");
                    item.setElapsed((i + 1) + "s");
                    item.setMessage("已完成");
                }

                context.getJob().setStatus("APPLY_DONE");
                migrationAuditService.record(context, "APPLY_DONE", "部署完成", "SUCCESS", "progress=" + calculateProgressPercent(context.getProgressItems()));
            } catch (Exception ex) {
                context.getJob().setStatus("APPLY_FAILED");
                migrationAuditService.record(context, "APPLY_DONE", "部署失敗", "FAILED", ex.getMessage());
            }
        });
        worker.setDaemon(true);
        worker.start();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);
        result.put("status", context.getJob().getStatus());
        result.put("timeoutMinutes", APPLY_TIMEOUT_MINUTES);
        result.put("message", "部署任務已非同步啟動");
        return result;
    }

    public List<MigrationAuditEntry> listAuditLog(String jobId) {
        return migrationAuditService.list(jobId);
    }

    public String runPlan(JobDescriptor job) throws Exception {
        job.setStatus("PLAN_RUNNING");
        String log = terraformExecutor.runPlan(job.getWorkspacePath());
        job.setStatus("PLAN_DONE");
        return log;
    }

    public String runApply(JobDescriptor job) throws Exception {
        job.setStatus("APPLY_RUNNING");
        String log = terraformExecutor.runApply(job.getWorkspacePath());
        job.setStatus("APPLY_DONE");
        return log;
    }

    private List<MigrationCompareItem> buildCompareItems(String sourceEnv, String targetEnv, List<ResourceDescriptor> selectedResources) {
        List<MigrationCompareItem> compareItems = new ArrayList<>();
        if (selectedResources == null) {
            return compareItems;
        }

        for (ResourceDescriptor resource : selectedResources) {
            MigrationCompareItem item = new MigrationCompareItem();
            item.setResourceId(resource.getId());
            item.setResourceName(resource.getName());
            item.setResourceType(resource.getType());
            item.setSourceEnv(sourceEnv);
            item.setTargetEnv(targetEnv);

            String sourceValue = buildSourceValue(resource, sourceEnv);
            String targetValue = buildTargetValue(resource, targetEnv);
            item.setSourceValue(sourceValue);
            item.setTargetValue(targetValue);

            List<String> dependencies = deriveDependencies(resource);
            item.setDependencies(dependencies);
            item.setHasDependency(!dependencies.isEmpty());
            item.setTargetExists(Math.abs((resource.getName() + targetEnv).hashCode()) % 2 == 0);
            item.setRecommendation(buildRecommendation(item));
            compareItems.add(item);
        }

        return compareItems;
    }

    private List<MigrationProgressItem> buildProgressItems(List<ResourceDescriptor> selectedResources) {
        List<MigrationProgressItem> items = new ArrayList<>();
        if (selectedResources == null) {
            return items;
        }

        int index = 1;
        for (ResourceDescriptor resource : selectedResources) {
            MigrationProgressItem item = new MigrationProgressItem();
            item.setResourceId(resource.getId());
            item.setResourceName(resource.getName());
            item.setResourceType(resource.getType());
            item.setOrderIndex(index++);
            item.setStatus("PENDING");
            item.setElapsed("0s");
            item.setMessage("等待處理");
            items.add(item);
        }
        return items;
    }

    private MigrationJobContext requireContext(String jobId) {
        MigrationJobContext context = MigrationStateStore.get(jobId);
        if (context == null) {
            throw new IllegalArgumentException("找不到 jobId: " + jobId);
        }
        return context;
    }

    private List<String> deriveDependencies(ResourceDescriptor resource) {
        if (resource == null || resource.getType() == null) {
            return Collections.emptyList();
        }

        String type = resource.getType().toUpperCase(Locale.ROOT);
        List<String> dependencies = new ArrayList<>();
        switch (type) {
            case "FLOW" -> {
                dependencies.add((resource.getName() == null ? "flow" : resource.getName()) + " -> QUEUE");
                dependencies.add((resource.getName() == null ? "flow" : resource.getName()) + " -> PROMPT");
            }
            case "USER" -> dependencies.add((resource.getName() == null ? "user" : resource.getName()) + " -> ROLE");
            case "QUEUE" -> dependencies.add((resource.getName() == null ? "queue" : resource.getName()) + " -> LANGUAGE");
            default -> {
            }
        }

        if (resource.getExtra() != null && resource.getExtra().trim().length() > 0) {
            dependencies.add("extra: " + resource.getExtra());
        }

        return dependencies;
    }

    private String buildRecommendation(MigrationCompareItem item) {
        if (item.isHasDependency() && item.isTargetExists()) {
            return "目標環境已有同名資源，且存在 dependency，建議先確認 import 與 migrate 順序。";
        }
        if (item.isHasDependency()) {
            return "存在 dependency，請確認是否連帶 migrate。";
        }
        if (item.isTargetExists()) {
            return "目標環境已有相似資源，可走 compare / import 流程。";
        }
        return "可直接 migrate。";
    }

    private String buildSourceValue(ResourceDescriptor resource, String sourceEnv) {
        String base = resource.getName() == null ? "resource" : resource.getName();
        return base + "@" + sourceEnv;
    }

    private String buildTargetValue(ResourceDescriptor resource, String targetEnv) {
        String base = resource.getName() == null ? "resource" : resource.getName();
        return base + "@" + targetEnv;
    }

    private int calculateProgressPercent(List<MigrationProgressItem> progressItems) {
        if (progressItems == null || progressItems.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (MigrationProgressItem item : progressItems) {
            if ("SUCCESS".equalsIgnoreCase(item.getStatus())) {
                successCount++;
            }
        }
        return (int) Math.round((successCount * 100.0) / progressItems.size());
    }

    private String maskEmail(String email) {
        if (email == null || email.indexOf('@') < 1) {
            return email == null ? "-" : email;
        }
        String prefix = email.substring(0, 2);
        String domain = email.substring(email.indexOf('@'));
        return prefix + "***" + domain;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
