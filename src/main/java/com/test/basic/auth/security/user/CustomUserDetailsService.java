package com.test.basic.auth.security.user;

import com.test.basic.user.UserEntity;
import com.test.basic.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

// @Service를 붙여서 CustomUserDetailsService를 빈으로 등록하면,
// Spring Security는 자동으로 CustomUserDetailsService를 사용하여 사용자 인증을 처리
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; // 사용자 정보 조회를 위한 repository

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 이메일 기반으로 사용자 조회
        return loadUserByEmail(email);
    }

    @Transactional
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        // 사용자 정보를 DB에서 조회
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // DB에서 조회한 사용자 정보를 기반으로 UserDetails 객체 생성
        // DB에서 권한 정보 가져오기 (comma-separated values로 가정)
        String[] authorities = user.getAuthority().split(",");
        Collection<GrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                grantedAuthorities
        );
    }

    /*private Collection<? extends GrantedAuthority> getAuthorities(UserEntity user) {
        // 예: DB에 저장된 역할을 기반으로 Authorities 객체 생성
        return Arrays.stream(user.getAuthority().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }*/
}
