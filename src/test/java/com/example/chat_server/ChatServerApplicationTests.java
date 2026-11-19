package com.example.chat_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

// Cần Docker để chạy MongoDB; không có Docker thì test được bỏ qua (skipped), không báo lỗi
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ChatServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
