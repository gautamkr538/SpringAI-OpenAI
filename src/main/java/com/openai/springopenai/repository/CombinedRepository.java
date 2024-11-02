package com.openai.springopenai.repository;

import java.util.List;

public interface CombinedRepository {

    void storeEmbeddings(List<Float> embeddings);

    String queryEmbeddings(String question);
}

