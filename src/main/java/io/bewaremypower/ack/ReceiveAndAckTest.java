package io.bewaremypower.ack;

import java.util.BitSet;
import java.util.function.Function;
import org.apache.pulsar.common.util.collections.ConcurrentBitSetRecyclable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class ReceiveAndAckTest {

  private static final int BATCH_SIZE = 100;
  private static final int NUM_ENTRIES = 100;

  private static <T extends BitSetInterface> void run(Function<BitSet, T> bitSetFactory)
      throws InterruptedException {
    final var consumer = new Consumer<>(new AckTracker<T>(bitSetFactory), BATCH_SIZE, NUM_ENTRIES);
    consumer.messageReceived();
    for (int i = 0; i < BATCH_SIZE * NUM_ENTRIES; i++) {
      final var msgId = consumer.receive();
      consumer.acknowledge(msgId);
    }
    consumer.flush();
  }

  @Warmup(iterations = 5, time = 1)
  @Measurement(iterations = 10, time = 1)
  @Benchmark
  public void testConcurrentBitSetRecyclable() throws InterruptedException {
    run(bitSet -> new RecyclableBitSet(ConcurrentBitSetRecyclable.create(bitSet)));
  }

  @Warmup(iterations = 5, time = 1)
  @Measurement(iterations = 10, time = 2)
  @Benchmark
  public void testStampedLockBitSet() throws InterruptedException {
    run(StampedLockBitSet::new);
  }

  @Warmup(iterations = 5, time = 1)
  @Measurement(iterations = 10, time = 2)
  @Benchmark
  public void testSynchronizedBitSet() throws InterruptedException {
    run(SynchronizedBitSet::new);
  }

  public static void main(String[] args) throws RunnerException {
    final var opt =
        new OptionsBuilder().include(ReceiveAndAckTest.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }
}
