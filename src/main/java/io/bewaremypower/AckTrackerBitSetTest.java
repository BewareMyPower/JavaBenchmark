package io.bewaremypower;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.pulsar.client.api.MessageIdAdv;
import org.apache.pulsar.client.impl.BatchMessageIdImpl;
import org.apache.pulsar.client.impl.MessageIdImpl;
import org.apache.pulsar.client.util.ExecutorProvider;
import org.apache.pulsar.common.util.collections.ConcurrentBitSetRecyclable;
import org.apache.pulsar.common.util.collections.GrowableArrayBlockingQueue;
import org.apache.pulsar.common.util.netty.EventLoopUtil;
import org.apache.pulsar.shade.io.netty.channel.EventLoopGroup;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class AckTrackerBitSetTest {

  static final int BATCH_SIZE = 500;

  interface BitSetInterface {
    boolean get(int i);

    void clear(int i);

    long[] toLongArray();

    default void recycle() {}
  }

  static class AckTracker<T extends BitSetInterface> {

    final ConcurrentSkipListMap<MessageIdAdv, T> pendingIndividualBatchIndexAcks =
        new ConcurrentSkipListMap<>();
    final Function<BitSet, T> factory;

    AckTracker(Function<BitSet, T> factory) {
      this.factory = factory;
    }

    boolean isDuplicated(MessageIdAdv msgId) {
      final var bitSet = pendingIndividualBatchIndexAcks.get(msgId);
      return bitSet != null && !bitSet.get(msgId.getBatchIndex());
    }

    void acknowledge(MessageIdAdv msgId) {
      final var bitSet =
          pendingIndividualBatchIndexAcks.computeIfAbsent(
              new MessageIdImpl(msgId.getLedgerId(), msgId.getEntryId(), msgId.getPartitionIndex()),
              __ -> {
                final var ackSet = msgId.getAckSet();
                synchronized (ackSet) {
                  return factory.apply(ackSet);
                }
              });
      bitSet.clear(msgId.getBatchIndex());
    }

    void flush() {
      final var entriesToAck = new ArrayList<Triple<Long, Long, T>>();
      while (true) {
        final var entry = pendingIndividualBatchIndexAcks.pollFirstEntry();
        if (entry == null) {
          break;
        }
        final var msgId = entry.getKey();
        final var bitSet = entry.getValue();
        entriesToAck.add(Triple.of(msgId.getLedgerId(), msgId.getEntryId(), bitSet));
      }
      for (final var entry : entriesToAck) {
        final var bitSet = entry.getRight();
        bitSet.toLongArray();
        bitSet.recycle();
      }
    }
  }

  static class Consumer<T extends BitSetInterface> {
    private static final EventLoopGroup EVENT_LOOP =
        EventLoopUtil.newEventLoopGroup(
            1,
            false,
            new ExecutorProvider.ExtendedThreadFactory(
                "pulsar-client-io-" + System.currentTimeMillis(), true));
    private final GrowableArrayBlockingQueue<MessageIdAdv> messages =
        new GrowableArrayBlockingQueue<>(1000);
    private final AckTracker<T> tracker;

    Consumer(AckTracker<T> tracker) {
      this.tracker = tracker;
    }

    void run() throws InterruptedException {
      EVENT_LOOP.execute(
          () -> {
            final var bitSet = new BitSet(BATCH_SIZE);
            bitSet.set(0, BATCH_SIZE);
            for (int i = 0; i < BATCH_SIZE; i++) {
              final var msgId = new BatchMessageIdImpl(0L, 0L, -1, i, BATCH_SIZE, bitSet);
              if (!tracker.isDuplicated(msgId)) {
                messages.add(msgId);
              }
            }
          });
      for (int i = 0; i < BATCH_SIZE; i++) {
        final var msgId = messages.take();
        tracker.acknowledge(msgId);
      }
      tracker.flush();
    }
  }

  static class RecyclableBitSet implements BitSetInterface {

    final ConcurrentBitSetRecyclable bitSetRecyclable;

    RecyclableBitSet(ConcurrentBitSetRecyclable bitSetRecyclable) {
      this.bitSetRecyclable = bitSetRecyclable;
    }

    @Override
    public boolean get(int i) {
      return bitSetRecyclable.get(i);
    }

    @Override
    public void clear(int i) {
      bitSetRecyclable.clear(i);
    }

    @Override
    public long[] toLongArray() {
      return bitSetRecyclable.toLongArray();
    }

    @Override
    public void recycle() {
      bitSetRecyclable.recycle();
    }
  }

  static class SynchronizedBitSet implements BitSetInterface {

    final BitSet bitSet;

    SynchronizedBitSet(BitSet bitSet) {
      this.bitSet = bitSet;
    }

    @Override
    public boolean get(int i) {
      synchronized (bitSet) {
        return bitSet.get(i);
      }
    }

    @Override
    public void clear(int i) {
      synchronized (bitSet) {
        bitSet.clear(i);
      }
    }

    @Override
    public long[] toLongArray() {
      synchronized (bitSet) {
        return bitSet.toLongArray();
      }
    }
  }

  @Warmup(iterations = 5, time = 1)
  @Measurement(iterations = 10, time = 1)
  @Benchmark
  public void testConcurrentBitSetRecyclable() throws InterruptedException {
    final var tracker =
        new AckTracker<>(bitSet -> new RecyclableBitSet(ConcurrentBitSetRecyclable.create(bitSet)));
    new Consumer<>(tracker).run();
  }

  @Warmup(iterations = 5, time = 1)
  @Measurement(iterations = 10, time = 1)
  @Benchmark
  public void testSynchronizedBitSet() throws InterruptedException {
    final var tracker = new AckTracker<>(bitSet -> new SynchronizedBitSet((BitSet) bitSet.clone()));
    new Consumer<>(tracker).run();
  }

  public static void main(String[] args) throws RunnerException, InterruptedException {
    final var opt =
        new OptionsBuilder().include(AckTrackerBitSetTest.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }
}
