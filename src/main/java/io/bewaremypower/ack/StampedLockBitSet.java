package io.bewaremypower.ack;

import java.util.BitSet;
import java.util.concurrent.locks.StampedLock;

/** Similar to {@link org.apache.pulsar.common.util.collections.ConcurrentBitSet}. */
public class StampedLockBitSet implements BitSetInterface {

  private final BitSet bitSet;
  private final StampedLock rwLock = new StampedLock();

  public StampedLockBitSet(BitSet bitSet) {
    this.bitSet = bitSet;
  }

  @Override
  public boolean get(int i) {
    long stamp = this.rwLock.tryOptimisticRead();
    boolean isSet = bitSet.get(i);
    if (!this.rwLock.validate(stamp)) {
      stamp = this.rwLock.readLock();
      try {
        isSet = bitSet.get(i);
      } finally {
        this.rwLock.unlockRead(stamp);
      }
    }
    return isSet;
  }

  @Override
  public void clear(int i) {
    long stamp = this.rwLock.writeLock();

    try {
      bitSet.clear(i);
    } finally {
      this.rwLock.unlockWrite(stamp);
    }
  }

  @Override
  public long[] toLongArray() {
    long stamp = this.rwLock.tryOptimisticRead();
    long[] longArray = bitSet.toLongArray();
    if (!this.rwLock.validate(stamp)) {
      stamp = this.rwLock.readLock();
      try {
        longArray = bitSet.toLongArray();
      } finally {
        this.rwLock.unlockRead(stamp);
      }
    }
    return longArray;
  }
}
