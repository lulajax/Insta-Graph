# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Insta-Graph** is a graph database application designed for social media analysts to discover core members of specific Instagram communities efficiently. Instead of mass data collection, it uses a "small and precise" strategy: analysts provide a small batch of "seed bloggers," and the system analyzes deep relationships between them to intelligently recommend new bloggers highly relevant to that group.

**Core Value Proposition**: The system's value is NOT in data volume, but in the **deep analysis of co-tagging relationships**. When two bloggers are tagged together in the same post, this indicates a strong real-world connection (e.g., attending events together, collaborations). By analyzing these co-tagging patterns across seed bloggers, the system can efficiently discover new community members with high relevance.

This approach enables analysts to trace from small clues and gradually map out precise network graphs of entire communities through iterative discovery.

## Technology Stack

- **Backend**: Spring Boot 3.2.5 with Java 21
- **Database**: Neo4j 5.x (graph database)
- **Query Language**: Cypher (for all graph analysis logic)
- **External API**: Tikhub API (via RapidAPI) for Instagram data acquisition
- **Deployment**: Docker Compose for containerized deployment
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)

## Build and Run Commands

### Local Development

```bash
# Build the project
./mvnw clean install

# Run the application (requires Neo4j running)
./mvnw spring-boot:run

# Package the application
./mvnw clean package
```

### Docker Deployment

