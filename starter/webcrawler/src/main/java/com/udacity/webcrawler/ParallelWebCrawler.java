package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final List<Pattern> ignoredUrls;
  private final int maxDepth;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @MaxDepth int maxDepth,
          @TargetParallelism int threadCount,
          @PopularWordCount int popularWordCount,
          @IgnoredUrls List<Pattern> ignoredUrls,
          PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.maxDepth = maxDepth;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.ignoredUrls = ignoredUrls;
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    final Instant deadline = clock.instant().plus(timeout);
    final ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
    final ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

    for (String urlLinks : startingUrls) {
      pool.invoke(new taskParallelWebCrawler(urlLinks, deadline, maxDepth, wordCounts, visitedUrls));
    }

    if (wordCounts.isEmpty()) {
      return new CrawlResult
              .Builder()
              .setWordCounts(wordCounts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult
            .Builder()
            .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }


  //  to test -   mvn test -Dtest=WebCrawlerTest,ParallelWebCrawlerTest

  public class taskParallelWebCrawler extends RecursiveTask<Boolean> {


    //    variable declaration
    private String url;
    private Instant deadline;
    private int maxDepth;
    private ConcurrentMap<String, Integer> counts;
    private ConcurrentSkipListSet<String> visitedUrls;



    //    variable constructors declaration
    public taskParallelWebCrawler(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls) {
      this.url = url;
      this.deadline = deadline;
      this.maxDepth = maxDepth;
      this.counts = counts;
      this.visitedUrls = visitedUrls;
    }

    @Override
//    reference - sequentialwebcrawler
//    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
//      return;
//    }
    protected Boolean compute() {
      if (clock.instant().isAfter(deadline) || maxDepth == 0) {
        return false;
      }


//     reference - sequentialwebcrawler
//      for (Pattern pattern : ignoredUrls) {
//        if (pattern.matcher(url).matches()) {
//          return;
//        }
//      }
      for (Pattern pattern : ignoredUrls) {
        if (pattern.matcher(url).matches()) {
          return false;
        }
      }
//      reference - sequentialwebcrawler
//      if (visitedUrls.contains(url)) {
//        return;
//      }
//      visitedUrls.add(url);
      if(!visitedUrls.add(url)) {
        return false;
      }
//      legacy code downloads and parses web pages reference - udacity material
      PageParser.Result result = parserFactory.get(url).parse();

//     reference - sequentialwebcrawler
//      for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
//        if (counts.containsKey(e.getKey())) {
//          counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
//        } else {
//          counts.put(e.getKey(), e.getValue());
//        }
//      }
      for (ConcurrentMap.Entry<String, Integer> entries : result.getWordCounts().entrySet()) {
        counts.compute(entries.getKey(), (key, val) -> (val == null) ? entries.getValue() : entries.getValue() + val);
      }

      List<taskParallelWebCrawler> subTasks = new ArrayList<>();
      for (String urlLink : result.getLinks()) {
        subTasks.add(new taskParallelWebCrawler(urlLink, deadline, maxDepth -1, counts, visitedUrls));
      }
//      the subclass will create sub tasks and run them using invokeall
      invokeAll(subTasks);
      return true;

    }
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

}