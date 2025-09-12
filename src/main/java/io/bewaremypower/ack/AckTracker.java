package io.bewaremypower.ack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.pulsar.client.api.MessageIdAdv;
import org.apache.pulsar.client.impl.MessageIdImpl;

public class AckTracker<T extends BitSetInterface> {

  final ConcurrentSkipListMap<MessageIdAdv, T> pendingIndividualBatchIndexAcks =
      new ConcurrentSkipListMap<>();
  final Function<BitSet, T> factory;

  public AckTracker(Function<BitSet, T> factory) {
    this.factory = factory;
  }

  public boolean isDuplicated(MessageIdAdv msgId) {
    final var key =
        new MessageIdImpl(msgId.getLedgerId(), msgId.getEntryId(), msgId.getPartitionIndex());
    final var bitSet = pendingIndividualBatchIndexAcks.get(key);
    return bitSet != null && !bitSet.get(msgId.getBatchIndex());
  }

  public void acknowledge(MessageIdAdv msgId) {
    final var key =
        new MessageIdImpl(msgId.getLedgerId(), msgId.getEntryId(), msgId.getPartitionIndex());
    final var bitSet =
        pendingIndividualBatchIndexAcks.computeIfAbsent(
            key,
            __ -> {
              final var ackSet = msgId.getAckSet();
              synchronized (ackSet) {
                return factory.apply(ackSet);
              }
            });
    bitSet.clear(msgId.getBatchIndex());
  }

  public void flush() {
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
