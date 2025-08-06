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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    private String password;
    private String email;
    private String name;
    private String authority;
    private String profileImageUrl;
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
