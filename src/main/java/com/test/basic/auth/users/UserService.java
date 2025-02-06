package com.test.basic.auth.users;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers(int page, int size, String keyword, String sort) {
        return userRepository.findAll();

    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public Optional<User> updateUser(Long id, User user) {
        Optional<User> existingUser = userRepository.findById(id);

        if (existingUser.isPresent()) {
            User userEntity = existingUser.get();
            userEntity.setEmail(user.getEmail());
            userEntity.setName(user.getName());
            userEntity.setPassword(user.getPassword());
            userEntity.setProfileImageUrl(user.getProfileImageUrl());
            userRepository.save(userEntity);

            return Optional.of(userEntity);
        }

        return Optional.empty();
    }

    public void deleteUser(Long id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        userRepository.deleteById(id);
    }
}
