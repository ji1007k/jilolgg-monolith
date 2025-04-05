package com.test.basic.auth.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/*
    Spring Security가 JWT 파싱한 후, Authentication 객체로 주입할 때
    그 안에 들어가는 Principal로 UserDetails 타입 데이터를 등록할 수 있음
    그래서 @AuthenticationPrincipal 쓰려면
    우리가 커스텀해서 원하는 필드(예: email, userId 등)를 담은 CustomUserDetails 클래스가 필요해.
    => JWT 기반 인증 후 사용자 정보 바로 꺼낼 수 있음
 */
public class CustomUserDetails implements UserDetails {

    @Getter
    private final Long id;
    @Getter
    private final String email;
    private final String password;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String email, String password, String username, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
