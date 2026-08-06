package com.yuki.sevendays_states.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockConfig {

  @Bean(destroyMethod = "close")
  BedrockRuntimeClient bedrockRuntimeClient(AiAnalysisProperties properties) {
    return BedrockRuntimeClient.builder()
        .region(Region.of(properties.awsRegion()))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }
}
