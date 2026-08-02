package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.entity.T_AiComment;
import com.yuki.sevendays_states.repository.T_AiCommentRepository;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiCommentService {

  private final T_AiCommentRepository repository;

  @Value("${app.ai-comment.editor-key:}")
  private String editorKey;

  public Optional<AiCommentEntry> latest() {
    return repository.findTopByOrderByPublishedAtDescIdDesc().map(this::toEntry);
  }

  public Optional<AiCommentEntry> latestDiary() {
    return repository.findTopByDiaryDateIsNotNullOrderByDiaryDateDescPublishedAtDesc()
        .map(this::toEntry);
  }

  public List<AiCommentEntry> diaries() {
    return repository.findTop100ByDiaryDateIsNotNullOrderByDiaryDateDescPublishedAtDesc().stream()
        .map(this::toEntry)
        .toList();
  }

  public List<AiCommentEntry> history() {
    return repository.findTop50ByOrderByPublishedAtDescIdDesc().stream().map(this::toEntry).toList();
  }

  public Optional<AiCommentEntry> findByDiaryDate(LocalDate diaryDate) {
    return repository.findByDiaryDate(diaryDate).map(this::toEntry);
  }

  public boolean editorEnabled() {
    return editorKey != null && !editorKey.isBlank();
  }

  @Transactional
  public AiCommentEntry publish(String title, String body, String submittedEditorKey) {
    return publish(null, title, body, submittedEditorKey);
  }

  @Transactional
  public AiCommentEntry publish(
      LocalDate diaryDate, String title, String body, String submittedEditorKey) {
    String normalizedTitle = normalize(title);
    String normalizedBody = normalize(body);
    if (!editorEnabled()) {
      throw new IllegalArgumentException("AIコメント編集機能が設定されていません。");
    }
    if (normalizedTitle.isBlank() || normalizedTitle.length() > 120) {
      throw new IllegalArgumentException("タイトルは1〜120文字で入力してください。");
    }
    if (normalizedBody.isBlank() || normalizedBody.length() > 4000) {
      throw new IllegalArgumentException("本文は1〜4000文字で入力してください。");
    }
    if (!editorKey.equals(submittedEditorKey)) {
      throw new IllegalArgumentException("編集キーが正しくありません。");
    }
    T_AiComment comment = diaryDate == null
        ? new T_AiComment()
        : repository.findByDiaryDate(diaryDate).orElseGet(T_AiComment::new);
    comment.setTitle(normalizedTitle);
    comment.setBody(normalizedBody);
    comment.setDiaryDate(diaryDate);
    comment.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
    comment.setSourceType("MANUAL_BETA");
    return toEntry(repository.save(comment));
  }

  private String normalize(String value) {
    return value == null ? "" : value.strip();
  }

  private AiCommentEntry toEntry(T_AiComment comment) {
    return new AiCommentEntry(
        comment.getId(), comment.getDiaryDate(), comment.getTitle(), comment.getBody(),
        comment.getPublishedAt(), comment.getSourceType());
  }

  public record AiCommentEntry(
      Long id, LocalDate diaryDate, String title, String body, OffsetDateTime publishedAt, String sourceType) {
  }
}
