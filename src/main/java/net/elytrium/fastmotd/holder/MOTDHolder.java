/*
 * Copyright (C) 2022 Elytrium
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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.elytrium.fastmotd.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public class MOTDHolder {

  private final MOTDBytesHolder legacyHolder;
  private final MOTDBytesHolder modernHolder;

  public MOTDHolder(ComponentSerializer<Component, Component, String> serializer, String descriptionSerialized, String favicon) {
    String name = Settings.IMP.MAIN.VERSION_NAME.replace("\"", "\\\"");
    Component description = serializer.deserialize(descriptionSerialized.replace("{NL}", "\n"));

    this.legacyHolder =
        new MOTDBytesHolder(serializer, ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_15_2), name, description, favicon);
    this.modernHolder =
        new MOTDBytesHolder(serializer, ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_16), name, description, favicon);
  }

  public void replaceOnline(int max, int online) {
    this.legacyHolder.replaceOnline(max, online);
    this.modernHolder.replaceOnline(max, online);
  }

  public ByteBuf getByteBuf(ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      return this.modernHolder.getByteBuf(version);
    } else {
      return this.legacyHolder.getByteBuf(version);
    }
  }

  public void dispose() {
    this.legacyHolder.dispose();
    this.modernHolder.dispose();
  }
}
