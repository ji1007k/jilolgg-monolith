package com.test.basic.auth.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "users")
@Schema(description = "사용자 정보")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    private String password;
    private String email;
    private String name;
    private String profileImageUrl;
    private LocalDateTime createdDt;
    private LocalDateTime updatedDt;

    public User(User user) {
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.name = user.getName();
        this.profileImageUrl = user.getProfileImageUrl();
    }
}
