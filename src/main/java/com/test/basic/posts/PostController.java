package com.test.basic.posts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
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

    @GetMapping(value = "/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable(name = "id") Long id) {
        Optional<Post> foundPost = postService.getPostById(id);

        if (!foundPost.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(foundPost.get());
    }

    @GetMapping
    public ResponseEntity<List> getAllPosts(@RequestParam("keyword") String keyword,
                                            @RequestParam("sort") String sort) {

        List<Post> posts = postService.getAllPosts(keyword, sort);
        return ResponseEntity.ok(posts);
    }

    @PutMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Post> updatePost(@PathVariable Long id,
                                           @RequestBody Post post) {

        post.setId(id);
        Post updatedPost = postService.updatePost(post);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

}
