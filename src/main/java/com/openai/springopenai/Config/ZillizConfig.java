package com.openai.springopenai.Config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zilliz.cloud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZillizConfig {

    private String uri;
    private String apiKey;

}
