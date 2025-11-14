package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UserPostResponse extends BaseApiResponse {
    private UserPostDataWrapper data;

    @Data
    public static class UserPostDataWrapper {
        private UserPostDataUserWrapper data;
    }

    @Data
    public static class UserPostDataUserWrapper {
        private UserPostData user;
    }

    @Data
    public static class UserPostData {
        @JsonProperty("edge_owner_to_timeline_media")
        private EdgeOwnerToTimelineMedia edgeOwnerToTimelineMedia;
    }

    @Data
    public static class EdgeOwnerToTimelineMedia {
        private int count;
        @JsonProperty("page_info")
        private PageInfo pageInfo;
        private List<UserPostEdge> edges;
    }

    @Data
    public static class PageInfo {
        @JsonProperty("has_next_page")
        private boolean hasNextPage;
        @JsonProperty("end_cursor")
        private String endCursor;
    }

    @Data
    public static class UserPostEdge {
        private PostNode node;
    }

    @Data
    public static class PostNode {
        @JsonProperty("__typename")
        private String typename;
        private String id;
        private String shortcode;
        @JsonProperty("display_url")
        private String displayUrl;
        @JsonProperty("display_resources")
        private List<DisplayResource> displayResources;
        @JsonProperty("is_video")
        private boolean isVideo;
        @JsonProperty("media_preview")
        private String mediaPreview;
        @JsonProperty("tracking_token")
        private String trackingToken;
        @JsonProperty("thumbnail_src")
        private String thumbnailSrc;
        @JsonProperty("taken_at_timestamp")
        private long takenAtTimestamp;
        @JsonProperty("viewer_has_liked")
        private boolean viewerHasLiked;
        @JsonProperty("viewer_has_saved")
        private boolean viewerHasSaved;
        @JsonProperty("viewer_has_saved_to_collection")
        private boolean viewerHasSavedToCollection;
        @JsonProperty("viewer_in_photo_of_you")
        private boolean viewerInPhotoOfYou;
        @JsonProperty("viewer_can_reshare")
        private boolean viewerCanReshare;
        @JsonProperty("is_affiliate")
        private boolean isAffiliate;
        @JsonProperty("is_paid_partnership")
        private boolean isPaidPartnership;
        @JsonProperty("like_and_view_counts_disabled")
        private boolean likeAndViewCountsDisabled;
        @JsonProperty("comments_disabled")
        private boolean commentsDisabled;
        @JsonProperty("has_upcoming_event")
        private boolean hasUpcomingEvent;
        @JsonProperty("accessibility_caption")
        private Object accessibilityCaption;
        @JsonProperty("gating_info")
        private Object gatingInfo;
        @JsonProperty("fact_check_overall_rating")
        private Object factCheckOverallRating;
        @JsonProperty("fact_check_information")
        private Object factCheckInformation;
        @JsonProperty("media_overlay_info")
        private Object mediaOverlayInfo;
        @JsonProperty("sensitivity_friction_info")
        private Object sensitivityFrictionInfo;
        @JsonProperty("sharing_friction_info")
        private Object sharingFrictionInfo;
        private Object location;
        @JsonProperty("nft_asset_info")
        private Object nftAssetInfo;
        private Owner owner;
        @JsonProperty("edge_media_to_comment")
        private EdgeMediaToComment edgeMediaToComment;
        @JsonProperty("edge_media_to_sponsor_user")
        private EdgeMediaToSponsorUser edgeMediaToSponsorUser;
        @JsonProperty("coauthor_producers")
        private List<Object> coauthorProducers;
        @JsonProperty("pinned_for_users")
        private List<Object> pinnedForUsers;
        @JsonProperty("thumbnail_resources")
        private List<ThumbnailResource> thumbnailResources;
        @JsonProperty("edge_sidecar_to_children")
        private EdgeSidecarToChildren edgeSidecarToChildren;
        @JsonProperty("edge_media_to_tagged_user")
        private EdgeMediaToTaggedUser edgeMediaToTaggedUser;
        @JsonProperty("edge_media_to_caption")
        private EdgeMediaToCaption edgeMediaToCaption;
        @JsonProperty("edge_media_preview_like")
        private EdgeMediaPreviewLike edgeMediaPreviewLike;
        private Dimensions dimensions;
    }

    @Data
    public static class Dimensions {
        private int height;
        private int width;
    }

    @Data
    public static class DisplayResource {
        private String src;
        @JsonProperty("config_width")
        private int configWidth;
        @JsonProperty("config_height")
        private int configHeight;
    }

    @Data
    public static class EdgeMediaToTaggedUser {
        private List<TaggedUserEdge> edges;
    }

    @Data
    public static class TaggedUserEdge {
        private TaggedUserNode node;
    }

    @Data
    public static class TaggedUserNode {
        private TaggedUser user;
        private double x;
        private double y;
    }

    @Data
    public static class TaggedUser {
        @JsonProperty("full_name")
        private String fullName;
        private String id;
        @JsonProperty("is_verified")
        private boolean isVerified;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
        private String username;
        @JsonProperty("followed_by_viewer")
        private boolean followedByViewer;
    }

    @Data
    public static class EdgeMediaPreviewLike {
        private int count;
        private List<LikedByEdge> edges;
    }

    @Data
    public static class LikedByEdge {
        private LikedByNode node;
    }

    @Data
    public static class LikedByNode {
        private String id;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
        private String username;
    }

    @Data
    public static class EdgeMediaToCaption {
        private List<CaptionEdge> edges;
    }

    @Data
    public static class CaptionEdge {
        private CaptionNode node;
    }

    @Data
    public static class CaptionNode {
        private String text;
    }

    @Data
    public static class Owner {
        private String id;
        private String username;
    }

    @Data
    public static class EdgeMediaToComment {
        private int count;
        @JsonProperty("page_info")
        private PageInfo pageInfo;
    }

    @Data
    public static class EdgeMediaToSponsorUser {
        private List<Object> edges;
    }

    @Data
    public static class ThumbnailResource {
        private String src;
        @JsonProperty("config_width")
        private int configWidth;
        @JsonProperty("config_height")
        private int configHeight;
    }

    @Data
    public static class EdgeSidecarToChildren {
        private List<SidecarEdge> edges;
    }

    @Data
    public static class SidecarEdge {
        private SidecarNode node;
    }

    @Data
    public static class SidecarNode {
        @JsonProperty("__typename")
        private String typename;
        private String id;
        @JsonProperty("display_url")
        private String displayUrl;
        @JsonProperty("is_video")
        private boolean isVideo;
        @JsonProperty("media_preview")
        private String mediaPreview;
        @JsonProperty("tracking_token")
        private String trackingToken;
        @JsonProperty("has_upcoming_event")
        private boolean hasUpcomingEvent;
        @JsonProperty("accessibility_caption")
        private Object accessibilityCaption;
        private Dimensions dimensions;
        @JsonProperty("display_resources")
        private List<DisplayResource> displayResources;
        @JsonProperty("edge_media_to_tagged_user")
        private EdgeMediaToTaggedUser edgeMediaToTaggedUser;
        @JsonProperty("dash_info")
        private DashInfo dashInfo;
        @JsonProperty("has_audio")
        private boolean hasAudio;
        @JsonProperty("video_url")
        private String videoUrl;
        @JsonProperty("video_view_count")
        private int videoViewCount;
    }

    @Data
    public static class DashInfo {
        @JsonProperty("is_dash_eligible")
        private boolean isDashEligible;
        @JsonProperty("video_dash_manifest")
        private Object videoDashManifest;
        @JsonProperty("number_of_qualities")
        private int numberOfQualities;
    }
}
