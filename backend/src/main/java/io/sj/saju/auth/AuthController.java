package io.sj.saju.auth;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserAccountRepository userAccountRepository;

    public AuthController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /** For the frontend to check "am I logged in" and show a nickname — nothing else. */
    @GetMapping("/api/auth/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return userAccountRepository.findById(userAccountId)
                .map(account -> ResponseEntity.ok(
                        new MeResponse(account.getProvider().name(), account.getNickname())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    public record MeResponse(String provider, String nickname) {
    }
}
