package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.AiAnalysisProperties;
import com.yuki.sevendays_states.web.WatchpointAiObservationService.AnalysisRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import tools.jackson.databind.ObjectMapper;

/** Invokes Bedrock Converse and validates its structured WATCHPOINT response. */
@Service
@RequiredArgsConstructor
public class BedrockWatchpointClient {

  private final BedrockRuntimeClient bedrockRuntimeClient;
  private final AiAnalysisProperties properties;
  private final ObjectMapper objectMapper;

  public GeneratedPost generate(AnalysisRequest analysisRequest) {
    String requestJson = serialize(analysisRequest);
    ConverseResponse response = bedrockRuntimeClient.converse(ConverseRequest.builder()
        .modelId(properties.modelId())
        .system(SystemContentBlock.fromText(analysisRequest.systemPrompt()))
        .messages(Message.builder()
            .role(ConversationRole.USER)
            .content(ContentBlock.fromText(requestJson))
            .build())
        .inferenceConfig(InferenceConfiguration.builder()
            .maxTokens(properties.maxOutputTokens())
            .temperature(0.6f)
            .build())
        .build());

    String responseText = response.output().message().content().stream()
        .filter(block -> block.text() != null)
        .map(ContentBlock::text)
        .reduce("", String::concat)
        .strip();
    GeneratedPost generated = deserialize(responseText);
    validate(generated, analysisRequest);
    return new GeneratedPost(generated.body().strip(), List.copyOf(generated.evidenceKeys()));
  }

  private String serialize(AnalysisRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (Exception exception) {
      throw new BedrockGenerationException("AI観測データをJSONへ変換できませんでした。", exception);
    }
  }

  private GeneratedPost deserialize(String responseText) {
    if (responseText.isBlank()) {
      throw new BedrockGenerationException("Bedrockから空の応答が返されました。");
    }
    try {
      return objectMapper.readValue(responseText, GeneratedPost.class);
    } catch (Exception exception) {
      throw new BedrockGenerationException("Bedrockの応答が要求したJSON形式ではありません。", exception);
    }
  }

  private void validate(GeneratedPost generated, AnalysisRequest request) {
    if (generated == null || generated.body() == null || generated.body().isBlank()) {
      throw new BedrockGenerationException("Bedrockの投稿本文が空です。");
    }
    if (generated.body().length() > request.outputContract().maxBodyCharacters()) {
      throw new BedrockGenerationException("Bedrockの投稿本文が文字数上限を超えています。");
    }
    if (generated.body().contains("```") || generated.body().matches("(?s).*^#{1,6}\\s.*")) {
      throw new BedrockGenerationException("Bedrockの投稿本文に許可されていないMarkdownがあります。");
    }
    if (generated.evidenceKeys() == null || generated.evidenceKeys().isEmpty()) {
      throw new BedrockGenerationException("Bedrockの応答に根拠キーがありません。");
    }
    Set<String> allowedKeys = new HashSet<>();
    allowedKeys.addAll(List.of("current-totals", "comparison-totals", "world-context"));
    request.observation().survivors().forEach(item -> allowedKeys.add(item.evidenceKey()));
    request.observation().events().forEach(item -> allowedKeys.add(item.evidenceKey()));
    if (!allowedKeys.containsAll(generated.evidenceKeys())) {
      throw new BedrockGenerationException("Bedrockの応答に存在しない根拠キーが含まれています。");
    }
  }

  public record GeneratedPost(String body, List<String> evidenceKeys) {
  }

  public static class BedrockGenerationException extends RuntimeException {

    public BedrockGenerationException(String message) {
      super(message);
    }

    public BedrockGenerationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
