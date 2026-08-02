package com.yuki.sevendays_states.web;

import java.nio.file.Files;
import java.nio.file.Path;
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

    String viewName = controller.index(model);

    assertThat(viewName).isEqualTo("dashboard");
    assertThat(model).containsKey("dashboard");
  }

  @Test
  void detailRoutesReturnTheirViewsAndModels() {
    ConcurrentModel serverModel = new ConcurrentModel();
    ConcurrentModel killModel = new ConcurrentModel();
    ConcurrentModel vehicleModel = new ConcurrentModel();
    ConcurrentModel explorationModel = new ConcurrentModel();
    ConcurrentModel aiCommentModel = new ConcurrentModel();

    assertThat(controller.server(serverModel)).isEqualTo("server-detail");
    assertThat(controller.kills(killModel)).isEqualTo("kill-detail");
    assertThat(controller.vehicles(vehicleModel)).isEqualTo("vehicle-detail");
    assertThat(controller.exploration(explorationModel)).isEqualTo("exploration-detail");
    assertThat(controller.aiComments(aiCommentModel)).isEqualTo("ai-comments");
    assertThat(serverModel).containsKey("server");
    assertThat(killModel).containsKey("kills");
    assertThat(vehicleModel).containsKey("vehicles");
    assertThat(explorationModel).containsKey("exploration");
    assertThat(aiCommentModel).containsKeys("comments", "editorEnabled");
  }

  @Test
  void dashboardTemplateDoesNotRenderSensitiveIdentifiers() throws Exception {
    String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"))
        + Files.readString(Path.of("src/main/resources/templates/server-detail.html"))
        + Files.readString(Path.of("src/main/resources/templates/kill-detail.html"))
        + Files.readString(Path.of("src/main/resources/templates/vehicle-detail.html"));
    template += Files.readString(Path.of("src/main/resources/templates/exploration-detail.html"));
    template += Files.readString(Path.of("src/main/resources/templates/ai-comments.html"));

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
}
