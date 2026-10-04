package com.example.chat_server.service;

import com.example.chat_server.exception.ApiException;
import com.example.chat_server.exception.ErrorCode;
import com.example.chat_server.model.AuthProvider;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UsernameGenerator usernameGenerator;
    private final String clientId;
    private final JwtDecoder googleIdTokenDecoder;

    public GoogleAuthService(UserRepository userRepository,
                             TokenService tokenService,
                             UsernameGenerator usernameGenerator,
                             @Value("${app.google.client-id:}") String clientId) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.usernameGenerator = usernameGenerator;
        this.clientId = clientId;

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString())),
                // Without the audience check, an ID token issued to ANY Google app would be accepted
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(clientId))));
        this.googleIdTokenDecoder = decoder;
    }

    public TokenService.IssuedTokens login(String idToken) {
        if (clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_ERROR,
                    "Google sign-in is not configured (GOOGLE_CLIENT_ID is missing)");
        }

        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("Rejected Google ID token: {}", e.getMessage());
            throw invalidToken("Google ID token is invalid or expired");
        }

        String rawEmail = jwt.getClaimAsString("email");
        if (rawEmail == null || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw invalidToken("The Google account's email is not verified");
        }
        String email = rawEmail.toLowerCase(Locale.ROOT);
        String googleId = jwt.getSubject();

        Optional<User> linked = userRepository.findByGoogleId(googleId);
        if (linked.isPresent()) {
            return tokenService.issueTokens(linked.get());
        }

        // Both sides have verified ownership of the email (OTP here, email_verified at Google), so an existing
        // account with this email is linked to Google automatically instead of answering ACCOUNT_LINK_REQUIRED
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return tokenService.issueTokens(linkGoogle(existing.get(), googleId));
        }

        return tokenService.issueTokens(createGoogleUser(jwt, email, googleId), true);
    }

    private User linkGoogle(User user, String googleId) {
        if (user.getGoogleId() != null) {
            // The email already belongs to a different Google account (Google changed the subject, or a reused address)
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ACCOUNT_LINK_REQUIRED,
                    "This email is already linked to another Google account");
        }
        user.setGoogleId(googleId);
        return userRepository.save(user);
    }

    private User createGoogleUser(Jwt jwt, String email, String googleId) {
        String fullName = jwt.getClaimAsString("name");
        String locale = jwt.getClaimAsString("locale");

        User user = new User();
        user.setUsername(usernameGenerator.fromEmail(email));
        user.setEmail(email);
        user.setFullName(fullName != null && !fullName.isBlank() ? fullName : email.substring(0, email.indexOf('@')));
        user.setLanguage(locale != null && locale.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "vi");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setGoogleId(googleId);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private static ApiException invalidToken(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.GOOGLE_TOKEN_INVALID, message);
    }
}
