package com.openai.springopenai.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class QueryResult {

    private List<Float> relevantEmbeddings;

    private String message;

}
