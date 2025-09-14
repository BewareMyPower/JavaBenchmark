package io.bewaremypower.recycler;

public class ThreadLocalQueueTuple {

  private static final ThreadLocalQueueRecycler<ThreadLocalQueueTuple> RECYCLER =
      new ThreadLocalQueueRecycler<>() {
        @Override
        protected ThreadLocalQueueTuple newObject() {
          return new ThreadLocalQueueTuple();
        }
      };

  private int i;
  private String s;
  private double d;

  public static ThreadLocalQueueTuple get(int i, String s, double d) {
    final var tuple = RECYCLER.get();
    tuple.i = i;
    tuple.s = s;
    tuple.d = d;
    return tuple;
  }

  public void recycle() {
    i = 0;
    s = null;
    d = 0.0;
    RECYCLER.recycle(this);
  }
}
