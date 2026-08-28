package com.test.basic.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Schema(description = "사용자 정보")
@Setter @Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /** 권한을 지정하지 않고 가입한 사용자의 기본 권한. */
    public static final String DEFAULT_AUTHORITY = "SCOPE_USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    private String password;
    @Column(unique = true, nullable = false)
    private String email;
    private String name;
    /**
     * 권한. 쉼표로 구분된 문자열(예: "SCOPE_USER").
     *
     * 반드시 자바 쪽 기본값을 둔다. DB 컬럼에 default 'SCOPE_USER'가 있지만,
     * JPA는 INSERT문에 이 컬럼을 NULL로 명시해 넣기 때문에 DB 기본값이 적용되지 않는다.
     * 비어 있으면 로그인 시 권한 파싱에서 실패해 가입은 되는데 로그인이 안 되는 상태가 된다.
     */
    @Builder.Default
    private String authority = DEFAULT_AUTHORITY;
    private String profileImageUrl;
    @Builder.Default
    private Integer passwordVersion = 1;
    @CreationTimestamp
    private LocalDateTime createdDt;
    private LocalDateTime updatedDt;

    public UserEntity(UserEntity user) {
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.name = user.getName();
        this.authority = user.getAuthority();
        this.profileImageUrl = user.getProfileImageUrl();
    }
}
