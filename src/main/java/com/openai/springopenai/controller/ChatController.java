package com.openai.springopenai.controller;

import com.openai.springopenai.DTO.ChatRequest;
import com.openai.springopenai.Service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/upload/file")
    @Operation(summary = "Upload a PDF file", description = "Uploads a PDF file and processes it.")
    public ResponseEntity<String> uploadPdf(
            @Parameter(description = "File to upload in vector database") @RequestParam("file") MultipartFile file) {
        logger.info("Received request to upload PDF.");
        chatService.processAndStorePdf(file);
        return ResponseEntity.ok("PDF uploaded and processed successfully.");
    }
    
    @PostMapping("/chat/query")
    @Operation(summary = "Query the chat model", description = "Queries the chat model with a given question.")
    public ResponseEntity<String> queryChat(
            @Parameter(description = "Question to ask the chat model") @RequestBody ChatRequest chatRequest) {
        logger.info("Received chat query: " + chatRequest.getQuestion());
        String response = chatService.handleQuery(chatRequest.getQuestion());
        return ResponseEntity.ok(response);
    }
}
