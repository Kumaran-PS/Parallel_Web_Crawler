package com.udacity.webcrawler.json;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    // TODO: Fill in this method.
    // Using try-with-resources which implements the autocloseable feature
    try (Reader reader = Files.newBufferedReader(path)) {
      return read(reader);   //to read the JSON string from a file path and pass to read
    } catch (IOException e) {
      System.out.println("Error in crawler configuration-load" +e);
      return null;
    }
  }


// to test -  mvn test -Dtest=ConfigurationLoaderTest



  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader) {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(reader);
    // TODO: Fill in this method
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);  // To avoid "Stream closed" failure in the test

    try {
      return mapper.readValue(reader, CrawlerConfiguration.Builder.class).build();   //read JSON input & parse into CrawlerConfiguration
    } catch (Exception e) {                                                          // suing json library
      System.out.println("Error in crawler configuration-read" +e);
      return null;
    }
  }
}