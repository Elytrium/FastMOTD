/*
 * Copyright (C) 2022 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.fastmotd.utils;

import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class ByteBufCopyThreadLocal extends ThreadLocal<ByteBuf> {

  private final List<ByteBuf> byteBuffers = new LinkedList<>();
  private final ListIterator<ByteBuf> byteBufferIterator;

  public ByteBufCopyThreadLocal(ByteBuf from) {
    super();

    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
      this.byteBuffers.add(from.copy());
    }

    this.byteBufferIterator = this.byteBuffers.listIterator();
  }

  protected ByteBuf initialValue() {
    return this.byteBufferIterator.next();
  }

  public void release() {
    for (ByteBuf byteBuffer : this.byteBuffers) {
      if (byteBuffer.refCnt() != 0) {
        byteBuffer.release();
      }
    }
  }
}
