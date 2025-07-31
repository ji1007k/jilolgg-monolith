package com.test.basic.post.batch;

import com.test.basic.post.Post;
import com.test.basic.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostBatchProcessor {
    // 인메모리 큐
    // ✅ ConcurrentLinkedQueue - 스레드 안전
    // Lock-free 알고리즘으로 고성능 + 안전성
    private final Queue<Post> postQueue = new ConcurrentLinkedQueue<>();
    private final PostService postService;

    // 10개 모이거나 1000ms 시간 지나면 즉시 배치 처리
    // 큐에서 10개씩 처리 → JPA가 jpa:batch_size개까지 모아서 DB 전송
    private static final int BATCH_SIZE = 10;  // 최대 큐 용량
    private static final int MAX_WAIT_MS = 500; // 대기시간

    // 개별 요청을 큐에 저장
    public CompletableFuture<Post> addPostToQueue(Post post) {
        postQueue.offer(post);
        return CompletableFuture.completedFuture(post);
    }

    // 500밀리초마다 큐를 확인하여 배치 처리
    @Scheduled(fixedDelay = MAX_WAIT_MS)
    public void processBatch() {
        List<Post> batch = new ArrayList<>();

        for (int i=0; i<BATCH_SIZE && !postQueue.isEmpty(); i++) {
            Post post = postQueue.poll();
            if (post != null) {
                batch.add(post);
            }
        }

        if (!batch.isEmpty()) {
            postService.createPostsBatch(batch);
            log.info("게시글 INSERT 배치 처리 완료: {}개", batch.size());
        }
    }

}
