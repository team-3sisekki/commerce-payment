package org.example.commercepayment.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.commercepayment.global.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toStaticResources;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // CSRF는 주로 쿠키 기반 세션 인증에서 문제가 되므로, Authorization 헤더 기반 JWT 인증에서는 필요성이 낮다.
                // 단, 브라우저에서 쿠키를 함께 쓰는 구조라면 다시 검토해야 한다.
                .csrf(AbstractHttpConfigurer::disable)

                // 브라우저 기본 팝업을 유발하는 Basic 인증을 사용하지 않기 위해 비활성화한다.
                .httpBasic(AbstractHttpConfigurer::disable)

                // 서버 렌더링 로그인 폼을 사용하지 않고, JSON API로 로그인(POST /api/auth/login) 후 JWT를 발급하는 방식이므로 비활성화한다.
                .formLogin(AbstractHttpConfigurer::disable)

                // sessionCreationPolicy(SessionCreationPolicy.STATELESS)로 서버 세션을 쓰지 않도록 고정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증이 필요한 API에 토큰이 없거나 유효하지 않을 때 호출된다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"AUTH_001\",\"message\":\"인증이 필요합니다\"}");
                        })
                )

                // “경로별 접근 정책”을 정의한다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/favicon.*").permitAll()
                        .requestMatchers("/", "/login", "/signup", "/products/**", "/cart", "/orders/**", "/checkout", "/webhooks").permitAll()
                        .requestMatchers("/api/auth/**", "/api/products/**", "/api/webhooks", "/api/webhooks/**", "/api/config/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )

                // 컨트롤러 진입 전, JwtAuthFilter가 Authorization 헤더에서 토큰을 꺼내 검증한다.
                // 검증 성공 시 SecurityContextHolder에 Authentication이 저장되고, 이후 인가 판단에서 authenticated() 조건을 만족
                // 컨트롤러에서는 @AuthenticationPrincipal Long memberId처럼 인증 주체를 바로 사용할 수 있다.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
