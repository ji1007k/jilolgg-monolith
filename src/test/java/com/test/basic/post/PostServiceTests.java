package com.test.basic.post;

import com.test.basic.post.batch.PostBatchProcessor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class PostServiceTests {

    @Mock
    private PostRepository postRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PostBatchProcessor postBatchProcessor;

    @InjectMocks
    private PostService postService;    // 테스트 대상 (Mock들을 주입받는 실제 객체)

    private Post post;

    @BeforeEach
    void setUp() {
        // @InjectMocks는 생성자나 setter로만 주입하고, @PersistenceContext 같은 어노테이션 필드는 못 찾음.
        // -> ReflectionTestUtils 로 강제 주입
        ReflectionTestUtils.setField(postService, "entityManager", entityManager);

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

    @Test
    void 인메모리큐에_게시글추가_성공테스트() {
        // when
        postService.addPostToQueue(post);

        // then - postBatchProcessor.addPostToQueue 호출여부 확인
        verify(postBatchProcessor).addPostToQueue(post);
    }

    @Test
    void 게시글등록배치_예외발생시_저장실패() {
        // 1. 예외 강제 발생시키기
        when(postRepository.saveAll(any()))
                .thenThrow(new RuntimeException("DB 저장 실패"));

        // 2. 스케줄러 실행 - 예외가 발생해도 테스트는 계속 진행
        assertThatThrownBy(() -> {
            postService.createPostsBatch(List.of(post));
        }).isInstanceOf(RuntimeException.class).hasMessage("DB 저장 실패");

        // 3. saveAll 호출되었는지 확인
        verify(postRepository).saveAll(any());
        verify(entityManager, never()).flush(); // 예외로 인해 flush 실행 안됨
    }

}
