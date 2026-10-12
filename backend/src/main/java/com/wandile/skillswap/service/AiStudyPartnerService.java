package com.wandile.skillswap.service;

import com.wandile.skillswap.dto.DirectMessageResponse;
import com.wandile.skillswap.model.Conversation;
import com.wandile.skillswap.model.ConversationType;
import com.wandile.skillswap.model.DirectMessage;
import com.wandile.skillswap.model.User;
import com.wandile.skillswap.repository.ConversationParticipantRepository;
import com.wandile.skillswap.repository.ConversationRepository;
import com.wandile.skillswap.repository.DirectMessageRepository;
import com.wandile.skillswap.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiStudyPartnerService {

    private static final Logger log = LoggerFactory.getLogger(AiStudyPartnerService.class);

    static final String BOT_EMAIL = "study-partner@skillswap.invalid";
    static final String BOT_NAME = "Study Partner (AI)";

    private static final int HISTORY_LIMIT = 12;
    private static final int MAX_REPLIES_PER_USER_PER_HOUR = 20;
    private static final int MAX_REPLIES_PER_DAY = 300;

    private static final String GREETING =
            "Hi! I'm an AI study partner. Tell me which concept you're working on, or share what you "
                    + "understand so far, and we'll break it down together. I'll ask questions to help you reason "
                    + "it through instead of just handing over answers. Note: your messages here are processed by "
                    + "a third-party AI provider, so please don't share personal information.";

    private static final String SYSTEM_PROMPT =
            "You are a friendly study partner for students learning programming. Explain concepts step by "
                    + "step using simple language and small examples. Ask a guiding question when it helps the "
                    + "student think. Do not simply hand over complete solutions to assignments; guide the student "
                    + "toward them. Keep replies under about 200 words and use plain text, not markdown.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final List<LlmProvider> providers;
    private final String preferredProvider;

    private final Map<Long, Deque<Instant>> recentRepliesByUser = new ConcurrentHashMap<>();
    private LocalDate dailyDate = LocalDate.now();
    private int dailyCount = 0;

    public AiStudyPartnerService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 ConversationRepository conversationRepository,
                                 ConversationParticipantRepository participantRepository,
                                 DirectMessageRepository messageRepository,
                                 SimpMessagingTemplate messagingTemplate,
                                 NotificationService notificationService,
                                 List<LlmProvider> providers,
                                 @Value("${ai.provider:groq}") String preferredProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.providers = providers;
        this.preferredProvider = preferredProvider;
    }

    public synchronized User getBotUser() {
        return userRepository.findByEmail(BOT_EMAIL).orElseGet(() ->
                userRepository.save(new User(BOT_NAME, BOT_EMAIL,
                        passwordEncoder.encode(UUID.randomUUID().toString()))));
    }

    public void postGreetingIfNew(Long conversationId) {
        if (!messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId).isEmpty()) return;
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        saveAndBroadcast(conversation, getBotUser(), GREETING);
    }

    @Async
    public void replyIfAddressed(Long conversationId, Long senderId) {
        try {
            User bot = getBotUser();
            if (bot.getId().equals(senderId)) return;

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null || conversation.getType() != ConversationType.ONE_ON_ONE) return;
            boolean botIsHere = participantRepository.findByConversation_Id(conversationId).stream()
                    .anyMatch(p -> p.getLeftAt() == null && p.getUser().getId().equals(bot.getId()));
            if (!botIsHere) return;

            List<LlmProvider.ChatTurn> turns = buildTurns(conversationId, bot.getId());
            if (turns.isEmpty() || turns.get(turns.size() - 1).fromAssistant()) return;

            String reply;
            if (providers.stream().noneMatch(LlmProvider::isConfigured)) {
                reply = "The AI study partner isn't switched on for this server yet.";
            } else if (!allowReply(senderId)) {
                reply = "We've hit the limit for AI replies for now. Please try again a bit later.";
            } else {
                reply = generateWithFallback(turns);
            }

            if (reply.length() > 1900) reply = reply.substring(0, 1900) + "…";
            saveAndBroadcast(conversation, bot, reply);

            String preview = reply.length() > 60 ? reply.substring(0, 60) + "..." : reply;
            notificationService.notifyNewDirectMessage(senderId, conversationId, bot.getName(), preview);
        } catch (Exception e) {
            log.error("AI study partner failed to reply in conversation {}", conversationId, e);
        }
    }

    private String generateWithFallback(List<LlmProvider.ChatTurn> turns) {
        List<LlmProvider> order = new ArrayList<>();
        for (LlmProvider p : providers) {
            if (p.isConfigured() && p.name().equalsIgnoreCase(preferredProvider)) order.add(p);
        }
        for (LlmProvider p : providers) {
            if (p.isConfigured() && !order.contains(p)) order.add(p);
        }
        for (LlmProvider provider : order) {
            try {
                String reply = provider.generate(SYSTEM_PROMPT, turns);
                log.info("AI reply generated by {}", provider.name());
                return reply;
            } catch (Exception e) {
                log.warn("AI provider {} failed: {}", provider.name(), e.getMessage());
            }
        }
        return "Sorry, I couldn't reach my AI service just now. Please try again in a moment.";
    }

    private List<LlmProvider.ChatTurn> buildTurns(Long conversationId, Long botId) {
        List<DirectMessage> all = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        List<DirectMessage> recent = all.subList(Math.max(0, all.size() - HISTORY_LIMIT), all.size());

        List<LlmProvider.ChatTurn> turns = new ArrayList<>();
        for (DirectMessage message : recent) {
            boolean fromBot = message.getSender().getId().equals(botId);
            if (turns.isEmpty() && fromBot) continue;
            if (!turns.isEmpty() && turns.get(turns.size() - 1).fromAssistant() == fromBot) {
                LlmProvider.ChatTurn last = turns.remove(turns.size() - 1);
                turns.add(new LlmProvider.ChatTurn(fromBot, last.text() + "\n" + message.getContent()));
            } else {
                turns.add(new LlmProvider.ChatTurn(fromBot, message.getContent()));
            }
        }
        return turns;
    }

    private synchronized boolean allowReply(Long userId) {
        LocalDate today = LocalDate.now();
        if (!today.equals(dailyDate)) {
            dailyDate = today;
            dailyCount = 0;
        }
        if (dailyCount >= MAX_REPLIES_PER_DAY) return false;

        Deque<Instant> recent = recentRepliesByUser.computeIfAbsent(userId, k -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) recent.pollFirst();
        if (recent.size() >= MAX_REPLIES_PER_USER_PER_HOUR) return false;

        recent.addLast(Instant.now());
        dailyCount++;
        return true;
    }

    private void saveAndBroadcast(Conversation conversation, User sender, String content) {
        DirectMessage saved = messageRepository.save(new DirectMessage(conversation, sender, content));
        messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId() + "/messages",
                new DirectMessageResponse(saved.getId(), conversation.getId(), sender.getId(),
                        sender.getName(), saved.getContent(), saved.getCreatedAt()));
    }
}