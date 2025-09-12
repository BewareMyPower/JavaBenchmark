package io.bewaremypower.ack;

import org.apache.pulsar.common.util.collections.ConcurrentBitSetRecyclable;

public class RecyclableBitSet implements BitSetInterface {

  final ConcurrentBitSetRecyclable bitSetRecyclable;

  public RecyclableBitSet(ConcurrentBitSetRecyclable bitSetRecyclable) {
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
