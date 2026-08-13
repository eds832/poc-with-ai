package org.ai.clinic.example.controller;

import org.ai.clinic.example.dto.AskResponse;
import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.ai.clinic.example.service.AiProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private final AiProxyService aiProxyService;

    public AiController(AiProxyService aiProxyService) {
        this.aiProxyService = aiProxyService;
    }

    /**
     * GET /ai/ask?query=My content
     *
     * <p>Passes the query to the AI proxy and returns the model answer.
     */
    @GetMapping("/ask")
    public AskResponse ask(@RequestParam("query") String query) {
        logger.info("Received AI query: {}", query);
        ChatCompletionResponse response = aiProxyService.complete(ChatCompletionRequest.ofUserMessage(query));
        return new AskResponse(query, response.firstContent(), response.model());
    }

    /**
     * GET /ai/ask/text?query=My content — plain text answer only.
     */
    @GetMapping(value = "/ask/text", produces = "text/plain;charset=UTF-8")
    public String askText(@RequestParam("query") String query) {
        logger.info("Received AI query (text): {}", query);
        return aiProxyService.ask(query);
    }

    /**
     * POST /ai/chat — pass a full messages payload through to the proxy.
     */
    @PostMapping("/chat")
    public ChatCompletionResponse chat(@RequestBody ChatCompletionRequest request) {
        logger.info("Received AI chat request with {} message(s)",
                request.messages() == null ? 0 : request.messages().size());
        return aiProxyService.complete(request);
    }
}
