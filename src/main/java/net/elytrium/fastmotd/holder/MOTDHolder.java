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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.elytrium.fastmotd.FastMOTD;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MOTDHolder {

  private final Map<MOTDHolderType, MOTDBytesHolder> holders;
  private final ServerPing compatPingInfo;

  public MOTDHolder(FastMOTD plugin, Set<MOTDHolderType> types, ComponentSerializer<Component, Component, String> serializer,
      String versionName, Component description, String favicon, List<String> information) {
    ServerPing.Builder compatServerPingBuilder = ServerPing.builder()
        .maximumPlayers(0)
        .onlinePlayers(1)
        .samplePlayers(information.stream()
            .map(e -> new ServerPing.SamplePlayer(
                LegacyComponentSerializer.legacySection().serialize(serializer.deserialize(e)), UUID.randomUUID()))
            .toArray(ServerPing.SamplePlayer[]::new))
        .description(description)
        .version(new ServerPing.Version(1, versionName));

    if (favicon != null && !favicon.isEmpty()) {
      compatServerPingBuilder.favicon(new Favicon(favicon));
    }

    this.compatPingInfo = compatServerPingBuilder.build();
    this.holders = types.stream().collect(Collectors.toMap(type -> type, type ->
        type.initialize(plugin, type, serializer, this.compatPingInfo)));
  }

  public void replaceOnline(int max, int online) {
    for (MOTDBytesHolder value : this.holders.values()) {
      value.replaceOnline(max, online);
    }
  }

  public ByteBuf getByteBuf(ProtocolVersion version, boolean replaceProtocol) {
    return this.holders.get(MOTDHolderType.map(version)).getByteBuf(version.getProtocol(), replaceProtocol);
  }

  public ByteBuf getByteBuf(LegacyMinecraftPingVersion version) {
    return this.holders.get(MOTDHolderType.map(version)).getByteBuf(0, false);
  }

  public ServerPing getCompatPingInfo(ProtocolVersion version, boolean replaceProtocol) {
    if (replaceProtocol) {
      return this.compatPingInfo.asBuilder()
          .version(new ServerPing.Version(version.getProtocol(), this.compatPingInfo.getVersion().getName()))
          .build();
    } else {
      return this.compatPingInfo;
    }
  }

  public void dispose() {
    for (MOTDBytesHolder value : this.holders.values()) {
      value.dispose();
    }
  }
}
