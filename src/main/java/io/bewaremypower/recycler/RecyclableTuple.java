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
