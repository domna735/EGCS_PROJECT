package com.ruoyi.migration.model;

import java.util.List;

public class JobDescriptor {

    private String jobId;                    // Job ID
    private String sourceEnv;                // venvA
    private String targetEnv;                // venvB

    private List<ResourceDescriptor> selectedResources;

    private String workspacePath;            // /data/workspace/job_xxx/
    private String status;                   // INIT / PLAN_READY / PLAN_RUNNING / APPLY_RUNNING / APPLY_DONE

    // Terraform provider credentials
    private String targetEnvRegion;
    private String targetEnvClientId;
    private String targetEnvClientSecret;

    public JobDescriptor() {}

    // Getter / Setter
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

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

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetEnvRegion() {
        return targetEnvRegion;
    }

    public void setTargetEnvRegion(String targetEnvRegion) {
        this.targetEnvRegion = targetEnvRegion;
    }

    public String getTargetEnvClientId() {
        return targetEnvClientId;
    }

    public void setTargetEnvClientId(String targetEnvClientId) {
        this.targetEnvClientId = targetEnvClientId;
    }

    public String getTargetEnvClientSecret() {
        return targetEnvClientSecret;
    }

    public void setTargetEnvClientSecret(String targetEnvClientSecret) {
        this.targetEnvClientSecret = targetEnvClientSecret;
    }
}
