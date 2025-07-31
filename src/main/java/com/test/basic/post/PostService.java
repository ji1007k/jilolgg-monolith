package com.test.basic.post;

import com.test.basic.user.UserEntity;
import com.test.basic.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${uploads.path}")
    private String UPLOAD_DIR;


    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    /**
     * 배치 INSERT 최적화 - 여러 게시글 한 번에 처리
     * JPA saveAll()을 사용하여 배치 처리 성능 향상
     */
    @Transactional
    public List<Post> createPostsBatch(List<Post> posts) {
        logger.info("배치 INSERT 시작: {} 개 게시글", posts.size());
        long startTime = System.currentTimeMillis();
        
        List<Post> savedPosts = postRepository.saveAll(posts);
        
        long endTime = System.currentTimeMillis();
        logger.info("배치 INSERT 완료: {} 개 게시글, 소요시간: {}ms", 
                   savedPosts.size(), (endTime - startTime));

        entityManager.flush(); // 즉시 DB 반영
        entityManager.clear(); // 영속성 컨텍스트 클리어
        
        return savedPosts;
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

    @Transactional(rollbackFor = Exception.class)
    public void savePost(String title, String content, MultipartFile file, String userId) {
        try {
            // 파일 저장
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();  // 디렉터리 생성
            }

            // 파일 이름 정리 (특수문자나 공백 등 제거)
            String sanitizedFileName = file != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : null;

            // 파일이 비어있지 않으면 파일 저장
            if (file != null && !file.isEmpty()) {
                // CanonicalPath 사용하여 절대경로로 변환
                File destinationFile = new File(Paths.get(UPLOAD_DIR, sanitizedFileName).toString());
                File canonicalFile = destinationFile.getCanonicalFile();  // CanonicalFile로 정규화

                // 파일 저장 (정규화된 경로 사용)
                file.transferTo(canonicalFile);

                // DB에 파일 정보 저장 (예: 파일 경로 저장)
                saveFileToDatabase(title, sanitizedFileName, canonicalFile.getAbsolutePath());
            }

            // 게시글 저장 로직 (DB 저장 등)
            logger.info("게시글 저장: {}, {}", title, content);
//            savePostToDatabase(title, content);

        } catch (IOException e) {
            // 파일 저장 실패 시 예외 던지기 (트랜잭션 롤백을 위해 RuntimeException 던짐)
            throw new RuntimeException("파일 저장 실패", e);
        } catch (Exception e) {
            // 예상치 못한 예외 처리
            throw new RuntimeException("게시글 저장 실패", e);
        }
    }

    // 예시: 게시글 저장 메서드 (DB 저장 로직은 예시로 추가)
    @Transactional
    public void savePostToDatabase(String title, String content, Long userId) {
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));

        Post post = new Post(title, content, author);

        postRepository.save(post);

        logger.info("DB에 게시글 저장됨: {}, {}", title, content);
    }


    // 예시: 파일 정보를 DB에 저장하는 메서드
    private void saveFileToDatabase(String title, String fileName, String filePath) {
        // 실제 DB 저장 로직을 여기에 구현
        // 예: fileRepository.save(new FileInfo(title, fileName, filePath));
        logger.info("DB에 파일 정보 저장됨: {} -> {}", fileName, filePath);
    }
}
