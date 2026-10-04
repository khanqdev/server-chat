package com.example.chat_server.repository;

import com.example.chat_server.model.PasswordReset;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetRepository extends MongoRepository<PasswordReset, String> {
}
