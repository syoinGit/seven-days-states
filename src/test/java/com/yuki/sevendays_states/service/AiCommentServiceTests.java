package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.repository.T_AiCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states_ai_comments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "app.sevendays.import.startup-enabled=false",
    "app.ai-comment.editor-key=test-editor-key"
})
class AiCommentServiceTests {

  @Autowired
  private AiCommentService service;

  @Autowired
  private T_AiCommentRepository repository;

  @BeforeEach
  void resetData() {
    repository.deleteAll();
  }

  @Test
  void publishesAndReturnsLatestDailyDiary() {
    service.publish(LocalDate.of(2026, 8, 1), " 荒野通信 ", " 今日も生存を確認。 ", "test-editor-key");
    service.publish(LocalDate.of(2026, 8, 2), "二報", "病院の探索が進みました。", "test-editor-key");

    assertThat(service.latestDiary()).get()
        .satisfies(comment -> {
          assertThat(comment.title()).isEqualTo("二報");
          assertThat(comment.body()).isEqualTo("病院の探索が進みました。");
          assertThat(comment.sourceType()).isEqualTo("MANUAL_BETA");
        });
    assertThat(service.diaries()).hasSize(2);
  }

  @Test
  void rejectsBlankComment() {
    assertThatThrownBy(() -> service.publish(
        LocalDate.of(2026, 8, 2), "", "", "test-editor-key"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void updatesExistingDiaryForSameDate() {
    LocalDate date = LocalDate.of(2026, 8, 2);
    service.publish(date, "初稿", "最初の日記", "test-editor-key");
    service.publish(date, "完成稿", "完成した日記", "test-editor-key");

    assertThat(repository.count()).isOne();
    assertThat(service.findByDiaryDate(date)).get()
        .satisfies(diary -> assertThat(diary.title()).isEqualTo("完成稿"));
  }

  @Test
  void publishesGeneratedObservationWithoutReplacingDailyDiary() {
    service.publish(LocalDate.of(2026, 8, 2), "日記", "一日の記録", "test-editor-key");
    service.publishGenerated("WATCHPOINT観測記録", "静かな探索が続いています。", "AWS_BEDROCK");

    assertThat(repository.count()).isEqualTo(2);
    assertThat(service.latestComment()).get().satisfies(comment -> {
      assertThat(comment.diaryDate()).isNull();
      assertThat(comment.body()).isEqualTo("静かな探索が続いています。");
      assertThat(comment.sourceType()).isEqualTo("AWS_BEDROCK");
    });
    assertThat(service.latestDiary()).get()
        .satisfies(comment -> assertThat(comment.title()).isEqualTo("日記"));
  }
}
