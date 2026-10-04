package com.example.chat_server.service;

import com.example.chat_server.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// The API no longer asks for a username; the internal handle is derived from the email
@Component
public class UsernameGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    public UsernameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String fromEmail(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._]", "");
        if (base.length() < 3) {
            base = base + "user";
        }
        base = base.substring(0, Math.min(base.length(), 25));

        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + (1000 + RANDOM.nextInt(9000));
        }
        return candidate;
    }
}
