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

@Node("Blogger")
@Getter
@Setter
@ToString(exclude = {"followings", "followers", "posts", "likedPosts", "taggedInPosts", "belongsToGroup"})
@AllArgsConstructor
@NoArgsConstructor
public class Blogger {

    @Id
    private String username;

    @Property("seed_group")
    private String seedGroup;

    // 分组关系：博主属于某个种子分组
    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private SeedGroup belongsToGroup;

    @Property("bio")
    private String bio;

    @Property("gender")
    private String gender;

    @Property("instagram_id")
    private Long instagramId;

    @Property("country")
    private String country;

    @Property("date_joined")
    private String dateJoined;

    @Property("date_joined_as_timestamp")
    private Long dateJoinedAsTimestamp;

    @Property("date_verified")
    private String dateVerified;

    @Property("date_verified_as_timestamp")
    private Long dateVerifiedAsTimestamp;

    @Property("former_usernames")
    private Integer formerUsernames;

    @Property("is_verified")
    private Boolean isVerified;

    @Property("full_name")
    private String fullName;

    @JsonIgnore
    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    private Set<Blogger> followings = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.INCOMING)
    private Set<Blogger> followers = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "POSTED", direction = Relationship.Direction.OUTGOING)
    private Set<Post> posts = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "LIKED", direction = Relationship.Direction.OUTGOING)
    private Set<Post> likedPosts = new HashSet<>();

    @JsonIgnore
    @Relationship(type = "TAGGED_IN", direction = Relationship.Direction.OUTGOING)
    private Set<Post> taggedInPosts = new HashSet<>();

    public Blogger(String username, String seedGroup) {
        this.username = username;
        this.seedGroup = seedGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blogger blogger = (Blogger) o;
        return Objects.equals(username, blogger.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
