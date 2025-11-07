package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class FollowingResponse extends BaseApiResponse {

    private FollowingDataWrapper data;

    @Data
    public static class FollowingDataWrapper {
        private FollowingData data;
    }

    @Data
    public static class FollowingData {
        private List<FollowingItem> items;
        @JsonProperty("pagination_token")
        private String paginationToken;
    }

    @Data
    public static class FollowingItem {
        @JsonProperty("full_name")
        private String fullName;
        private String id;
        @JsonProperty("is_private")
        private Boolean isPrivate;
        @JsonProperty("is_verified")
        private Boolean isVerified;
        private String username;
    }
}
