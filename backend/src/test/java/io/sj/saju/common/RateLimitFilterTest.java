package io.sj.saju.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 순수 인메모리 필터라 Spring 컨텍스트/DB 없이 단위 테스트로 충분하다. */
class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter();

    @Test
    void requestsWithinLimitAllPassThrough() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = readingRequest("203.0.113.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void the11thRequestFromTheSameIpWithinTheWindowIsRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(readingRequest("203.0.113.2"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse eleventh = new MockHttpServletResponse();
        filter.doFilter(readingRequest("203.0.113.2"), eleventh, new MockFilterChain());

        assertThat(eleventh.getStatus()).isEqualTo(429);
    }

    @Test
    void differentIpsAreTrackedIndependently() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(readingRequest("203.0.113.3"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse fromAnotherIp = new MockHttpServletResponse();
        filter.doFilter(readingRequest("203.0.113.4"), fromAnotherIp, new MockFilterChain());

        assertThat(fromAnotherIp.getStatus()).isEqualTo(200);
    }

    @Test
    void unrelatedEndpointsAreNeverRateLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/billing/packages");
        request.setRemoteAddr("203.0.113.5");

        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletRequest readingRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/saju/breakup");
        request.setRemoteAddr(ip);
        return request;
    }
}
