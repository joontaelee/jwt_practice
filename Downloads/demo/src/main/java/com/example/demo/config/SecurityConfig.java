package com.example.demo.config;

import com.example.demo.jwt.LoginFilter; // 커스텀 필터 (LoginFilter) 클래스 임포트
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // AuthenticationManager가 AuthenticationConfiguration 객체를 사용하여 설정되기 때문에, 이를 생성자 주입
    private final AuthenticationConfiguration authenticationConfiguration;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration) {
        // AuthenticationConfiguration은 인증 매니저를 생성하기 위한 필수 요소
        this.authenticationConfiguration = authenticationConfiguration;
    }

    // AuthenticationManager를 Bean으로 등록
    // AuthenticationManager는 스프링 시큐리티에서 인증을 담당하는 주요 컴포넌트
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // AuthenticationConfiguration에서 AuthenticationManager 인스턴스를 생성
        return configuration.getAuthenticationManager();
    }

    // 비밀번호를 암호화하기 위해 BCryptPasswordEncoder Bean 등록
    // 이유: BCrypt는 보안성이 높고 Spring Security에서 권장하는 해시 알고리즘
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // SecurityFilterChain 설정
    // 이유: Spring Security 5.7부터 WebSecurityConfigurerAdapter가 더 이상 권장되지 않음. 대신 SecurityFilterChain을 사용
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CSRF 보호 비활성화
        // 이유: REST API는 보통 상태를 저장하지 않으므로 CSRF 보호가 필요하지 않음
        http.csrf((auth) -> auth.disable());

        // Form 기반 로그인 비활성화
        // 이유: JWT 인증을 사용할 것이기 때문에 기본적인 Form Login은 필요하지 않음
        http.formLogin((auth) -> auth.disable());

        // HTTP Basic 인증 비활성화
        // 이유: JWT 인증을 사용하므로 HTTP Basic 인증은 필요하지 않음
        http.httpBasic((auth) -> auth.disable());

        // 경로별 접근 권한 설정
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/login", "/", "/join").permitAll() // 로그인, 루트, 회원가입 경로는 인증 없이 접근 가능
                .anyRequest().authenticated()); // 나머지 경로는 인증이 필요

        // 커스텀 필터 (LoginFilter) 추가
        // 이유: Spring Security의 기본 필터 대신 JWT 인증을 처리하기 위한 LoginFilter를 사용
        // LoginFilter는 AuthenticationManager를 생성자 인자로 받기 때문에, 이를 주입
        http.addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration)),
                UsernamePasswordAuthenticationFilter.class);

        // 세션 관리 설정
        // 이유: JWT 기반 인증은 세션을 사용하지 않으므로 STATELESS로 설정
        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // SecurityFilterChain 빌드 및 반환
        return http.build();
    }
}
