package com.test.basic.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<UserEntity> findByEmail(String email);

//    @Query(value = "SELECT * FROM users WHERE name LIKE %:name%", nativeQuery = true)
    @Query(value = "SELECT * FROM users WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    Optional<UserEntity> findByName(String name); // 사용자의 username으로 조회

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
