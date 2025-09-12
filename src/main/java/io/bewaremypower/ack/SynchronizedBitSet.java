package io.bewaremypower.ack;

import java.util.BitSet;

public class SynchronizedBitSet implements BitSetInterface {

  final BitSet bitSet;

  public SynchronizedBitSet(BitSet bitSet) {
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
