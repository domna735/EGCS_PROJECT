package com.ruoyi.migration.model;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class MigrationStateStore {

    private static final ConcurrentHashMap<String, MigrationJobContext> JOBS = new ConcurrentHashMap<String, MigrationJobContext>();

    private MigrationStateStore() {
    }

    public static MigrationJobContext register(JobDescriptor job) {
        MigrationJobContext context = new MigrationJobContext();
        context.setJob(job);
        context.setSourceEnv(job.getSourceEnv());
        context.setTargetEnv(job.getTargetEnv());
        JOBS.put(job.getJobId(), context);
        return context;
    }

    public static MigrationJobContext get(String jobId) {
        return JOBS.get(jobId);
    }

    public static Collection<MigrationJobContext> all() {
        return JOBS.values();
    }
}