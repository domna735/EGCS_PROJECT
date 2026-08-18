package com.ruoyi.migration.model;

public class ResourceDescriptor {

    private String id;               // GC resource ID
    private String name;             // Resource name
    private String type;             // FLOW / QUEUE / PROMPT / USER / ROLE
    private String terraformType;    // genesyscloud_flow / genesyscloud_routing_queue / ...

    // 可擴展欄位（例如 Flow filepath、Queue media settings）
    private String extra;

    public ResourceDescriptor() {}

    public ResourceDescriptor(String id, String name, String type, String terraformType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.terraformType = terraformType;
    }

    // Getter / Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTerraformType() {
        return terraformType;
    }

    public void setTerraformType(String terraformType) {
        this.terraformType = terraformType;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
