package com.yuki.sevendays_states.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties(prefix = "app.sevendays")
public record SevenDaysDataProperties(
    String environmentName,
    String mode,
    Path root,
    Path configDir,
    Path dataDir,
    Path gameDir,
    @Name("import") Import importSettings,
    Log log,
    Docker docker,
    Telnet telnet,
    Transaction transaction,
    Poi poi
) {

  public SevenDaysDataProperties {
    environmentName = environmentName == null || environmentName.isBlank() ? "local" : environmentName;
    mode = mode == null || mode.isBlank() ? "file" : mode;
    root = root == null ? Path.of("7dtd") : root;
    configDir = blankToNull(configDir);
    dataDir = blankToNull(dataDir);
    gameDir = blankToNull(gameDir);
    importSettings = importSettings == null ? new Import(true, false, 600000L) : importSettings;
    log = log == null ? new Log(null, false, 600000L, 600000L, 1L) : log;
    docker = docker == null ? new Docker("7dtd", "5m", true, 5000L, 5L) : docker;
    telnet = telnet == null ? new Telnet("localhost", 8081, "", false, 30000L, 5000) : telnet;
    transaction = transaction == null ? new Transaction(120L) : transaction;
    poi = poi == null ? new Poi("classpath:poi-translations.csv") : poi;
  }

  public Path configPath() {
    return configDir == null ? root.resolve("config") : configDir;
  }

  public Path dataPath() {
    return dataDir == null ? root.resolve("data") : dataDir;
  }

  public Path gamePath() {
    return gameDir == null ? root.resolve("game") : gameDir;
  }

  public Path serverConfigPath() {
    return configPath().resolve("serverconfig.xml");
  }

  public Path generatedWorldsPath() {
    return dataPath().resolve("GeneratedWorlds");
  }

  public Path savesPath() {
    return dataPath().resolve("Saves");
  }

  public Path gameConfigPath() {
    return gamePath().resolve("Data").resolve("Config");
  }

  public Path gamePrefabsPath() {
    return gamePath().resolve("Data").resolve("Prefabs");
  }

  public Path logPath() {
    return log.dir() == null ? root.resolve("log") : log.dir();
  }

  private static Path blankToNull(Path path) {
    return path == null || path.toString().isBlank() ? null : path;
  }

  public record Import(
      boolean startupEnabled,
      boolean scheduledEnabled,
      long fixedDelayMs
  ) {
  }

  public record Log(
      Path dir,
      boolean scheduledEnabled,
      long fixedDelayMs,
      long initialDelayMs,
      long serverMetricIntervalMinutes
  ) {

    public Log {
      dir = blankToNull(dir);
      fixedDelayMs = fixedDelayMs <= 0 ? 600000L : fixedDelayMs;
      initialDelayMs = initialDelayMs < 0 ? 600000L : initialDelayMs;
      serverMetricIntervalMinutes = serverMetricIntervalMinutes <= 0 ? 1L : serverMetricIntervalMinutes;
    }
  }

  public record Docker(
      String containerName,
      String logSince,
      boolean enabled,
      long reconnectDelayMs,
      long reconnectDelaySeconds
  ) {

    public Docker {
      containerName = containerName == null || containerName.isBlank() ? "7dtd" : containerName;
      logSince = logSince == null || logSince.isBlank() ? "5m" : logSince;
      reconnectDelayMs = reconnectDelayMs <= 0 ? 5000L : reconnectDelayMs;
      reconnectDelaySeconds = reconnectDelaySeconds <= 0 ? 5L : reconnectDelaySeconds;
    }

    public long effectiveReconnectDelayMs() {
      return reconnectDelaySeconds * 1000L;
    }
  }

  public record Telnet(
      String host,
      int port,
      String password,
      boolean scheduledEnabled,
      long fixedDelayMs,
      int readTimeoutMs
  ) {

    public Telnet {
      host = host == null || host.isBlank() ? "localhost" : host;
      port = port <= 0 ? 8081 : port;
      password = password == null ? "" : password;
      fixedDelayMs = fixedDelayMs <= 0 ? 30000L : fixedDelayMs;
      readTimeoutMs = readTimeoutMs <= 0 ? 5000 : readTimeoutMs;
    }
  }

  public record Transaction(
      long currentStateMaxAgeSeconds
  ) {

    public Transaction {
      currentStateMaxAgeSeconds = currentStateMaxAgeSeconds <= 0 ? 120L : currentStateMaxAgeSeconds;
    }
  }

  public record Poi(
      String translationResource
  ) {

    public Poi {
      translationResource = translationResource == null || translationResource.isBlank()
          ? "classpath:poi-translations.csv"
          : translationResource;
    }
  }
}
