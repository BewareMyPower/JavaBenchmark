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
