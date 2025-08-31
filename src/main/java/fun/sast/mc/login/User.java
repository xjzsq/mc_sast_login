package fun.sast.mc.login;

import java.util.UUID;

public class User {
    private UUID uuid;
    private String feishu_id;
    private String feishu_name;
    private String sast_id;
    private String sast_name;

    public User(UUID uuid, String feishu_id, String feishu_name, String sast_id, String sast_name) {
        this.uuid = uuid;
        this.feishu_id = feishu_id;
        this.feishu_name = feishu_name;
        this.sast_id = sast_id;
        this.sast_name = sast_name;
    }

    public User() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getFeishu_id() {
        return feishu_id;
    }

    public void setFeishu_id(String feishu_id) {
        this.feishu_id = feishu_id;
    }

    public String getFeishu_name() {
        return feishu_name;
    }

    public void setFeishu_name(String feishu_name) {
        this.feishu_name = feishu_name;
    }

    public String getSast_id() {
        return sast_id;
    }

    public void setSast_id(String sast_id) {
        this.sast_id = sast_id;
    }

    public String getSast_name() {
        return sast_name;
    }

    public void setSast_name(String sast_name) {
        this.sast_name = sast_name;
    }
}
