package com.compliance.controller;

import com.compliance.model.AssistantConversation;
import com.compliance.model.AssistantMessage;
import com.compliance.model.Obligation;
import com.compliance.model.Task;
import com.compliance.model.Circular;
import com.compliance.repository.AssistantConversationRepository;
import com.compliance.repository.AssistantMessageRepository;
import com.compliance.repository.ObligationRepository;
import com.compliance.repository.TaskRepository;
import com.compliance.repository.CircularRepository;
import com.compliance.security.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final RestTemplate restTemplate;
    private final ObligationRepository obligationRepository;
    private final TaskRepository taskRepository;
    private final CircularRepository circularRepository;
    private final AssistantConversationRepository conversationRepository;
    private final AssistantMessageRepository messageRepository;

    @Value("${app.ai-service.base-url}")
    private String aiServiceUrl;

    public AssistantController(RestTemplate restTemplate,
                               ObligationRepository obligationRepository,
                               TaskRepository taskRepository,
                               CircularRepository circularRepository,
                               AssistantConversationRepository conversationRepository,
                               AssistantMessageRepository messageRepository) {
        this.restTemplate = restTemplate;
        this.obligationRepository = obligationRepository;
        this.taskRepository = taskRepository;
        this.circularRepository = circularRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        String message = (String) request.get("message");
        String conversationIdStr = (String) request.get("conversation_id");

        // Get or create conversation
        AssistantConversation conversation;
        if (conversationIdStr != null) {
            conversation = conversationRepository.findById(UUID.fromString(conversationIdStr))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } else {
            conversation = AssistantConversation.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(message.length() > 50 ? message.substring(0, 50) + "..." : message)
                .build();
            conversationRepository.save(conversation);
        }

        // Save user message
        AssistantMessage userMsg = AssistantMessage.builder()
            .conversationId(conversation.getId())
            .role("user")
            .content(message)
            .build();
        messageRepository.save(userMsg);

        // Build context from real DB data
        String context = buildContext(tenantId);

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("message", message);
        aiRequest.put("tenant_id", tenantId.toString());
        aiRequest.put("context", context);
        aiRequest.put("conversation_id", conversation.getId().toString());

        try {
            var response = restTemplate.postForEntity(
                aiServiceUrl + "/assistant/chat", aiRequest, Map.class);
            Map<String, Object> body = response.getBody();

            // Save assistant response
            AssistantMessage assistantMsg = AssistantMessage.builder()
                .conversationId(conversation.getId())
                .role("assistant")
                .content((String) body.get("content"))
                .modelUsed((String) body.get("model_used"))
                .build();
            messageRepository.save(assistantMsg);

            // Add conversation_id to response
            Map<String, Object> result = new HashMap<>(body);
            result.put("conversation_id", conversation.getId().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("content", "The AI assistant is temporarily unavailable. Please try again shortly.");
            fallback.put("citations", List.of());
            fallback.put("model_used", "fallback");
            fallback.put("conversation_id", conversation.getId().toString());
            return ResponseEntity.ok(fallback);
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> listConversations() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        var conversations = conversationRepository.findByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable UUID id) {
        var messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        return ResponseEntity.ok(messages);
    }

    private String buildContext(UUID tenantId) {
        StringBuilder ctx = new StringBuilder();

        // Recent circulars
        List<Circular> circulars = circularRepository.findAll();
        List<Circular> relevant = circulars.stream().limit(10).toList();
        if (!relevant.isEmpty()) {
            ctx.append("=== Recent Circulars ===\n");
            for (Circular c : relevant) {
                ctx.append(String.format("- %s: %s [Status: %s, Dept: %s]\n",
                    c.getCircularNumber() != null ? c.getCircularNumber() : "N/A",
                    c.getTitle(), c.getStatus(), c.getDepartment()));
            }
            ctx.append("\n");
        }

        // Obligations for this tenant
        var obligations = obligationRepository.findByTenantIdAndReviewStatus(tenantId,
            com.compliance.model.enums.ReviewStatus.PENDING);
        var approved = obligationRepository.findByTenantIdAndReviewStatus(tenantId,
            com.compliance.model.enums.ReviewStatus.APPROVED);
        obligations.addAll(approved);

        if (!obligations.isEmpty()) {
            ctx.append("=== Active Obligations ===\n");
            for (Obligation o : obligations.stream().limit(15).toList()) {
                ctx.append(String.format("- [%s] %s (Severity: %s, Review: %s, Deadline: %s)\n  Detail: %s\n",
                    o.getCircularNumber() != null ? o.getCircularNumber() : "N/A",
                    o.getObligationTitle(), o.getSeverity(), o.getReviewStatus(),
                    o.getDeadline() != null ? o.getDeadline().toString() : "none",
                    o.getObligationDetail() != null ? o.getObligationDetail().substring(0, Math.min(200, o.getObligationDetail().length())) : ""));
            }
            ctx.append("\n");
        }

        // Tasks for this tenant
        var tasks = taskRepository.findByTenantId(tenantId);
        if (!tasks.isEmpty()) {
            ctx.append("=== Tasks ===\n");
            for (Task t : tasks.stream().limit(15).toList()) {
                ctx.append(String.format("- %s [Status: %s, Priority: %s, Dept: %s, Due: %s]\n",
                    t.getTitle(), t.getStatus(), t.getPriority(), t.getDepartment(),
                    t.getDueDate() != null ? t.getDueDate().toString() : "none"));
            }
        }

        return ctx.toString();
    }
}
