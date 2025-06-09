package com.test.basic.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class PostServiceTests {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private Post post;

    @BeforeEach
    void setUp() {

        post = new Post();
        post.setId(1L);
        post.setTitle("게시글 제목");
        post.setContent("게시글 내용");
        post.setCategory("게시글 카테고리");
    }


    @Test
    void testCreatePost() {
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocationOnMock -> {
                    Post newPost = invocationOnMock.getArgument(0);
                    return newPost;
                }
        );

        Post createdPost = postService.createPost(post);

        assertNotNull(createdPost);
        assertThat(createdPost.getId()).isNotNull();
        assertThat(createdPost.getId()).isEqualTo(post.getId());
        assertThat(createdPost.getTitle()).isEqualTo("게시글 제목");
        assertThat(createdPost.getContent()).isEqualTo("게시글 내용");
    }

    @Test
    void testGetPostById() {
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));

        Optional<Post> foundPost = postService.getPostById(1L);

        assertThat(foundPost.isPresent()).isTrue();
        assertThat(foundPost.get().getId()).isEqualTo(post.getId());
    }

    @Test
    void testGetAllPosts() {
        List<Post> posts = List.of(post);

        when(postRepository.findAll()).thenReturn(posts);

        List<Post> foundPosts = postService.getAllPosts("", "");

        assertThat(foundPosts).isNotNull();
        assertThat(foundPosts.size()).isGreaterThan(0);
        assertThat(foundPosts.get(0).getId()).isEqualTo(post.getId());
    }

    @Test
    void testUpdatePost() {
        // 수정된 게시글 내용
        Post updatedPost = new Post();
        updatedPost.setId(1L);
        updatedPost.setContent("수정된 내용");  // 수정된 내용
        updatedPost.setUpdatedDt(LocalDateTime.now());

        // mock 설정: findById가 existingPost를 반환하고, save는 updatedPost를 반환
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        // save 메서드에서 새로운 객체를 만들어 반환
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post postToSave = invocation.getArgument(0);
            return postToSave;  // 수정 후 객체 반환
        });

        // 실제 서비스 메서드 호출
        Post result = postService.updatePost(updatedPost);

        // 검증
        assertNotNull(result);
        assertThat(result.getTitle()).isEqualTo(updatedPost.getTitle());
        assertThat(result.getContent()).isEqualTo(updatedPost.getContent());
    }

    @Test
    void testDeletePost() {
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(any(Post.class));
        
        postService.deletePost(post.getId());
        
        // postRepository의 findById와 delete 호출여부 확인
        verify(postRepository, times(1)).findById(post.getId());
        verify(postRepository, times(1)).delete(post);
    }
}
