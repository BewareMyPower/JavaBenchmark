package io.bewaremypower.recycler;

import lombok.Getter;

@Getter
public class ThreadLocalSingletonTuple {

  private static final ThreadLocalSingletonRecycler<ThreadLocalSingletonTuple> RECYCLER =
      new ThreadLocalSingletonRecycler<>() {
        @Override
        protected ThreadLocalSingletonTuple newObject() {
          return new ThreadLocalSingletonTuple();
        }
      };

  private int i;
  private String s;
  private double d;

  public static ThreadLocalSingletonTuple get(int i, String s, double d) {
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
  }
}
