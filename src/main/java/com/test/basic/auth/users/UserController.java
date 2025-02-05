package com.test.basic.auth.users;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/users")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User newUser = userService.createUser(user);

        // 생성한 사용자 정보 조회 URI 반환
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(newUser);
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam int page,
                                                  @RequestParam(value = "size", defaultValue = "10") int size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false, defaultValue = "id,asc") String sort) {

        List<User> users = userService.getAllUsers(page, size, keyword, sort);

        return ResponseEntity.ok().body(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);

        if (user.isPresent()) {
            return ResponseEntity.ok().body(user.get());
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @RequestBody User user) {

        Optional<User> updatedUser = userService.updateUser(id, user);

        if (updatedUser.isPresent()) {
            return ResponseEntity.ok().body(updatedUser.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // INFO
    // deleteUserById 메서드가 내부적으로 예외를 던진다고 하더라도,
    // 컨트롤러 메서드에 throws를 명시하지 않아도
    // 런타임 예외는 전파되어 전역 예외 처리기에 의해 처리됩니다.
    @DeleteMapping("/{id}")
    public ResponseEntity deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
