package com.example.chat_server.repository;

import com.example.chat_server.model.PendingRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PendingRegistrationRepository extends MongoRepository<PendingRegistration, String> {
}
