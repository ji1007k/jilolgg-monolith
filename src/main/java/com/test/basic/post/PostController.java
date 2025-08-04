package com.test.basic.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/posts")
@Tag(name="Post API", description="게시글 관리 API")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @Operation(summary="게시글 등록 대기열에 추가", description="게시글 등록 대기열에 추가 API")
    public ResponseEntity<Map<String, Object>> addPostToQueue(@RequestBody Post post) {
        // 큐에 추가하고 즉시 응답
        postService.addPostToQueue(post);

        return ResponseEntity.accepted()    // 202 즉시 응답
                .body(Map.of(
                        "status", "queued",
                        "message", "요청이 처리 대기열에 추가되었습니다"
                ));
    }

    @PostMapping("/non-batch")
    @Operation(summary="게시글 등록", description="게시글 등록 API")
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post newPost = postService.createPost(post);

        // 새로 생성된 게시글의 ID를 기반으로 URI 생성
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newPost.getId())
                .toUri();

        // 생성된 위치를 Location 헤더에 추가하고, HTTP 201 상태 코드로 응답 반환
        // 주의) 만약 location이 null이라면? → 201 Created가 아닌 200 OK로 동작할 수도 있음
        return ResponseEntity.created(location).body(newPost);
    }

    @PostMapping("/batch")
    @Operation(summary="게시글 배치 등록", description="여러 게시글을 한 번에 등록하는 수동 API")
    public ResponseEntity<List<Post>> createPostsBatch(@RequestBody List<Post> posts) {

        // 배치 크기 제한 (메모리 보호)
        if (posts.size() > 100) {
//            return ResponseEntity.badRequest().body("최대 100개까지만 처리 가능");
            return ResponseEntity.badRequest().build();
        }

        List<Post> newPosts = postService.createPostsBatch(posts);
        return ResponseEntity.status(HttpStatus.CREATED).body(newPosts);
    }

    @GetMapping(value = "/{id}")
    @Operation(summary="게시글 조회", description="게시글 조회 API")
    public ResponseEntity<Post> getPostById(@PathVariable(name = "id") Long id) {
        Optional<Post> foundPost = postService.getPostById(id);

        if (!foundPost.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(foundPost.get());
    }

    @GetMapping
    @Operation(summary="게시글 목록 조회", description="게시글 목록 조회 API")
    public ResponseEntity<List> getAllPosts(@RequestParam("keyword") String keyword,
                                            @RequestParam("sort") String sort) {

        List<Post> posts = postService.getAllPosts(keyword, sort);
        return ResponseEntity.ok(posts);
    }

    @PutMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    @Operation(summary="게시글 수정", description="게시글 수정 API")
    public ResponseEntity<Post> updatePost(@PathVariable Long id,
                                           @RequestBody Post post) {

        post.setId(id);
        Post updatedPost = postService.updatePost(post);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    @Operation(summary="게시글 삭제", description="게시글 삭제 API")
    public ResponseEntity deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

}
