package com.openai.springopenai.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatGptResponse {

    private String answer;

    private List<String> context;

}
