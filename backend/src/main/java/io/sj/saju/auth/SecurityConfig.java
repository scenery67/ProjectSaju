package io.sj.saju.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 사주 계산 API(/api/saju/**)는 로그인 없이 계속 공개로 둔다 — 로그인은 지금은
 * "되는지 확인"만 하는 단계이고, 실제 기능을 로그인 뒤로 잠그는 건 아직 안 한다
 * (2026-09-01 결정, LLM 상담·질문 횟수 과금 설계가 끝난 뒤 붙일 예정).
 *
 * CSRF는 껐다 — 세션 쿠키가 아니라 Authorization 헤더의 JWT로 인증하는
 * API라서, 브라우저가 자동으로 실어 보내는 인증 정보(쿠키)에 기대는 공격인
 * CSRF의 전제 자체가 성립하지 않는다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler successHandler;
    private final OAuth2LoginFailureHandler failureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2LoginSuccessHandler successHandler,
            OAuth2LoginFailureHandler failureHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                // OAuth2 로그인 리다이렉트 왕복 동안만 세션이 필요하다(권한부여 요청 상태 저장).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // Without this, an unauthenticated call to /api/** gets the oauth2Login
                // default entry point — a 302 redirect into the Kakao/Google/Naver login
                // flow — instead of a clean 401 a fetch() caller can actually handle.
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/saju/**", "/actuator/health", "/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
