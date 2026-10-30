package com.wandile.skillswap;

import com.wandile.skillswap.dto.CreateSessionRequest;
import com.wandile.skillswap.dto.SessionResponse;
import com.wandile.skillswap.model.SessionType;
import com.wandile.skillswap.model.SessionVisibility;
import com.wandile.skillswap.model.User;
import com.wandile.skillswap.repository.UserRepository;
import com.wandile.skillswap.service.StudySessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StudySessionServiceTest {

    @Autowired
    private StudySessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDeleteSessionWhenAlone() {
        User host = userRepository.save(new User("Host User", "host@test.com", "hash"));
        CreateSessionRequest req = new CreateSessionRequest();
        req.setTitle("Test Session");
        req.setTopic("Java");
        req.setDescription("Let's study");
        req.setVisibility(SessionVisibility.PUBLIC);
        req.setType(SessionType.GROUP);

        SessionResponse session = sessionService.createSession(host.getId(), req);
        assertNotNull(session);

        // Try deleting the session as host when alone
        assertDoesNotThrow(() -> sessionService.deleteSession(session.getId(), host.getId()));
    }
}
