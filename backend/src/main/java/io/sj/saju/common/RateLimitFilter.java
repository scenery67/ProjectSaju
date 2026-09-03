package io.sj.saju.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 최소한의 인메모리 요청 속도 제한. 특정 엔드포인트에만 적용한다 — 전체
 * {@code /api/**}에 걸면 정상적인 폴링(내 정보 조회 등)까지 막을 수 있어서,
 * 실제로 자원을 많이 쓰는(사주 계산, LLM 호출) 곳만 고른다:
 * <ul>
 *   <li>{@code /api/saju/breakup}, {@code /api/saju/couple-compatibility} —
 *       로그인 없이도 호출 가능해서(anonymous), 스크립트로 반복 호출해도
 *       막을 다른 수단이 없다.</li>
 *   <li>{@code /api/consultation/sessions/*}{@code /messages} — 크레딧으로
 *       이미 막혀 있지만, 계정이 뚫렸을 때 OpenAI 비용이 그대로 새는 걸
 *       막는 2차 방어선이다.</li>
 * </ul>
 * Fly 머신 1대 안에서만 유효한 메모리 상태라 여러 머신에 걸쳐 합산되진
 * 않는다 — "완벽한 차단"이 아니라 "스크립트성 남용에 대한 1차 저지선"이
 * 목적이다. IP당 항목이 한 번 생기면 정리되지 않고 남는데, 대상 엔드포인트가
 * 소수라 이 앱 규모에서는 문제가 되지 않는다(트래픽이 커지면 그때 정리
 * 로직이나 외부 저장소로 옮긴다).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String pathPattern, String method, int maxRequests, Duration window) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("/api/saju/breakup", "POST", 10, Duration.ofMinutes(1)),
            new Rule("/api/saju/couple-compatibility", "POST", 10, Duration.ofMinutes(1)),
            new Rule("/api/consultation/sessions/*/messages", "POST", 20, Duration.ofMinutes(1)));

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, Deque<Long>> hitsByKey = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Rule rule = matchingRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.pathPattern() + "|" + clientIp(request);
        if (isOverLimit(key, rule)) {
            // HttpServletResponse에는 429 상수가 없다(서블릿 스펙에 원래 없던 코드).
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limited\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Rule matchingRule(HttpServletRequest request) {
        for (Rule rule : RULES) {
            if (rule.method().equals(request.getMethod())
                    && pathMatcher.match(rule.pathPattern(), request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    private boolean isOverLimit(String key, Rule rule) {
        long now = System.currentTimeMillis();
        long windowStart = now - rule.window().toMillis();
        Deque<Long> timestamps = hitsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= rule.maxRequests()) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    // Fly 엣지 프록시가 실제 클라이언트 IP를 X-Forwarded-For로 넘긴다
    // (server.forward-headers-strategy와 별개로, 이 필터는 서블릿 API를
    // 직접 쓰므로 헤더를 직접 읽는다). 없으면(로컬 개발 등) getRemoteAddr로.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
