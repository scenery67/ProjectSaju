package io.sj.saju.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Loads the OAuth2 user from the provider, normalizes it (see
 * {@link OAuthUserInfo}), and upserts our own {@link UserAccount} row.
 * The user account's id is stashed into the returned OAuth2User's attributes
 * under "sajuUserAccountId" so the success handler can issue a JWT for it
 * without a second DB lookup.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserAccountRepository userAccountRepository;

    public CustomOAuth2UserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = OAuthProvider.fromRegistrationId(registrationId);
        OAuthUserInfo info = OAuthUserInfo.of(provider, oauth2User.getAttributes());

        UserAccount account = userAccountRepository
                .findByProviderAndProviderUserId(provider, info.providerUserId())
                .orElseGet(() -> new UserAccount(provider, info.providerUserId(), info.nickname()));
        account.recordLogin(info.nickname());
        userAccountRepository.save(account);

        Map<String, Object> attributes = new LinkedHashMap<>(oauth2User.getAttributes());
        attributes.put("sajuUserAccountId", account.getId().toString());

        String userNameAttribute = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(oauth2User.getAuthorities(), attributes, userNameAttribute);
    }
}
