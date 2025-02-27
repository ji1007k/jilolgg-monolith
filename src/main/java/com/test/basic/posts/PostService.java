package com.test.basic.posts;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    // TODO 검색 조건 적용
    public List<Post> getAllPosts(String keyword, String sort) {
        return postRepository.findAll();
    }

    public Post updatePost(Post post) {
        // 기존 post 찾기
        Post foundPost = this.getPostById(post.getId())
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + post.getId()));

        foundPost.setTitle(post.getTitle());
        foundPost.setContent(post.getContent());
        foundPost.setUpdatedDt(post.getUpdatedDt());

        return postRepository.save(foundPost); // 변경된 포스트를 반환
    }

    public void deletePost(Long id) {
        Post foundPost = getPostById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        postRepository.delete(foundPost);
    }
}
