package com.lulajax.instagraph.dto;

/**
 * 连接的种子博主信息
 */
public class ConnectedSeedInfo {
    private String username;
    private Integer coTagCount;  // 与该博主共同被标记的帖子数

    public ConnectedSeedInfo() {
    }

    public ConnectedSeedInfo(String username, Integer coTagCount) {
        this.username = username;
        this.coTagCount = coTagCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getCoTagCount() {
        return coTagCount;
    }

    public void setCoTagCount(Integer coTagCount) {
        this.coTagCount = coTagCount;
    }
}

