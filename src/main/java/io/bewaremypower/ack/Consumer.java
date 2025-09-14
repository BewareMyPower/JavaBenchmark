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
