package io.bewaremypower.recycler;

public abstract class ThreadLocalSingletonRecycler<T> {

  private final ThreadLocal<T> localObject = ThreadLocal.withInitial(this::newObject);

  protected abstract T newObject();

  public T get() {
    return localObject.get();
  }

  public void recycle() {
    // We don't need to manually recycle this object, the caller side should be responsible to reset
    // all fields
  }
}
