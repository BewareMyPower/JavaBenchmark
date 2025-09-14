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
package io.bewaremypower;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

class Sender {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private synchronized int internalAsyncSend(int x) {
    return x;
  }

  void asyncSend(int x) {
    executor.execute(() -> internalAsyncSend(x));
  }

  public void shutdown() {
    executor.shutdown();
  }
}

@State(Scope.Benchmark)
public class ReflectionBenchmark {

  @State(Scope.Benchmark)
  public static class Input {
    private final Sender sender = new Sender();
    Method method;
  }

  @Setup(Level.Trial)
  public void setup(Input input) throws Exception {
    // input.method = Sender.class.getDeclaredMethod("internalAsyncSend", int.class);
    // input.method.setAccessible(true);
  }

  @TearDown(Level.Trial)
  public void teardown(Input input) throws Exception {
    input.sender.shutdown();
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testReflection(Input input) {
    try {
      input.method = Sender.class.getDeclaredMethod("internalAsyncSend", int.class);
      input.method.setAccessible(true);
      input.method.invoke(input.sender, 100);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
  @Benchmark
  public void testThreadSwitch(Input input) {
    input.sender.asyncSend(100);
  }

  public static void main(String[] args) throws RunnerException {
    final var opt =
        new OptionsBuilder().include(ReflectionBenchmark.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }
}
