/**
 * Copyright 2025 Yunze Xu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bewaremypower.recycler;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class RecyclerNotRecycledBenchmark {

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testRecyclerWithoutRecycle(Blackhole blackhole) {
    // Don't call recycle(), so that tuple will be allocated from heap memory directly after warming
    // up
    final var tuple = RecyclableTuple.get(1, "hello", 3.14);
    blackhole.consume(tuple);
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
        new OptionsBuilder()
            .include(RecyclerNotRecycledBenchmark.class.getSimpleName())
            .forks(1)
            .build();
    new Runner(opt).run();
  }
}
