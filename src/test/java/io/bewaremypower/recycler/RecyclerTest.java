package io.bewaremypower.recycler;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class RecyclerTest {

  @Test
  public void testThreadLocalSingletonTuple() {
    var tuple = ThreadLocalSingletonTuple.get(1, "hello", 2.3);
    Assert.assertEquals(tuple.getI(), 1);
    Assert.assertEquals(tuple.getS(), "hello");
    Assert.assertEquals(tuple.getD(), 2.3);
    tuple.recycle();

    tuple = ThreadLocalSingletonTuple.get(4, "world", 5.6);
    Assert.assertEquals(tuple.getI(), 4);
    Assert.assertEquals(tuple.getS(), "world");
    Assert.assertEquals(tuple.getD(), 5.6);

    // tuple is not recycled, so tuple2 will be the same with tuple
    final var tuple2 = ThreadLocalSingletonTuple.get(7, "xxx", 8.9);
    Assert.assertEquals(tuple.getI(), 7);
    Assert.assertEquals(tuple.getS(), "xxx");
    Assert.assertEquals(tuple.getD(), 8.9);
    Assert.assertSame(tuple, tuple2);
  }
}
