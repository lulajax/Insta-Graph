package com.lulajax.instagraph.dto;

import java.util.List;

/**
 * 共同标记的帖子信息
 */
public class CoTaggedPostInfo {
    private String shortCode;
    private Long takenAt;
    private List<String> taggedSeeds;  // 在该帖子中被标记的种子博主列表

    public CoTaggedPostInfo() {
    }

    public CoTaggedPostInfo(String shortCode, Long takenAt, List<String> taggedSeeds) {
        this.shortCode = shortCode;
        this.takenAt = takenAt;
        this.taggedSeeds = taggedSeeds;
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
}

