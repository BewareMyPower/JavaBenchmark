package io.bewaremypower.ack;

public interface BitSetInterface {
  boolean get(int i);

  void clear(int i);

  long[] toLongArray();

  default void recycle() {}
}
