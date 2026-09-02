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
                        // /api/saju/**보다 먼저 와야 한다 — authorizeHttpRequests는
                        // 선언 순서대로 첫 매치를 쓰므로, 더 구체적인 규칙을 넓은
                        // permitAll(/api/saju/**)보다 앞에 둬야 여기만 인증을 요구한다.
                        // 로그인 사용자의 서버 저장 기록이라 비로그인으로 열람 못 하게 막는다.
                        .requestMatchers("/api/saju/history").authenticated()
                        // /error도 permitAll — 아니면 permitAll 경로에서 발생한 에러가
                        // 컨테이너의 /error 포워딩을 타는 순간 Security가 그 요청(인증
                        // 안 된 /error 요청)을 다시 막아 원래 상태 코드(404/405 등) 대신
                        // 401로 덮어써 버린다(예: GET /api/saju/breakup → 405가 아니라 401).
                        .requestMatchers(
                                "/api/saju/**", "/actuator/health", "/oauth2/**", "/login/**",
                                "/api/auth/dev-admin-login", "/error")
                                .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
