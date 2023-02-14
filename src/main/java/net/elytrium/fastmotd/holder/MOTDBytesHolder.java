/*
 * Copyright (C) 2022 - 2023 Elytrium
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

package net.elytrium.fastmotd.holder;

import com.velocitypowered.api.proxy.server.ServerPing;
import io.netty.buffer.ByteBuf;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.utils.ByteBufCopyThreadLocal;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public abstract class MOTDBytesHolder {

  private final ByteBuf byteBuf;
  private final int maxOnlineDigit;
  private final int onlineDigit;
  private final int protocolDigit;
  private ByteBufCopyThreadLocal localByteBuf;

  protected MOTDBytesHolder(ByteBuf byteBuf, int maxOnlineDigit, int onlineDigit, int protocolDigit) {
    this.byteBuf = byteBuf;
    this.maxOnlineDigit = maxOnlineDigit;
    this.onlineDigit = onlineDigit;
    this.protocolDigit = protocolDigit;
    this.localByteBuf = new ByteBufCopyThreadLocal(this.byteBuf);
  }

  public void dispose() {
    if (this.byteBuf.refCnt() != 0) {
      this.byteBuf.release();
    }

    this.localByteBuf.release();
  }

  public void replaceOnline(int max, int online) {
    this.localReplaceOnline(this.byteBuf, this.maxOnlineDigit, max);
    this.localReplaceOnline(this.byteBuf, this.onlineDigit, online);

    ByteBufCopyThreadLocal previousLocalBuffer = this.localByteBuf;
    this.localByteBuf = new ByteBufCopyThreadLocal(this.byteBuf);
    previousLocalBuffer.release();
  }

  protected abstract void localReplaceOnline(ByteBuf byteBuf, int digit, int to);

  public ByteBuf getByteBuf(int protocol, boolean replaceProtocol) {
    ByteBuf buf = this.localByteBuf.get();

    if (replaceProtocol) {
      this.replaceStrInt(buf, this.protocolDigit, this.protocolDigit - 9, protocol);
    }

    return buf.retain();
  }

  protected abstract void replaceStrInt(ByteBuf buf, int startIndex, int endIndex, int toSet);

  public interface Initializer {

    MOTDBytesHolder initialize(FastMOTD plugin, MOTDHolderType type,
        ComponentSerializer<Component, Component, String> inputSerializer, ServerPing pingInfo);
  }
}
