package io.bewaremypower.recycler;

import java.util.ArrayDeque;

public abstract class ThreadLocalRecycler<T> {

  private final ThreadLocal<ArrayDeque<T>> localQueue =
      new ThreadLocal<>() {
        @Override
        protected ArrayDeque<T> initialValue() {
          return new ArrayDeque<>(10);
        }
      };

  protected abstract T newObject();

  public T get() {
    final var object = localQueue.get().pollFirst();
    return object == null ? newObject() : object;
  }

  // NOTE: it must be called in the same thread when `get` is called
  public void recycle(T object) {
    localQueue.get().push(object);
  }
}
