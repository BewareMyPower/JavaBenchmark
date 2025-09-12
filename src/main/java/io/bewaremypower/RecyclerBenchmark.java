package io.bewaremypower;

import java.util.concurrent.TimeUnit;
import org.apache.pulsar.shade.io.netty.util.Recycler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

record Tuple(int i, String s, double d) {}

class RecycleTuple {

  private static final Recycler<RecycleTuple> RECYCLER =
      new Recycler<>() {
        @Override
        protected RecycleTuple newObject(Recycler.Handle<RecycleTuple> handle) {
          return new RecycleTuple(handle);
        }
      };

  private final Recycler.Handle<RecycleTuple> handle;
  private int i;
  private String s;
  private double d;

  static RecycleTuple get(int i, String s, double d) {
    final var tuple = RECYCLER.get();
    tuple.i = i;
    tuple.s = s;
    tuple.d = d;
    return tuple;
  }

  private RecycleTuple(Recycler.Handle<RecycleTuple> handle) {
    this.handle = handle;
  }

  void recycle() {
    i = 0;
    s = null;
    d = 0.0;
  }
}

public class RecyclerBenchmark {

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testRecycler() {
    final var tuple = RecycleTuple.get(1, "hello", 3.14);
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
