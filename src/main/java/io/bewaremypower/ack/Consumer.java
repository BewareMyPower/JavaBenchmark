package io.bewaremypower.ack;

import java.util.BitSet;
import org.apache.pulsar.client.api.MessageIdAdv;
import org.apache.pulsar.client.impl.BatchMessageIdImpl;
import org.apache.pulsar.client.util.ExecutorProvider;
import org.apache.pulsar.common.util.collections.GrowableArrayBlockingQueue;
import org.apache.pulsar.common.util.netty.EventLoopUtil;
import org.apache.pulsar.shade.io.netty.channel.EventLoopGroup;

public class Consumer<T extends BitSetInterface> {
  // Simulate the event loop of PulsarClientImpl
  private static final EventLoopGroup EVENT_LOOP =
      EventLoopUtil.newEventLoopGroup(
          1,
          false,
          new ExecutorProvider.ExtendedThreadFactory(
              "pulsar-client-io-" + System.currentTimeMillis(), true));
  private final GrowableArrayBlockingQueue<MessageIdAdv> incomingMessageIds =
      new GrowableArrayBlockingQueue<>(1000);
  private final AckTracker<T> tracker;
  private final int batchSize;
  private final int numEntries;

  public Consumer(AckTracker<T> tracker, int batchSize, int numEntries) {
    this.tracker = tracker;
    this.batchSize = batchSize;
    this.numEntries = numEntries;
  }

  /**
   * Simulate the dispatching logic of {@link org.apache.pulsar.client.impl.ConsumerImpl} when
   * receiving an entry that represents batched messages in Netty's I/O thread.
   */
  public void messageReceived() {
    EVENT_LOOP.execute(
        () -> {
          for (int entryId = 0; entryId < numEntries; entryId++) {
            final var bitSet = new BitSet(batchSize);
            bitSet.set(0, batchSize);
            for (int i = 0; i < batchSize; i++) {
              final var msgId = new BatchMessageIdImpl(0L, entryId, -1, i, batchSize, bitSet);
              if (!tracker.isDuplicated(msgId)) {
                incomingMessageIds.add(msgId);
              }
            }
          }
        });
  }

  public MessageIdAdv receive() throws InterruptedException {
    return incomingMessageIds.take();
  }

  public void acknowledge(MessageIdAdv msgId) {
    tracker.acknowledge(msgId);
  }

  public void flush() {
    tracker.flush();
  }
}
