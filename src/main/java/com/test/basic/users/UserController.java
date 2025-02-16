package com.test.basic.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User API", description = "사용자 관리 API")  // API 그룹 태그
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "사용자 등록", description = "새로운 사용자를 등록합니다.")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserEntity user, HttpSession session) {
        try {
            // 비밀번호 복호화
            String decryptedPwd = userService.decryptPassword(user.getPassword(), session);
            user.setPassword(decryptedPwd);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        UserEntity newUser = userService.createUser(user);
        newUser.setPassword(null);

        // 생성한 사용자 정보 조회 URI 반환
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(newUser);
    }


    @GetMapping
    @Operation(summary = "사용자 목록 조회", description = "사용자 목록을 조회합니다.")
    public ResponseEntity<List<UserEntity>> getAllUsers(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(value = "size", defaultValue = "10") int size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false, defaultValue = "id,asc") String sort) {

        List<UserEntity> users = userService.getAllUsers(page, size, keyword, sort);

        return ResponseEntity.ok().body(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 조회", description = "ID로 사용자를 조회합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @Parameter(name = "id", description = "사용자 ID", required = true)
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        Optional<UserEntity> user = userService.getUserById(id);

        if (user.isPresent()) {
            user.get().setPassword(null);
            return ResponseEntity.ok().body(user.get());
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "사용자 정보 수정", description = "ID로 사용자 정보를 조회합니다.")
    @Parameter(name = "id", description = "사용자 ID", required = true)
    public ResponseEntity<UserEntity> updateUser(@PathVariable Long id,
                                                 @RequestBody UserEntity user) {

        Optional<UserEntity> updatedUser = userService.updateUser(id, user);

        if (updatedUser.isPresent()) {
            return ResponseEntity.ok().body(updatedUser.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/password")
    public ResponseEntity<String> checkPassword(@RequestParam Long id,
                                               @RequestParam String password) {
        boolean isMatched = userService.checkPassword(id, password);
        if (isMatched) {
            return ResponseEntity.ok("비밀번호가 일치합니다.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
        }
    }

    // 비밀번호 변경 API
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@RequestParam Long id,
                                                 @RequestParam String newPassword) {

        boolean success = userService.changePassword(id, newPassword);

        if (success) {
            return ResponseEntity.ok("비밀번호가 변경되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호 변경에 실패했습니다.");
        }
    }

    // INFO
    // deleteUserById 메서드가 내부적으로 예외를 던진다고 하더라도,
    // 컨트롤러 메서드에 throws를 명시하지 않아도
    // 런타임 예외는 전파되어 전역 예외 처리기에 의해 처리됩니다.
    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 삭제", description = "ID로 사용자 정보를 삭제합니다.")
    @Parameter(name = "id", description = "사용자 ID", required = true)
    public ResponseEntity deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/rsa")
    @Operation(summary = "RSA 암호화 키 생성", description = "사용자 정보 암호화를 위한 RSA 암호화 키를 생성합니다.")
    public ResponseEntity<String> generateRSAKeyPair(HttpSession session) {
        try {
            String publicKey = userService.generateRSAKeyPair(session);
            return ResponseEntity.ok().body(publicKey);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
