package com.test.basic.auth.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String mail);


    // 기본 메소드를 대신할 커스텀 메소드 정의. 커스텀 메소드는 엔티티 객체 반환 불가
    @Modifying  // INSERT, UPDATE, DELETE 쿼리 실행을 위한 어노테이션
    @Query(value = "INSERT INTO User (createdDt, email, name, password) " +
            "VALUES (CURRENT_TIME(), :email, :name, :password)"
            , nativeQuery = true)   // CURRENT_TIME() 사용을 위해 nativeQuery = true 설정
    int customSave(
                   @Param("email") String email,
                   @Param("name") String name,
                   @Param("password") String password);
}
