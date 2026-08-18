package org.ai.clinic.example.controller;

import jakarta.validation.Valid;
import org.ai.clinic.example.dto.AskResponse;
import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.ai.clinic.example.service.ClinicChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clinic")
public class ClinicChatController {

    private static final Logger logger = LoggerFactory.getLogger(ClinicChatController.class);

    private final ClinicChatService clinicChatService;

    public ClinicChatController(ClinicChatService clinicChatService) {
        this.clinicChatService = clinicChatService;
    }

    /**
     * GET /clinic/ask?query=When is Dr. Smith available this week?
     *
     * <p>Single-turn question, runs the full RAG pipeline and returns the model answer.
     */
    @GetMapping("/ask")
    public AskResponse ask(@RequestParam("query") String query) {
        logger.debug("Received clinic query: {}", query);
        ChatCompletionResponse response = clinicChatService.complete(ChatCompletionRequest.ofUserMessage(query));
        return new AskResponse(query, response.firstContent(), response.model());
    }

    /**
     * GET /clinic/ask/text?query=When is Dr. Smith available this week? — plain text answer only.
     */
    @GetMapping(value = "/ask/text", produces = "text/plain;charset=UTF-8")
    public String askText(@RequestParam("query") String query) {
        logger.debug("Received clinic query (text): {}", query);
        return clinicChatService.ask(query);
    }

    /**
     * POST /clinic/chat — pass a full messages payload (conversation history) through
     * the clinic RAG pipeline (SQL generation + policy lookup + final answer).
     */
    @PostMapping("/chat")
    public ChatCompletionResponse chat(@Valid @RequestBody ChatCompletionRequest request) {
        logger.info("Received clinic chat request with {} message(s)",
                request.messages() == null ? 0 : request.messages().size());
        return clinicChatService.complete(request);
    }
}