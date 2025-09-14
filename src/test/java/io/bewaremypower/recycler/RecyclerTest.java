/**
 * Copyright 2025 Yunze Xu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
