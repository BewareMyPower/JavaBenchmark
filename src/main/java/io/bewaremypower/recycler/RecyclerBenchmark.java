package io.bewaremypower.recycler;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class RecyclerBenchmark {

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testRecycler() {
    final var tuple = RecyclableTuple.get(1, "hello", 3.14);
    tuple.recycle();
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testRecord(Blackhole blackhole) {
    final var tuple = new Tuple(1, "hello", 3.14);
    blackhole.consume(tuple);
  }

  public static void main(String[] args) throws RunnerException {
    final var opt =
        new OptionsBuilder().include(RecyclerBenchmark.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }
}
