package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.PlayerSocialService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states_web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "app.sevendays.import.startup-enabled=false"
})
class DashboardControllerTests {

  @Autowired
  private DashboardController controller;

  @Test
  void dashboardReturnsViewAndModel() {
    ConcurrentModel model = new ConcurrentModel();

    String viewName = controller.index(model, null);

    assertThat(viewName).isEqualTo("dashboard");
    assertThat(model).containsKeys("dashboard", "timeline");
  }

  @Test
  void combinesPostsAndGameEventsInOneNewestFirstTimeline() {
    DashboardViewService.TravelEntry event = new DashboardViewService.TravelEntry(
        "2026-08-05 19:40:00", "KILL", "combat", "PlayerA", "討伐", "zombie",
        "PlayerAがゾンビを討伐した", "荒野", "10, 20, 30");
    var post = new PlayerSocialService.PostView(
        1L, 10L, "PlayerB", "探索いってきます", "2026-08-05 19:41:00", 2, true, true);

    List<DashboardController.TimelineItem> timeline = DashboardController.timeline(
        List.of(event), List.of(post));

    assertThat(timeline)
        .extracting(DashboardController.TimelineItem::itemType)
        .containsExactly("POST", "EVENT");
  }

  @Test
  void samplesAtMostOneGameEventPerFiveMinuteWindowWithoutDroppingPosts() {
    var firstEvent = travelEntry("2026-08-05 19:40:10", "PlayerA");
    var sameWindowEvent = travelEntry("2026-08-05 19:44:59", "PlayerB");
    var nextWindowEvent = travelEntry("2026-08-05 19:45:00", "PlayerC");
    var post = new PlayerSocialService.PostView(
        1L, 10L, "PlayerD", "投稿", "2026-08-05 19:42:00", 0, false, true);

    List<DashboardController.TimelineItem> timeline = DashboardController.timeline(
        List.of(firstEvent, sameWindowEvent, nextWindowEvent), List.of(post));

    assertThat(timeline).filteredOn(item -> item.itemType().equals("EVENT")).hasSize(2);
    assertThat(timeline).filteredOn(item -> item.itemType().equals("POST")).hasSize(1);
    assertThat(DashboardController.sampledEvents(List.of(firstEvent, sameWindowEvent)))
        .isEqualTo(DashboardController.sampledEvents(List.of(firstEvent, sameWindowEvent)));
  }

  private static DashboardViewService.TravelEntry travelEntry(String occurredAt, String actor) {
    return new DashboardViewService.TravelEntry(
        occurredAt, "KILL", "combat", actor, "討伐", "zombie",
        actor + "がゾンビを討伐した", "荒野", "10, 20, 30");
  }

  @Test
  void oldCommunityRouteRedirectsToUnifiedTimeline() {
    assertThat(controller.community()).isEqualTo("redirect:/dashboard#timeline");
  }

  @Test
  void publicRootShowsLandingPage() {
    assertThat(controller.landing(null)).isEqualTo("landing");
  }

  @Test
  void detailRoutesReturnTheirViewsAndModels() {
    ConcurrentModel serverModel = new ConcurrentModel();
    ConcurrentModel killModel = new ConcurrentModel();
    ConcurrentModel vehicleModel = new ConcurrentModel();
    ConcurrentModel explorationModel = new ConcurrentModel();

    assertThat(controller.server(serverModel)).isEqualTo("server-detail");
    assertThat(controller.kills(killModel)).isEqualTo("kill-detail");
    assertThat(controller.vehicles(vehicleModel)).isEqualTo("vehicle-detail");
    assertThat(controller.exploration(explorationModel)).isEqualTo("exploration-detail");
    assertThat(serverModel).containsKey("server");
    assertThat(killModel).containsKey("kills");
    assertThat(vehicleModel).containsKey("vehicles");
    assertThat(explorationModel).containsKey("exploration");
  }

  @Test
  void dashboardTemplateDoesNotRenderSensitiveIdentifiers() throws Exception {
    String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"))
        + Files.readString(Path.of("src/main/resources/templates/server-detail.html"))
        + Files.readString(Path.of("src/main/resources/templates/kill-detail.html"))
        + Files.readString(Path.of("src/main/resources/templates/vehicle-detail.html"));
    template += Files.readString(Path.of("src/main/resources/templates/exploration-detail.html"));
    template += Files.readString(Path.of("src/main/resources/templates/diary-maintenance.html"));
    template += Files.readString(Path.of("src/main/resources/templates/diary-generation-data.html"));
    template += Files.readString(Path.of("src/main/resources/templates/diary-editor.html"));
    template += Files.readString(Path.of("src/main/resources/templates/diaries.html"));
    template += Files.readString(Path.of("src/main/resources/templates/diary-detail.html"));

    assertThat(template)
        .doesNotContain("Steam_")
        .doesNotContain("EOS_")
        .doesNotContain("platform_id")
        .doesNotContain("cross_platform_id")
        .doesNotContain("native_user_id")
        .doesNotContain("source_log_hash")
        .doesNotContain("source_file")
        .doesNotContain("platformId")
        .doesNotContain("crossPlatformId")
        .doesNotContain("nativeUserId")
        .doesNotContain("sourceLogHash")
        .doesNotContain("sourceFile");
  }

  @Test
  void diaryMaintenanceRoutesReturnTheirViews() {
    LocalDate date = LocalDate.of(2026, 8, 2);
    ConcurrentModel listModel = new ConcurrentModel();
    ConcurrentModel dataModel = new ConcurrentModel();
    ConcurrentModel editorModel = new ConcurrentModel();

    assertThat(controller.diaryMaintenance(listModel)).isEqualTo("diary-maintenance");
    assertThat(controller.diaryGenerationData(date, dataModel)).isEqualTo("diary-generation-data");
    assertThat(controller.diaryEditor(date, editorModel)).isEqualTo("diary-editor");
    assertThat(listModel).containsKey("days");
    assertThat(dataModel).containsKey("packet");
    assertThat(editorModel).containsKeys("packet", "editorEnabled");
  }

  @Test
  void publicDiaryListReturnsDatabaseBackedView() {
    ConcurrentModel model = new ConcurrentModel();

    assertThat(controller.diaries(model)).isEqualTo("diaries");
    assertThat(model).containsKey("diaries");
  }
}
