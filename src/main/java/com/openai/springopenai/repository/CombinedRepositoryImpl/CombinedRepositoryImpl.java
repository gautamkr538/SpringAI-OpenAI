package com.openai.springopenai.repository.CombinedRepositoryImpl;

import com.openai.springopenai.Service.ChatServiceImpl.ChatServiceImpl;
import com.openai.springopenai.Config.ZillizConfig;
import com.openai.springopenai.repository.CombinedRepository;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResultData;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class CombinedRepositoryImpl implements CombinedRepository {

    private static final Logger log = LoggerFactory.getLogger(CombinedRepositoryImpl.class);
    private MilvusClient milvusClient;

    private final ChatServiceImpl chatServiceImpl;
    private final ZillizConfig zillizConfig;

    public CombinedRepositoryImpl(ChatServiceImpl chatServiceImpl, ZillizConfig zillizConfig) {
        this.chatServiceImpl = chatServiceImpl;
        this.zillizConfig = zillizConfig;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Connecting to Milvus at :{}", zillizConfig.getUri());

            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(zillizConfig.getUri())
                    .withAuthorization("Bearer " + zillizConfig.getApiKey())
                    .withConnectTimeout(120, TimeUnit.SECONDS)
                    .build();

            milvusClient = new MilvusServiceClient(connectParam);
            log.info("Milvus client initialized successfully.");
            createCollection();
        } catch (Exception e) {
            log.error("Error initializing CombinedRepositoryImpl: ", e);
            throw new RuntimeException("Failed to initialize repository", e);
        }
    }

    private void createCollection() {
        try {
            HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                    .withCollectionName("pdf_embeddings")
                    .build();

            boolean collectionExists = milvusClient.hasCollection(hasCollectionParam).getData();

            if (!collectionExists) {
                FieldType vectorField = FieldType.newBuilder()
                        .withName("vector")
                        .withDataType(DataType.FloatVector)
                        .withDimension(128)
                        .build();

                CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName("pdf_embeddings")
                        .withDescription("Embeddings from PDF documents")
                        .addFieldType(vectorField)
                        .build();

                milvusClient.createCollection(createCollectionParam);
                log.info("Collection 'pdf_embeddings' created successfully.");
            } else {
                log.info("Collection 'pdf_embeddings' already exists.");
            }
        } catch (Exception e) {
            log.error("Error while creating collection in Milvus: ", e);
        }
    }

    @Override
    public void storeEmbeddings(List<Float> embeddings) {
        log.info("Storing embeddings into vector database.");
        List<List<Float>> vectors = Collections.singletonList(embeddings);

        InsertParam.Field field = new InsertParam.Field("vector", vectors);

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName("pdf_embeddings")
                .withFields(Collections.singletonList(field))
                .build();

        MutationResult result = milvusClient.insert(insertParam).getData();
        log.info("Inserted embeddings into Milvus with status: " + result.getStatus());
    }

    @Override
    public String queryEmbeddings(String question) {
        log.info("Querying vector database with question: " + question);

        List<Float> questionEmbedding = chatServiceImpl.convertTextToEmbeddings(question);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName("pdf_embeddings")
                .withMetricType(MetricType.L2)
                .withTopK(5)
                .withVectors(Collections.singletonList(questionEmbedding))
                .build();

        var searchResults = milvusClient.search(searchParam);

        if (searchResults == null || searchResults.getData() == null) {
            log.info("No relevant information found.");
            return "No relevant information found.";
        }

        SearchResultData resultData = searchResults.getData().getResults();
        if (resultData.getFieldsDataCount() == 0) {
            log.info("No relevant information found.");
            return "No relevant information found.";
        }
        return "Relevant information found in Milvus.";
    }

    @PreDestroy
    public void closeClient() {
        if (milvusClient != null) {
            milvusClient.close();
            log.info("Milvus client closed successfully.");
        }
    }
}
