package com.ruoyi.migration.builder;

import com.ruoyi.migration.model.ResourceDescriptor;

public class TfTemplateGenerator {

    /**
     * Provider block
     */
    public String providerBlock() {
        return
            "terraform {\n" +
            "  required_providers {\n" +
            "    genesyscloud = {\n" +
            "      source  = \"mypurecloud/genesyscloud\"\n" +
            "      version = \"1.84.2\"\n" +
            "    }\n" +
            "  }\n" +
            "}\n\n" +
            "provider \"genesyscloud\" {\n" +
            "  region        = var.region\n" +
            "  client_id     = var.client_id\n" +
            "  client_secret = var.client_secret\n" +
            "}\n\n";
    }

    /**
     * Division block（所有環境都需要）
     */
    public String divisionBlock() {
        return
            "data \"genesyscloud_auth_division\" \"home\" {\n" +
            "  name = \"Home\"\n" +
            "}\n\n";
    }

    /**
     * Data block（讀取現有資源）
     */
    public String dataBlock(ResourceDescriptor r) {
        return
            "data \"" + r.getTerraformType() + "\" \"existing_" + r.getName() + "\" {\n" +
            "  name = \"" + r.getName() + "\"\n" +
            "}\n\n";
    }

    /**
     * Import block（Terraform 1.5+）
     */
    public String importBlock(ResourceDescriptor r) {
        return
            "import {\n" +
            "  to = " + r.getTerraformType() + "." + r.getName() + "\n" +
            "  id = data." + r.getTerraformType() + ".existing_" + r.getName() + ".id\n" +
            "}\n\n";
    }

    /**
     * Resource block（管理現有資源）
     */
    public String resourceBlock(ResourceDescriptor r) {
        return
            "resource \"" + r.getTerraformType() + "\" \"" + r.getName() + "\" {\n" +
            "  name = \"" + r.getName() + "\"\n" +
            "}\n\n";
    }

    /**
     * variables.tf
     */
    public String variablesTf() {
        return
            "variable \"region\" {}\n" +
            "variable \"client_id\" {}\n" +
            "variable \"client_secret\" {}\n";
    }

    /**
     * terraform.tfvars
     */
    public String tfVars(String region, String clientId, String clientSecret) {
        return
            "region        = \"" + region + "\"\n" +
            "client_id     = \"" + clientId + "\"\n" +
            "client_secret = \"" + clientSecret + "\"\n";
    }
}
