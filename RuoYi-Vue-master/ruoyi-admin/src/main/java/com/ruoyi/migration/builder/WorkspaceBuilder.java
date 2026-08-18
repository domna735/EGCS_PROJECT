package com.ruoyi.migration.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.migration.model.JobDescriptor;
import com.ruoyi.migration.model.ResourceDescriptor;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorkspaceBuilder {

    private static final String WORKSPACE_ROOT = "/data/workspace/";

    private final TfFileWriter writer = new TfFileWriter();
    private final TfTemplateGenerator template = new TfTemplateGenerator();

    /**
     * 建立 Workspace（Phase 2 核心）
     */
    public String buildWorkspace(JobDescriptor job) throws Exception {

        // 1. 建立 workspace 目錄
        String workspacePath = WORKSPACE_ROOT + "job_" + job.getJobId() + "/";
        writer.ensureDirectory(workspacePath);

        // 2. 寫 selectedResources.json
        writeSelectedResources(job, workspacePath);

        // 3. 寫 variables.tf
        writer.write(workspacePath + "variables.tf", template.variablesTf());

        // 4. 寫 terraform.tfvars
        writer.write(
                workspacePath + "terraform.tfvars",
                template.tfVars(
                        job.getTargetEnvRegion(),
                        job.getTargetEnvClientId(),
                        job.getTargetEnvClientSecret()
                )
        );

        // 5. 寫 main.tf（Provider + Division + Data + Import + Resource）
        StringBuilder tf = new StringBuilder();

        tf.append(template.providerBlock());
        tf.append(template.divisionBlock());

        for (ResourceDescriptor r : job.getSelectedResources()) {
            tf.append(template.dataBlock(r));
            tf.append(template.importBlock(r));
            tf.append(template.resourceBlock(r));
        }

        writer.write(workspacePath + "main.tf", tf.toString());

        return workspacePath;
    }

    private void writeSelectedResources(JobDescriptor job, String workspacePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(
                Path.of(workspacePath + "selectedResources.json").toFile(),
                job.getSelectedResources()
        );
    }
}
