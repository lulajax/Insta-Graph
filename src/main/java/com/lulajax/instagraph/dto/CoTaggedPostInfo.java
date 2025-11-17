package com.lulajax.instagraph.dto;

import java.util.List;

/**
 * 共同标记的帖子信息
 */
public class CoTaggedPostInfo {
    private String shortCode;
    private Long takenAt;
    private List<String> taggedSeeds;  // 在该帖子中被标记的种子博主列表
    private List<String> allTaggedUsers; // 在该帖子中被标记的所有博主列表

    public CoTaggedPostInfo() {
    }

    public CoTaggedPostInfo(String shortCode, Long takenAt, List<String> taggedSeeds, List<String> allTaggedUsers) {
        this.shortCode = shortCode;
        this.takenAt = takenAt;
        this.taggedSeeds = taggedSeeds;
        this.allTaggedUsers = allTaggedUsers;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Long getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Long takenAt) {
        this.takenAt = takenAt;
    }

    public List<String> getTaggedSeeds() {
        return taggedSeeds;
    }

    public void setTaggedSeeds(List<String> taggedSeeds) {
        this.taggedSeeds = taggedSeeds;
    }

    public List<String> getAllTaggedUsers() {
        return allTaggedUsers;
    }

    public void setAllTaggedUsers(List<String> allTaggedUsers) {
        this.allTaggedUsers = allTaggedUsers;
    }
}

