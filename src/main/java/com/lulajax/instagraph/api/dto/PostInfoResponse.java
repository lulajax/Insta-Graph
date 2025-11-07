package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class PostInfoResponse extends BaseApiResponse {

    private PostInfo data;

    @Data
    public static class PostInfo {
        private String id;
        private String shortcode;
        @JsonProperty("display_url")
        private String displayUrl;
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
    }

    @Data
    public static class Dimensions {
        private int height;
        private int width;
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
    }
    
    @Data
    public static class TaggedUser {
        private String id;
        private String username;
        @JsonProperty("full_name")
        private String fullName;
        @JsonProperty("profile_pic_url")
        private String profilePicUrl;
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
}
