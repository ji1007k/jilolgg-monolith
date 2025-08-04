package com.test.basic.post.batch;

import com.test.basic.post.Post;
import com.test.basic.post.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostBatchProcessor {
    private final PostRepository postRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 인메모리 큐
    // ✅ ConcurrentLinkedQueue - 스레드 안전한 큐
    // Lock-free 알고리즘으로 고성능 + 안전성
    private final Queue<Post> postQueue = new ConcurrentLinkedQueue<>();

    // MAX_BATCH_SIZE개 모이거나 MAX_WAIT_MS 시간 지나면 즉시 배치 처리
    // 큐에서 10개씩 처리 → JPA가 jpa:batch_size개까지 모아서 DB 전송
    private static final int MAX_BATCH_SIZE = 10;  // 최대 큐 용량
    private static final int MAX_WAIT_MS = 30000; // 대기시간

    // 개별 요청을 큐에 저장
    public CompletableFuture<Post> addPostToQueue(Post post) {
        postQueue.offer(post);
        return CompletableFuture.completedFuture(post);
    }

    // 일정 시간마다 큐를 확인하여 배치 처리
    @Scheduled(fixedDelay = MAX_WAIT_MS)
    public void processBatch() {
        List<Post> batch = new ArrayList<>();

        for (int i=0; i<MAX_BATCH_SIZE && !postQueue.isEmpty(); i++) {
            Post post = postQueue.poll();
            if (post != null) {
                batch.add(post);
            }
        }

        if (!batch.isEmpty()) {
            this.createPostsBatch(batch);
            log.info("게시글 INSERT 배치 처리 완료: {}개", batch.size());
        }
    }

    /**
     * 배치 INSERT 최적화 - 여러 게시글 한 번에 처리
     * JPA saveAll()을 사용하여 배치 처리 성능 향상
     */
    @Transactional
    public List<Post> createPostsBatch(List<Post> posts) {
        log.info("배치 INSERT 시작: {} 개 게시글", posts.size());
        long startTime = System.currentTimeMillis();

        List<Post> savedPosts = postRepository.saveAll(posts);

        long endTime = System.currentTimeMillis();
        log.info("배치 INSERT 완료: {} 개 게시글, 소요시간: {}ms",
                savedPosts.size(), (endTime - startTime));

        entityManager.flush(); // 즉시 DB 반영
        entityManager.clear(); // 영속성 컨텍스트 클리어

        return savedPosts;
    }

}
