package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Node("Post")
@Getter
@Setter
@ToString(exclude = {"taggedInUsers", "likedBy", "owner", "location"})
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    @Id
    private String id;

    @Property("display_url")
    private String displayUrl;

    @Property("caption")
    private String caption;

    @Property("is_video")
    private Boolean isVideo;
    
    @Property("shortcode")
    private String shortcode;

    @Property("timestamp")
    private Long timestamp;

    @Property("like_count")
    private Integer likeCount;

    @Property("video_view_count")
    private Integer videoViewCount;

    @Property("comment_count")
    private Integer commentCount;

    @Property("title")
    private String title;
    
    @Property("video_duration")
    private Double videoDuration;

    @Property("video_play_count")
    private Integer videoPlayCount;

    @Relationship(type = "HAS_LOCATION", direction = Relationship.Direction.OUTGOING)
    private Location location;

    @JsonIgnore
    @Relationship(type = "TAGGED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Blogger> taggedInUsers = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "LIKED", direction = Relationship.Direction.INCOMING)
    private Set<Blogger> likedBy = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "POSTED", direction = Relationship.Direction.INCOMING)
    private Blogger owner;

    public Post(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
