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
