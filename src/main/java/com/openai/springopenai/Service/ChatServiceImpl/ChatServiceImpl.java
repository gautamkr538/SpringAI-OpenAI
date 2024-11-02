package com.openai.springopenai.Service.ChatServiceImpl;

import com.openai.springopenai.Config.FileProcessingException;
import com.openai.springopenai.DTO.ChatGptResponse;
import com.openai.springopenai.Service.ChatService;
import com.openai.springopenai.repository.CombinedRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);


    private final CombinedRepository combinedRepository;
    public ChatServiceImpl(@Lazy CombinedRepository combinedRepository) {
        this.combinedRepository = combinedRepository;
    }

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.api-url}")
    private String apiUrl;

    @Override
    public void processAndStorePdf(MultipartFile file) {
        try {
            String pdfText = extractTextFromPdf(file);
            logger.info("Extracted text from PDF: {}", pdfText);

            List<Float> embeddings = convertTextToEmbeddings(pdfText);
            logger.info("Converted text to embeddings: {}", embeddings);

            combinedRepository.storeEmbeddings(embeddings);
            logger.info("Stored embeddings in the vector database.");
        } catch (Exception e) {
            logger.error("Error processing PDF: {}", e.getMessage(), e);
            throw new FileProcessingException("Failed to process and store PDF.");
        }
    }

    @Override
    public String handleQuery(String question) {
        logger.info("Querying the vector database for: {}", question);
        String resultFromPdf = String.valueOf(combinedRepository.queryEmbeddings(question));

        if (resultFromPdf == null || resultFromPdf.isEmpty()) {
            logger.info("No relevant info found in PDF, querying ChatGPT...");
            ChatGptResponse chatGptResponse = callChatGptApi(question);
            return chatGptResponse.getAnswer();
        }
        logger.info("Found relevant information in PDF.");
        return resultFromPdf;
    }


    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        } catch (IOException e) {
            logger.error("Error extracting text from PDF: {}", e.getMessage());
            throw e;
        }
    }

    public List<Float> convertTextToEmbeddings(String text) {
        List<Float> embeddings = new ArrayList<>();

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "text-embedding-ada-002");
            requestBody.put("input", text);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JSONObject> response = restTemplate.postForEntity(apiUrl, request, JSONObject.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject responseBody = response.getBody();
                assert responseBody != null;
                JSONArray embeddingArray = responseBody.getJSONArray("data").getJSONObject(0).getJSONArray("embedding");
                for (int i = 0; i < embeddingArray.length(); i++) {
                    embeddings.add((float) embeddingArray.getDouble(i));
                }
            }
        } catch (Exception e) {
            logger.error("Error converting text to embeddings: {}", e.getMessage());
        }

        return embeddings;
    }

    @Override
    public ChatGptResponse callChatGptApi(String question) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        JSONObject requestBody = new JSONObject();
        requestBody.put("prompt", question);
        requestBody.put("max_tokens", 1000);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(apiUrl, request, JSONObject.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Received response from ChatGPT.");
            JSONObject responseBody = response.getBody();
            assert responseBody != null;
            String answer = responseBody.getString("answer");
            return new ChatGptResponse(answer, null);
        } else {
            logger.error("Error calling ChatGPT: {}", response.getStatusCode());
            return new ChatGptResponse("Error querying ChatGPT", null);
        }
    }
}
