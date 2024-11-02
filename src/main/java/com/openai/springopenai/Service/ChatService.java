package com.openai.springopenai.Service;

import com.openai.springopenai.DTO.ChatGptResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    void processAndStorePdf(MultipartFile file);

    String handleQuery(String question);
    ChatGptResponse callChatGptApi(String question);

}

