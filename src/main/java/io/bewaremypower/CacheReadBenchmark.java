package io.bewaremypower;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class CacheReadBenchmark {

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    public final List<String> input =
        Arrays.asList("test-0", "public/default/test-1", "persistent://public/default/test-2");
    private final LoadingCache<String, String> guavaCache =
        CacheBuilder.newBuilder()
            .maximumSize(100000L)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build(
                new CacheLoader<>() {
                  @Override
                  public String load(String key) {
                    return key + "-value";
                  }
                });
    private final com.github.benmanes.caffeine.cache.LoadingCache<String, String> caffeineCache =
        Caffeine.newBuilder()
            .maximumSize(100000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build(key -> key + "-value");
    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
  }

  @Setup(Level.Trial)
  public void setup(BenchmarkState state) throws Exception {
    for (int i = 0; i < 10000; i++) {
      state.guavaCache.put("test-" + i, "test-" + i);
      state.caffeineCache.put("test-" + i, "test-" + 1);
      state.map.put("test-" + i, "test-" + i);
    }
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testCaffine(BenchmarkState state) {
    state.input.forEach(state.caffeineCache::get);
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testGuava(BenchmarkState state) {
    state.input.forEach(
        x -> {
          try {
            state.guavaCache.get(x);
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testMapGet(BenchmarkState state) {
    state.input.forEach(state.map::get);
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testMapComputeIfAbsent(BenchmarkState state) {
    state.input.forEach(x -> state.map.computeIfAbsent(x, __ -> __ + "-value"));
  }

  public static void main(String[] args) throws RunnerException {
    final var opt =
        new OptionsBuilder().include(CacheReadBenchmark.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }
}
