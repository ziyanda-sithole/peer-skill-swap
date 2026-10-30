package com.wandile.skillswap;

import com.wandile.skillswap.dto.*;
import com.wandile.skillswap.model.*;
import com.wandile.skillswap.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AllServicesVerificationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private StudySessionService sessionService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private AiStudyPartnerService aiStudyPartnerService;

    @Test
    void testUserServiceMethods() {
        // Register user 1
        RegisterRequest regReq1 = new RegisterRequest();
        regReq1.setName("Alice");
        regReq1.setEmail("alice@test.com");
        regReq1.setPassword("password123");
        AuthResponse user1 = userService.register(regReq1);
        assertNotNull(user1.getId());
        assertEquals("Alice", user1.getName());

        // Duplicate email should throw
        assertThrows(IllegalArgumentException.class, () -> userService.register(regReq1));

        // Login valid
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("alice@test.com");
        loginReq.setPassword("password123");
        AuthResponse loginRes = userService.login(loginReq);
        assertEquals(user1.getId(), loginRes.getId());

        // Login wrong password
        loginReq.setPassword("wrongpass");
        assertThrows(IllegalArgumentException.class, () -> userService.login(loginReq));

        // Login nonexistent email
        loginReq.setEmail("nobody@test.com");
        assertThrows(IllegalArgumentException.class, () -> userService.login(loginReq));
    }

    @Test
    void testPostAndMatchMethods() {
        RegisterRequest regReq2 = new RegisterRequest();
        regReq2.setName("Bob");
        regReq2.setEmail("bob@test.com");
        regReq2.setPassword("password123");
        AuthResponse bob = userService.register(regReq2);

        RegisterRequest regReq3 = new RegisterRequest();
        regReq3.setName("Charlie");
        regReq3.setEmail("charlie@test.com");
        regReq3.setPassword("password123");
        AuthResponse charlie = userService.register(regReq3);

        // Bob creates a post
        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setType(PostType.OFFER);
        postReq.setSkillTag("Python");
        postReq.setDescription("Teaching Python basics");
        PostResponse post = postService.createPost(bob.getId(), postReq);
        assertNotNull(post.getId());
        assertEquals("Python", post.getSkillTag());

        // List posts
        List<PostResponse> allPosts = postService.listPosts(null);
        assertFalse(allPosts.isEmpty());
        List<PostResponse> offerPosts = postService.listPosts(PostType.OFFER);
        assertFalse(offerPosts.isEmpty());
        List<PostResponse> requestPosts = postService.listPosts(PostType.NEED);
        assertTrue(requestPosts.isEmpty());

        // Bob cannot respond to his own post
        RespondRequest respReq = new RespondRequest();
        respReq.setMessage("I want to learn");
        assertThrows(IllegalArgumentException.class, () -> matchService.respond(post.getId(), bob.getId(), respReq));

        // Charlie responds to Bob's post
        MatchResponse match = matchService.respond(post.getId(), charlie.getId(), respReq);
        assertNotNull(match.getId());
        assertEquals(MatchStatus.PENDING, match.getStatus());

        // Charlie cannot accept the match (only post owner can)
        assertThrows(IllegalArgumentException.class, () -> matchService.accept(match.getId(), charlie.getId()));

        // Bob accepts the match
        MatchResponse acceptedMatch = matchService.accept(match.getId(), bob.getId());
        assertEquals(MatchStatus.ACCEPTED, acceptedMatch.getStatus());

        // List matches for Bob and Charlie
        List<MatchResponse> bobMatches = matchService.listForUser(bob.getId());
        assertFalse(bobMatches.isEmpty());
        List<MatchResponse> charlieMatches = matchService.listForUser(charlie.getId());
        assertFalse(charlieMatches.isEmpty());
    }

    @Test
    void testStudySessionLifecycleAndDeletion() {
        RegisterRequest hostReq = new RegisterRequest();
        hostReq.setName("Host User");
        hostReq.setEmail("host_lifecycle@test.com");
        hostReq.setPassword("password123");
        AuthResponse host = userService.register(hostReq);

        RegisterRequest memberReq = new RegisterRequest();
        memberReq.setName("Member User");
        memberReq.setEmail("member_lifecycle@test.com");
        memberReq.setPassword("password123");
        AuthResponse member = userService.register(memberReq);

        // 1. Host creates session
        CreateSessionRequest sessReq = new CreateSessionRequest();
        sessReq.setTitle("Algorithms Workshop");
        sessReq.setTopic("Sorting");
        sessReq.setDescription("Sorting algorithms analysis");
        sessReq.setVisibility(SessionVisibility.PUBLIC);
        sessReq.setType(SessionType.GROUP);
        SessionResponse session = sessionService.createSession(host.getId(), sessReq);
        assertNotNull(session.getId());

        // 2. List public & my sessions
        List<SessionResponse> publicSessions = sessionService.listPublicSessions();
        assertTrue(publicSessions.stream().anyMatch(s -> s.getId().equals(session.getId())));
        List<SessionResponse> mySessions = sessionService.listMySessions(host.getId());
        assertTrue(mySessions.stream().anyMatch(s -> s.getId().equals(session.getId())));

        // 3. Member requests to join
        MembershipResponse joinReq = sessionService.requestToJoin(session.getId(), member.getId());
        assertEquals(MembershipStatus.PENDING, joinReq.getStatus());

        // 4. Pending requests list
        List<MembershipResponse> pending = sessionService.listPendingRequests(session.getId(), host.getId());
        assertEquals(1, pending.size());

        // 5. Host approves request
        MembershipResponse approved = sessionService.decideRequest(session.getId(), member.getId(), host.getId(), true);
        assertEquals(MembershipStatus.APPROVED, approved.getStatus());

        // 6. List members
        List<MembershipResponse> members = sessionService.listMembers(session.getId(), host.getId());
        assertEquals(2, members.size());

        // 7. Promote member to admin
        MembershipResponse promoted = sessionService.promoteToAdmin(session.getId(), member.getId(), host.getId());
        assertEquals(MembershipRole.ADMIN, promoted.getRole());

        // 8. Chat messages
        SendMessageRequest msgReq = new SendMessageRequest();
        msgReq.setContent("Hello everyone!");
        ChatMessageResponse chatRes = chatService.sendMessage(session.getId(), host.getId(), msgReq);
        assertNotNull(chatRes.getId());
        List<ChatMessageResponse> messages = chatService.listMessages(session.getId(), member.getId());
        assertEquals(1, messages.size());

        // 9. Host tries to delete session while other approved member is present -> should throw
        assertThrows(IllegalArgumentException.class, () -> sessionService.deleteSession(session.getId(), host.getId()));

        // 10. Non-host tries to delete session -> should throw
        assertThrows(IllegalArgumentException.class, () -> sessionService.deleteSession(session.getId(), member.getId()));

        // 11. Remove the member
        assertDoesNotThrow(() -> sessionService.removeMember(session.getId(), member.getId(), host.getId()));

        // 12. Host deletes session now that there's no one else in it -> MUST SUCCEED cleanly!
        assertDoesNotThrow(() -> sessionService.deleteSession(session.getId(), host.getId()));
    }

    @Test
    void testDirectAndGroupConversations() {
        RegisterRequest userReq1 = new RegisterRequest();
        userReq1.setName("User One");
        userReq1.setEmail("user1_dm@test.com");
        userReq1.setPassword("password123");
        AuthResponse u1 = userService.register(userReq1);

        RegisterRequest userReq2 = new RegisterRequest();
        userReq2.setName("User Two");
        userReq2.setEmail("user2_dm@test.com");
        userReq2.setPassword("password123");
        AuthResponse u2 = userService.register(userReq2);

        // Start direct conversation
        ConversationResponse direct = conversationService.startDirectConversation(u1.getId(), u2.getId());
        assertNotNull(direct.getId());
        assertEquals(ConversationType.ONE_ON_ONE, direct.getType());

        // Calling again reuses the existing conversation
        ConversationResponse directReused = conversationService.startDirectConversation(u1.getId(), u2.getId());
        assertEquals(direct.getId(), directReused.getId());

        // Send message
        SendDirectMessageRequest dmReq = new SendDirectMessageRequest();
        dmReq.setContent("Hey User Two!");
        DirectMessageResponse dmRes = conversationService.sendMessage(direct.getId(), u1.getId(), dmReq);
        assertNotNull(dmRes.getId());

        // List messages
        List<DirectMessageResponse> dmMessages = conversationService.listMessages(direct.getId(), u2.getId());
        assertEquals(1, dmMessages.size());

        // List my conversations
        List<ConversationResponse> myConvs = conversationService.listMyConversations(u1.getId());
        assertFalse(myConvs.isEmpty());

        // Leave conversation
        assertDoesNotThrow(() -> conversationService.leaveConversation(direct.getId(), u2.getId()));

        // AI bot user & greeting
        User bot = aiStudyPartnerService.getBotUser();
        assertNotNull(bot);
        ConversationResponse aiConv = conversationService.startDirectConversation(u1.getId(), bot.getId());
        assertDoesNotThrow(() -> aiStudyPartnerService.postGreetingIfNew(aiConv.getId()));
    }
}
