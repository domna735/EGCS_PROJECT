package com.ruoyi.migration.model;

import java.util.ArrayList;
import java.util.List;

public class MigrationJobContext {

    private String compareId;
    private String sourceEnv;
    private String targetEnv;
    private JobDescriptor job;
    private List<MigrationCompareItem> compareItems = new ArrayList<MigrationCompareItem>();
    private List<MigrationProgressItem> progressItems = new ArrayList<MigrationProgressItem>();
    private List<MigrationAuditEntry> auditEntries = new ArrayList<MigrationAuditEntry>();
    private String passcode;
    private long passcodeExpiredAt;
    private int passcodeAttempts;
    private boolean passcodeVerified;
    private boolean locked;
    private long deadlineAt;
    private String publishStatus;

    public String getCompareId() {
        return compareId;
    }

    public void setCompareId(String compareId) {
        this.compareId = compareId;
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

    public JobDescriptor getJob() {
        return job;
    }

    public void setJob(JobDescriptor job) {
        this.job = job;
    }

    public String getJobId() {
        return job == null ? compareId : job.getJobId();
    }

    public List<MigrationCompareItem> getCompareItems() {
        return compareItems;
    }

    public void setCompareItems(List<MigrationCompareItem> compareItems) {
        this.compareItems = compareItems;
    }

    public List<MigrationProgressItem> getProgressItems() {
        return progressItems;
    }

    public void setProgressItems(List<MigrationProgressItem> progressItems) {
        this.progressItems = progressItems;
    }

    public List<MigrationAuditEntry> getAuditEntries() {
        return auditEntries;
    }

    public void setAuditEntries(List<MigrationAuditEntry> auditEntries) {
        this.auditEntries = auditEntries;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public long getPasscodeExpiredAt() {
        return passcodeExpiredAt;
    }

    public void setPasscodeExpiredAt(long passcodeExpiredAt) {
        this.passcodeExpiredAt = passcodeExpiredAt;
    }

    public int getPasscodeAttempts() {
        return passcodeAttempts;
    }

    public void setPasscodeAttempts(int passcodeAttempts) {
        this.passcodeAttempts = passcodeAttempts;
    }

    public boolean isPasscodeVerified() {
        return passcodeVerified;
    }

    public void setPasscodeVerified(boolean passcodeVerified) {
        this.passcodeVerified = passcodeVerified;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(long deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }
}