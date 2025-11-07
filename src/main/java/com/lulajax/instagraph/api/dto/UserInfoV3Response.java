package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserInfoV3Response extends BaseApiResponse {

    private UserInfoV3Data data;

    @Data
    public static class UserInfoV3Data {
        private UserInfoV3 data;
    }

    @Data
    public static class UserInfoV3 {
        private String country;

        @JsonProperty("date_joined")
        private String dateJoined;

        @JsonProperty("date_joined_as_timestamp")
        private Long dateJoinedAsTimestamp;

        @JsonProperty("date_verified")
        private String dateVerified;

        @JsonProperty("date_verified_as_timestamp")
        private Long dateVerifiedAsTimestamp;

        @JsonProperty("former_usernames")
        private Integer formerUsernames;

        private Long id;

        @JsonProperty("is_verified")
        private Boolean isVerified;

        private String username;
        private String bio;

        @JsonProperty("full_name")
        private String fullName;
    }
}
