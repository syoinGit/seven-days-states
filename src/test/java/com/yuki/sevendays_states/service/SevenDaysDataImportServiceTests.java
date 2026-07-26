package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.repository.M_PlayerRepository;
import com.yuki.sevendays_states.repository.T_ImportRunRepository;
import com.yuki.sevendays_states.repository.T_PlayerMarkerSnapshotRepository;
import com.yuki.sevendays_states.repository.T_PlayerStateSnapshotRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states_data_import;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "app.sevendays.import.startup-enabled=false",
    "app.sevendays.root=target/test-7dtd-data-import"
})
class SevenDaysDataImportServiceTests {

  private static final Path GAME_DIR = Path.of("target/test-7dtd-data-import/data/Saves/TestWorld/TestGame");

  @Autowired
  private SevenDaysDataImportService importService;

  @Autowired
  private M_PlayerRepository playerRepository;

  @Autowired
  private T_PlayerStateSnapshotRepository playerStateSnapshotRepository;

  @Autowired
  private T_PlayerMarkerSnapshotRepository playerMarkerSnapshotRepository;

  @Autowired
  private T_ImportRunRepository importRunRepository;

  @BeforeEach
  void resetData() throws Exception {
    playerMarkerSnapshotRepository.deleteAll();
    playerStateSnapshotRepository.deleteAll();
    importRunRepository.deleteAll();
    playerRepository.deleteAll();
    Files.createDirectories(GAME_DIR);
  }

  @Test
  void importingSameEosPlayerDoesNotIncreasePlayerMaster() throws Exception {
    writePlayersXml("""
        <player platform="EOS" userid="00024b5c4d2546468b7c6775bd927c32" nativeplatform="Steam" nativeuserid="76561198382915826" playername="魅惑のこし餡ぼでぃ" playgroup="Standalone" lastlogin="2026-07-25 22:45:07" position="464,47,-602" />
        """);

    importService.importCurrentData();
    importService.importCurrentData();

    assertThat(playerRepository.findAll())
        .extracting(player -> player.getPlayerKey() + ":" + player.getPlayerName())
        .containsExactly("EOS:00024b5c4d2546468b7c6775bd927c32:魅惑のこし餡ぼでぃ");
  }

  @Test
  void steamMatchUpdatesExistingPlayerWhenCanonicalKeyChangesToEos() throws Exception {
    writePlayersXml("""
        <player platform="Steam" userid="76561198382915826" playername="PlayerBefore" playgroup="Standalone" lastlogin="2026-07-25 22:45:07" position="464,47,-602" />
        """);
    importService.importCurrentData();

    writePlayersXml("""
        <player platform="EOS" userid="00024b5c4d2546468b7c6775bd927c32" nativeplatform="Steam" nativeuserid="76561198382915826" playername="PlayerAfter" playgroup="Standalone" lastlogin="2026-07-26 01:45:07" position="500,47,-602" />
        """);
    importService.importCurrentData();

    assertThat(playerRepository.findAll())
        .extracting(player -> player.getPlayerKey() + ":" + player.getPlayerName() + ":" + player.getNativeUserId())
        .containsExactly("EOS:00024b5c4d2546468b7c6775bd927c32:PlayerAfter:76561198382915826");
  }

  @Test
  void sameNameWithDifferentExternalIdsCreatesDifferentPlayers() throws Exception {
    writePlayersXml("""
        <player platform="EOS" userid="eos-a" nativeplatform="Steam" nativeuserid="steam-a" playername="SameName" playgroup="Standalone" lastlogin="2026-07-25 22:45:07" position="1,2,3" />
        <player platform="EOS" userid="eos-b" nativeplatform="Steam" nativeuserid="steam-b" playername="SameName" playgroup="Standalone" lastlogin="2026-07-25 22:45:07" position="4,5,6" />
        """);

    importService.importCurrentData();

    assertThat(playerRepository.findAll())
        .extracting(player -> player.getPlayerKey() + ":" + player.getPlayerName())
        .containsExactlyInAnyOrder("EOS:eos-a:SameName", "EOS:eos-b:SameName");
  }

  private void writePlayersXml(String players) throws Exception {
    Files.writeString(GAME_DIR.resolve("players.xml"), """
        <?xml version="1.0" encoding="UTF-8"?>
        <persistentplayerdata version="1">
        %s
        </persistentplayerdata>
        """.formatted(players));
  }
}
