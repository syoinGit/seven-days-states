package com.yuki.sevendays_states.web;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventMessageFormatterTests {

  private final EventMessageFormatter formatter = new EventMessageFormatter();

  @Test
  void killMessagesKeepKillerAndVictimWhileUsingVariations() {
    Set<String> messages = new HashSet<>();

    for (int i = 0; i < 20; i++) {
      String message = formatter.format("KILL", "DDD烈火王テムジン", "討伐した", "ゾンビ" + i, "");

      assertThat(message).contains("DDD烈火王テムジン");
      assertThat(message).contains("ゾンビ" + i);
      assertThat(message).contains("！\n");
      messages.add(message.replace("ゾンビ" + i, "ゾンビ"));
    }

    assertThat(messages).hasSizeGreaterThanOrEqualTo(8);
  }

  @Test
  void movementMessageContainsDestinationAndDistance() {
    assertThat(formatter.format("MOVE", "PlayerA", "移動した", "42.5 m ", "軍事基地"))
        .isEqualTo("PlayerAが軍事基地で42.5 m 移動！");
  }

  @Test
  void sleeperSpawnMessagesKeepPlayerAndEnemyWhileUsingVariations() {
    Set<String> messages = new HashSet<>();

    for (int i = 0; i < 20; i++) {
      String message = formatter.format("SLEEPER_SPAWN", "魅惑のこし餡ぼでぃ", "眠っていた敵を起こした", "木こり" + i, "");

      assertThat(message).contains("魅惑のこし餡ぼでぃ");
      assertThat(message).contains("木こり" + i);
      messages.add(message.replace("木こり" + i, "木こり"));
    }

    assertThat(messages).hasSizeGreaterThanOrEqualTo(8);
  }
}
