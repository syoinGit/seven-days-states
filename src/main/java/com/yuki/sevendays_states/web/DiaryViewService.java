package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.AiCommentService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiaryViewService {

  private static final int ARCHIVE_EXCERPT_LENGTH = 180;

  private final AiCommentService aiCommentService;
  private final DisplayTimeFormatter displayTimeFormatter = new DisplayTimeFormatter();

  public List<DiaryCard> archive() {
    return aiCommentService.diaries().stream()
        .map(entry -> new DiaryCard(
            entry.diaryDate(), entry.title(), excerpt(entry.body(), ARCHIVE_EXCERPT_LENGTH),
            displayTimeFormatter.format(entry.publishedAt())))
        .toList();
  }

  public Optional<DiaryDetail> detail(LocalDate date) {
    return aiCommentService.findByDiaryDate(date)
        .map(entry -> new DiaryDetail(
            entry.diaryDate(), entry.title(), entry.body(),
            displayTimeFormatter.format(entry.publishedAt())));
  }

  static String excerpt(String body, int maxLength) {
    String normalized = body == null ? "" : body.replaceAll("\\s+", " ").strip();
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, maxLength).stripTrailing() + "…";
  }

  public record DiaryCard(LocalDate date, String title, String excerpt, String publishedAt) {
  }

  public record DiaryDetail(LocalDate date, String title, String body, String publishedAt) {
  }
}
