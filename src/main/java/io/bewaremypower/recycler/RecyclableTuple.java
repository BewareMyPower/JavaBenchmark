package io.bewaremypower.recycler;

import org.apache.pulsar.shade.io.netty.util.Recycler;

public class RecyclableTuple {

  private static final Recycler<RecyclableTuple> RECYCLER =
      new Recycler<>(10) {
        @Override
        protected RecyclableTuple newObject(Handle<RecyclableTuple> handle) {
          return new RecyclableTuple(handle);
        }
      };

  private final Recycler.Handle<RecyclableTuple> handle;
  private int i;
  private String s;
  private double d;

  public static RecyclableTuple get(int i, String s, double d) {
    final var tuple = RECYCLER.get();
    tuple.i = i;
    tuple.s = s;
    tuple.d = d;
    return tuple;
  }

  private RecyclableTuple(Recycler.Handle<RecyclableTuple> handle) {
    this.handle = handle;
  }

  public void recycle() {
    i = 0;
    s = null;
    d = 0.0;
    handle.recycle(this);
  }
}
