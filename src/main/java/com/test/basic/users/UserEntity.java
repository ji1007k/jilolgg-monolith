package com.test.basic.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Schema(description = "사용자 정보")
@Setter
@Getter
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
