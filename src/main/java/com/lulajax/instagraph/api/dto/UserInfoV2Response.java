package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserInfoV2Response extends BaseApiResponse {

    private UserInfoV2Data data;

    @Data
    public static class UserInfoV2Data {
        private boolean status;
        @JsonProperty("full_name")
        private String fullName;
        @JsonProperty("is_memorialized")
        private boolean isMemorialized;
        @JsonProperty("is_private")
        private boolean isPrivate;
        @JsonProperty("has_story_archive")
        private Object hasStoryArchive;
        private String username;
        @JsonProperty("is_regulated_c18")
        private boolean isRegulatedC18;
        @JsonProperty("regulated_news_in_locations")
        private List<Object> regulatedNewsInLocations;
        @JsonProperty("text_post_app_badge_label")
        private Object textPostAppBadgeLabel;
        @JsonProperty("show_text_post_app_badge")
        private Object showTextPostAppBadge;
        private String pk;
        @JsonProperty("live_broadcast_visibility")
        private Object liveBroadcastVisibility;
        @JsonProperty("live_broadcast_id")
        private Object liveBroadcastId;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
        @JsonProperty("hd_profile_pic_url_info")
        private HdProfilePicUrlInfo hdProfilePicUrlInfo;
        @JsonProperty("is_unpublished")
        private boolean isUnpublished;
        @JsonProperty("mutual_followers_count")
        private Object mutualFollowersCount;
        @JsonProperty("profile_context_links_with_user_ids")
        private Object profileContextLinksWithUserIds;
        @JsonProperty("biography_with_entities")
        private BiographyWithEntities biographyWithEntities;
        @JsonProperty("account_badges")
        private List<Object> accountBadges;
        @JsonProperty("bio_links")
        private List<BioLink> bioLinks;
        @JsonProperty("external_lynx_url")
        private String externalLynxUrl;
        @JsonProperty("external_url")
        private String externalUrl;
        @JsonProperty("has_chaining")
        private Object hasChaining;
        @JsonProperty("fbid_v2")
        private String fbidV2;
        @JsonProperty("supervision_info")
        private Object supervisionInfo;
        @JsonProperty("interop_messaging_user_fbid")
        private String interopMessagingUserFbid;
        @JsonProperty("account_type")
        private int accountType;
        private String biography;
        @JsonProperty("is_embeds_disabled")
        private boolean isEmbedsDisabled;
        @JsonProperty("show_account_transparency_details")
        private boolean showAccountTransparencyDetails;
        @JsonProperty("is_verified")
        private boolean isVerified;
        @JsonProperty("is_professional_account")
        private Object isProfessionalAccount;
        @JsonProperty("follower_count")
        private int followerCount;
        @JsonProperty("address_street")
        private Object addressStreet;
        @JsonProperty("city_name")
        private Object cityName;
        @JsonProperty("is_business")
        private boolean isBusiness;
        private Object zip;
        private String category;
        @JsonProperty("should_show_category")
        private boolean shouldShowCategory;
        @JsonProperty("transparency_label")
        private Object transparencyLabel;
        @JsonProperty("transparency_product")
        private Object transparencyProduct;
        @JsonProperty("following_count")
        private int followingCount;
        @JsonProperty("media_count")
        private int mediaCount;
        @JsonProperty("latest_reel_media")
        private Object latestReelMedia;
        @JsonProperty("total_clips_count")
        private int totalClipsCount;
        @JsonProperty("latest_besties_reel_media")
        private Object latestBestiesReelMedia;
        @JsonProperty("reel_media_seen_timestamp")
        private Object reelMediaSeenTimestamp;
        private String id;
        private String attempts;
    }

    @Data
    public static class HdProfilePicUrlInfo {
        private String url;
    }

    @Data
    public static class BiographyWithEntities {
        private List<Entity> entities;
    }

    @Data
    public static class Entity {
        private Object hashtag;
        private User user;
    }

    @Data
    public static class User {
        private String username;
        private String id;
    }

    @Data
    public static class BioLink {
        @JsonProperty("link_type")
        private String linkType;
        @JsonProperty("lynx_url")
        private String lynxUrl;
        private String title;
        private String url;
    }
}