```bash
# Start all services (Neo4j + Spring Boot app)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

### Accessing Services

- **Spring Boot API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Neo4j Browser**: http://localhost:7474 (credentials: neo4j/password)
- **Neo4j Bolt**: bolt://localhost:7687

## Configuration

Before running the application:

1. Copy `src/main/resources/application.properties.example` to `application.properties`
2. Set your RapidAPI key: `tikhub.api.x-rapidapi-key=YOUR_KEY_HERE`
3. Adjust Neo4j credentials if needed (default: neo4j/Passw0rd for local, neo4j/password for Docker)

**Important**: `application.properties` is git-ignored to prevent exposing API keys.

## Architecture

### Graph Data Model

The application uses Neo4j to model Instagram relationships according to the requirements document:

**Nodes:**
- `:Blogger` - Instagram users with properties:
  - `username` (ID) - Instagram username (e.g., @username)
  - `seed_group` (core property) - Project identifier (e.g., "busan_dancers"). If null, the blogger is newly discovered
  - `instagram_id`, `full_name`, `bio`, `gender` - Profile information from Tikhub API
  - Additional metadata: `country`, `date_joined`, `is_verified`, etc.

- `:Post` - Instagram posts with properties:
  - `id` (ID) - Post unique identifier
  - **Core properties (from requirements):**
    - `notes` - Manual annotations (e.g., "XX dance studio video")
    - `latitude`, `longitude` - Geographic coordinates
  - **Extended properties (from Tikhub API):**
    - `shortcode`, `caption`, `display_url`, `timestamp`
    - `like_count`, `video_view_count`, `video_duration`, etc.

- `:Location` - Geographic locations associated with posts

- `:Hashtag` - Hashtags used in posts
  - `name` (ID) - Hashtag name (e.g., "#부산댄스")

**Relationships (all unidirectional):**
- `:FOLLOWS` - `(Blogger)-[:FOLLOWS]->(Blogger)` - Follower relationship
- **`:TAGGED_IN`** - `(Post)-[:TAGGED_IN]->(Blogger)` - **Core relationship**: Post tags a Blogger (indicates strong real-world connection)
- `:POSTED` - `(Blogger)-[:POSTED]->(Post)` - Blogger created a Post
- `:LIKED` - `(Blogger)-[:LIKED]->(Post)` - Blogger liked a Post
- `:HAS_LOCATION` - `(Post)-[:HAS_LOCATION]->(Location)` - Post has a Location
- `:USES_HASHTAG` - `(Post)-[:USES_HASHTAG]->(Hashtag)` - Post uses a Hashtag

**Design Notes:**
- All relationships are **unidirectional** - Neo4j can efficiently traverse relationships in any direction, so bidirectional modeling is unnecessary and adds maintenance complexity
- The `seed_group` property organizes different analysis projects - bloggers with null `seed_group` are candidates for discovery
- **`:TAGGED_IN` is the most critical relationship** - when multiple bloggers are tagged in the same post, it indicates strong community ties

### Core Analysis Queries

Located in `BloggerRepository.java`:

1. **`findCommonFollows`**: Finds bloggers followed by multiple seed bloggers (common follows pattern)
   ```cypher
   MATCH (seed:Blogger {seed_group: $project})
   MATCH (seed)-[:FOLLOWS]->(rec:Blogger)
   WHERE rec.seed_group IS NULL OR rec.seed_group <> $project
   WITH rec, COUNT(seed) AS common_follow_count
   WHERE common_follow_count >= $min_follows
   RETURN rec.username, common_follow_count
   ```

2. **`findCoTagged`** (Core Algorithm): Finds bloggers who are co-tagged with seed bloggers in posts
   ```cypher
   MATCH (seed:Blogger {seed_group: $project})
   MATCH (seed)<-[:TAGGED_IN]-(post:Post)-[:TAGGED_IN]->(rec:Blogger)
   WHERE rec.seed_group IS NULL OR rec.seed_group <> $project
   WITH rec, COUNT(DISTINCT post) AS common_post_count
   WHERE common_post_count >= $min_co_tags
   RETURN rec.username, common_post_count
   ```
   Note: The pattern `(seed)<-[:TAGGED_IN]-(post)-[:TAGGED_IN]->(rec)` means:
   - Post tags seed blogger (existing direction: Post→Blogger)
   - Same post also tags recommended blogger
   - This identifies strong community connections through co-tagging

These Cypher queries use the `seed_group` property to isolate different analysis projects and find recommendations outside the current seed group.

### Application Layers

**Controller Layer** (`com.lulajax.instagraph.controller` & `.api.controller`):
- `InstaGraphController` - Manual data entry endpoints (add bloggers, posts, relationships)
- `AggregationController` - Main data collection orchestrator
- `UserInfoController`, `FollowingController`, `PostController`, `TaggedPostController`, `PostInfoController` - Individual Tikhub API endpoints

**Service Layer** (`com.lulajax.instagraph.service` & `.api.service`):
- `InstaGraphService` - Core graph operations and analysis queries
- `AggregationService` - Orchestrates multi-step data collection workflow:
  1. Fetch user info → 2. Fetch followings → 3. Fetch posts → 4. Fetch tagged posts
- Individual service classes fetch from Tikhub API and persist to Neo4j

**Repository Layer** (`com.lulajax.instagraph.repository`):
- Spring Data Neo4j repositories with custom Cypher queries
- `BloggerRepository` contains the critical analysis queries

**Model Layer** (`com.lulajax.instagraph.model`):
- Neo4j node entities with Spring Data Neo4j annotations
- Uses `@Node`, `@Relationship`, and `@Property` annotations
- `@JsonIgnore` on relationship fields to prevent circular serialization

**Configuration** (`com.lulajax.instagraph.config`):
- `Neo4jConfig` - Configures Neo4j 5 dialect (uses `elementId()` instead of deprecated `id()`)
- `TikhubApiProperties` - Binds `tikhub.api.*` properties from configuration
- `DatabaseInitializer` - Sets up initial schema/indexes on startup

### Data Collection Workflow

The typical workflow for analyzing a community:

1. **Identify seed bloggers**: Manually add initial known community members with a `seed_group` tag
2. **Aggregate data**: Call `/api/aggregate/{username}` for each seed blogger to:
   - Fetch and store their profile info
   - Fetch and store who they follow
   - Fetch and store their posts
   - Fetch and store posts they're tagged in
3. **Run analysis**: Query `/analysis/co-tagged` or `/analysis/common-follows` to discover new bloggers
4. **Iterate**: Add discovered bloggers as new seeds and repeat

### Neo4j 5 Compatibility

The application is configured for Neo4j 5.x:
- Uses `spring.data.neo4j.use-element-id=true` to use `elementId()` instead of deprecated `id()`
- `Neo4jConfig` sets Cypher-DSL dialect to `NEO4J_5`
- Deprecation warnings are suppressed via logging configuration

## Common Development Patterns

### Adding New Tikhub API Endpoints

1. Add endpoint URL to `application.properties.example` and `TikhubApiProperties.java`
2. Create DTO classes in `com.lulajax.instagraph.api.dto` for API responses
3. Create service in `.api.service` to call API and persist data
4. Create controller in `.api.controller` to expose REST endpoint
5. Use `HttpUtil` for making HTTP requests and `JsonUtil` for parsing responses

### Writing Custom Cypher Queries

Add `@Query` annotated methods to repository interfaces. Example pattern:
```java
@Query("""
    MATCH (seed:Blogger {seed_group: $project})
    MATCH (seed)-[relation]->(target)
    RETURN target.username AS username, COUNT(*) AS count
    ORDER BY count DESC
""")
List<AnalysisResult> customQuery(String project);
```

### Working with Graph Relationships

When creating/updating relationships in code:
- Fetch both nodes first using repositories
- Establish relationship in memory by adding to the Set (e.g., `post.getTaggedInUsers().add(blogger)`)
- Save the **source** node - Spring Data Neo4j cascades the relationship

Example for TAGGED_IN:
```java
// Correct: Post -> Blogger direction
Post post = postRepository.findById(postId).orElse(new Post(postId));
Blogger blogger = bloggerRepository.findById(username).orElse(new Blogger(username, null));
post.getTaggedInUsers().add(blogger);  // Add to Post's outgoing relationship
postRepository.save(post);  // Save source node
```

**Important**: Always add relationships from the source node (the one with OUTGOING direction in the model).

## Project Structure Notes

- Chinese comments are used throughout the codebase for Chinese-speaking team
- Lombok is used extensively (`@Getter`, `@Setter`, `@AllArgsConstructor`, etc.)
- `@ToString(exclude = {...})` prevents circular references in bidirectional relationships
- No test directory currently exists - tests should be added in `src/test/java`
