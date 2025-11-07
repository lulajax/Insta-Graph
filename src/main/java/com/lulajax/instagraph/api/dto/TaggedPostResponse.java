package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class TaggedPostResponse extends BaseApiResponse {
    private TaggedPostDataWrapper data;

    @Data
    public static class TaggedPostDataWrapper {
        private TaggedPostDataUserWrapper data;
    }

    @Data
    public static class TaggedPostDataUserWrapper {
        private TaggedPostData user;
    }

    @Data
    public static class TaggedPostData {
        @JsonProperty("edge_user_to_photos_of_you")
        private EdgeUserToPhotosOfYou edgeUserToPhotosOfYou;
    }

    @Data
    public static class EdgeUserToPhotosOfYou {
        private int count;
        @JsonProperty("page_info")
        private PageInfo pageInfo;
        private List<TaggedPostEdge> edges;
    }

    @Data
    public static class PageInfo {
        @JsonProperty("has_next_page")
        private boolean hasNextPage;
        @JsonProperty("end_cursor")
        private String endCursor;
    }

    @Data
    public static class TaggedPostEdge {
        private PostNode node;
    }

    @Data
    public static class PostNode {
        private String id;
        @JsonProperty("shortcode")
        private String shortcode;
        @JsonProperty("display_url")
        private String displayUrl;
        @JsonProperty("is_video")
        private boolean isVideo;
        @JsonProperty("video_view_count")
        private Integer videoViewCount;
        @JsonProperty("taken_at_timestamp")
        private long takenAtTimestamp;
        private Dimensions dimensions;
        private Owner owner;
        @JsonProperty("edge_media_to_caption")
        private EdgeMediaToCaption edgeMediaToCaption;
        @JsonProperty("edge_liked_by")
        private EdgeLikedBy edgeLikedBy;
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
    public static class EdgeLikedBy {
        private int count;
    }
}
