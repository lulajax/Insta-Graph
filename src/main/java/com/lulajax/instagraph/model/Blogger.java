package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Node("Blogger")
@Getter
@Setter
@ToString(exclude = {"belongsToGroup"})
@AllArgsConstructor
@NoArgsConstructor
public class Blogger {

    @Id
    private String username;

    @Property("seed_group")
    private String seedGroup;

    @Property("seed_reason")
    private String seedReason;

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

    @Property("is_private")
    private Boolean isPrivate;

    @Property("full_name")
    private String fullName;

    // 放弃相关字段
    @Property("abandoned")
    private Boolean abandoned;  // 是否已放弃（默认 false 或 null）

    @Property("abandoned_at")
    private Long abandonedAt;  // 放弃时间戳

    @Property("abandoned_reason")
    private String abandonedReason;  // 放弃原因（可选）

    @Property("aggregation_reason")
    private String aggregationReason;  // 聚合原因（可选）

    public Blogger(String username) {
        this.username = username;
    }

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
