package com.ruoyi.migration.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;
@Component
public class TerraformExecutor {

    /**
     * 執行 terraform init
     */
    public String runInit(String workspacePath) throws Exception {
        return runCommand(workspacePath, "terraform", "init");
    }

    /**
     * 執行 terraform plan
     */
    public String runPlan(String workspacePath) throws Exception {
        return runCommand(workspacePath, "terraform", "plan");
    }

    /**
     * 執行 terraform apply -auto-approve
     */
    public String runApply(String workspacePath) throws Exception {
        return runCommand(workspacePath, "terraform", "apply", "-auto-approve");
    }

    /**
     * 共用命令執行方法
     */
    private String runCommand(String workspacePath, String... command) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workspacePath));
        pb.redirectErrorStream(true); // stderr 合併到 stdout

        Process process = pb.start();

        StringBuilder log = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8")
        );

        String line;
        while ((line = reader.readLine()) != null) {
            log.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        log.append("\n[exitCode=").append(exitCode).append("]\n");

        return log.toString();
    }
}
