package io.bewaremypower.recycler;

public class SingleThreadRecyclableTuple {

  private static final ThreadLocalRecycler<SingleThreadRecyclableTuple> RECYCLER =
      new ThreadLocalRecycler<SingleThreadRecyclableTuple>() {
        @Override
        protected SingleThreadRecyclableTuple newObject() {
          return new SingleThreadRecyclableTuple();
        }
      };

  private int i;
  private String s;
  private double d;

  public static SingleThreadRecyclableTuple get(int i, String s, double d) {
    final var tuple = RECYCLER.get();
    tuple.i = i;
    tuple.s = s;
    tuple.d = d;
    return tuple;
  }

  public void recycle() {
    RECYCLER.recycle(this);
  }
}
