package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostInfoResponse extends BaseApiResponse {

    private PostInfo data;

    @Data
    public static class PostInfo {
        private String id;
        @JsonProperty("__typename")
        private String typename;
        private String shortcode;
        @JsonProperty("display_url")
        private String displayUrl;
        @JsonProperty("display_resources")
        private List<DisplayResource> displayResources;
        @JsonProperty("accessibility_caption")
        private String accessibilityCaption;
        @JsonProperty("dash_info")
        private DashInfo dashInfo;
        @JsonProperty("has_audio")
        private Boolean hasAudio;
        @JsonProperty("video_url")
        private String videoUrl;
        private Dimensions dimensions;
        @JsonProperty("is_video")
        private boolean isVideo;
        private String title;
        @JsonProperty("video_duration")
        private Double videoDuration;
        @JsonProperty("video_view_count")
        private Integer videoViewCount;
        @JsonProperty("video_play_count")
        private Integer videoPlayCount;
        @JsonProperty("clips_music_attribution_info")
        private ClipsMusicAttributionInfo clipsMusicAttributionInfo;
        @JsonProperty("tracking_token")
        private String trackingToken;
        private Owner owner;
        @JsonProperty("edge_media_to_caption")
        private EdgeMediaToCaption edgeMediaToCaption;
        @JsonProperty("taken_at_timestamp")
        private long takenAtTimestamp;
        private Location location;
        @JsonProperty("edge_media_to_tagged_user")
        private EdgeMediaToTaggedUser edgeMediaToTaggedUser;
        @JsonProperty("edge_media_preview_like")
        private EdgeMediaPreviewLike edgeMediaPreviewLike;
        @JsonProperty("edge_media_to_parent_comment")
        private EdgeMediaToParentComment edgeMediaToParentComment;
        @JsonProperty("edge_media_preview_comment")
        private EdgeMediaPreviewComment edgeMediaPreviewComment;
        @JsonProperty("comments_disabled")
        private boolean commentsDisabled;
        @JsonProperty("commenting_disabled_for_viewer")
        private boolean commentingDisabledForViewer;
        @JsonProperty("product_type")
        private String productType;
        @JsonProperty("is_paid_partnership")
        private boolean isPaidPartnership;
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
    public static class Owner {
        private String id;
        private String username;
        @JsonProperty("is_private")
        private boolean isPrivate;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
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
    public static class Location {
        private String id;
        private String name;
        private String slug;
        @JsonProperty("address_json")
        private String addressJson;
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
        private String id;
        private String username;
        @JsonProperty("full_name")
        private String fullName;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
        @JsonProperty("is_verified")
        private boolean isVerified;
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
        @JsonProperty("is_verified")
        private boolean isVerified;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
        private String username;
    }

    @Data
    public static class DashInfo {
        @JsonProperty("is_dash_eligible")
        private boolean isDashEligible;
        @JsonProperty("video_dash_manifest")
        private String videoDashManifest;
        @JsonProperty("number_of_qualities")
        private int numberOfQualities;
    }

    @Data
    public static class ClipsMusicAttributionInfo {
        @JsonProperty("artist_name")
        private String artistName;
        @JsonProperty("song_name")
        private String songName;
        @JsonProperty("uses_original_audio")
        private boolean usesOriginalAudio;
        @JsonProperty("should_mute_audio")
        private boolean shouldMuteAudio;
        @JsonProperty("audio_id")
        private String audioId;
    }

    @Data
    public static class EdgeMediaToParentComment {
        private int count;
        @JsonProperty("page_info")
        private PageInfo pageInfo;
        private List<CommentEdge> edges;
    }

    @Data
    public static class EdgeMediaPreviewComment {
        private int count;
        private List<CommentEdge> edges;
    }

    @Data
    public static class CommentEdge {
        private CommentNode node;
    }

    @Data
    public static class CommentNode {
        private String id;
        private String text;
        @JsonProperty("created_at")
        private long createdAt;
        private Owner owner;
    }

    @Data
    public static class PageInfo {
        @JsonProperty("has_next_page")
        private boolean hasNextPage;
        @JsonProperty("end_cursor")
        private String endCursor;
    }
}
