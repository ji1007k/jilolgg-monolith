package com.test.basic.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<UserEntity> findByEmail(String email);

//    @Query(value = "SELECT * FROM users WHERE name LIKE %:name%", nativeQuery = true)
    @Query(value = "SELECT * FROM users WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    Optional<UserEntity> findByName(String name); // 사용자의 username으로 조회
}
