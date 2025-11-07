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
        private String id;
        @JsonProperty("display_url")
        private String displayUrl;
        @JsonProperty("display_resources")
        private List<DisplayResource> displayResources;
        @JsonProperty("is_video")
        private boolean isVideo;
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
}
