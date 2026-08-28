package com.test.basic.auth.security.user;

import com.test.basic.user.UserEntity;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;
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

    /**
     * 쉼표로 구분된 권한 문자열을 GrantedAuthority 목록으로 바꾼다.
     *
     * 값이 비어 있으면 기본 권한을 준다. 예전에는 호출부마다 getAuthority().split(",")를
     * 그대로 불렀는데, 권한이 null인 계정에서 NPE가 나고 그게 인증 실패로 감싸여
     * "가입은 되는데 로그인만 401"이 되었다. 로그에도 원인이 남지 않아 추적이 어려웠다.
     */
    public static Collection<GrantedAuthority> parseAuthorities(String authority) {
        String source = StringUtils.hasText(authority) ? authority : UserEntity.DEFAULT_AUTHORITY;

        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Getter
    private final Long id;
    @Getter
    private final String email;
    private final String password;
    private final String username;
    @Getter
    private final Integer passwordVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String email, String password, String username, Integer passwordVersion, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
        this.passwordVersion = passwordVersion;
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
