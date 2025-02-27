package com.test.basic.posts;

import com.test.basic.users.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Setter;

import java.time.LocalDateTime;

@Table(name = "posts")
@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String title;

    private String content;

    // 현재 테이블(예: posts)에 있는 외래키 컬럼을 의미하고,
    // 참조하는 테이블(users)에서는 기본적으로 @Id가 붙어 있는 PK를 자동으로 참조
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    private UserEntity author;

    @Column(updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdDt;

    private LocalDateTime updatedDt;

    private Long viewCnt;

    private Long LikeCnt;

    private String category;

    private ActiveStatus status;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserEntity author) {
        this.author = author;
    }

    public LocalDateTime getCreatedDt() {
        return createdDt;
    }

    public void setCreatedDt(LocalDateTime createdDt) {
        this.createdDt = createdDt;
    }

    public LocalDateTime getUpdatedDt() {
        return updatedDt;
    }

    public void setUpdatedDt(LocalDateTime updatedDt) {
        this.updatedDt = updatedDt;
    }

    public Long getViewCnt() {
        return viewCnt;
    }

    public void setViewCnt(Long viewCnt) {
        this.viewCnt = viewCnt;
    }

    public Long getLikeCnt() {
        return LikeCnt;
    }

    public void setLikeCnt(Long likeCnt) {
        LikeCnt = likeCnt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", author=" + author +
                ", createdDt=" + createdDt +
                ", updatedDt=" + updatedDt +
                ", viewCnt=" + viewCnt +
                ", LikeCnt=" + LikeCnt +
                ", category='" + category + '\'' +
                ", status=" + status +
                '}';
    }
}


