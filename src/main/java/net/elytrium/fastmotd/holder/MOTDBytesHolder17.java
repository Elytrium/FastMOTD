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

import com.google.common.primitives.Bytes;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.elytrium.fastmotd.FastMOTD;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MOTDBytesHolder17 extends MOTDBytesHolder {

  public static MOTDBytesHolder initialize(FastMOTD plugin, MOTDHolderType type,
      ComponentSerializer<Component, Component, String> inputSerializer, ServerPing pingInfo) {
    GsonComponentSerializer outputSerializer;
    switch (type) {
      case MINECRAFT_1_7: {
        outputSerializer = ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_15_2);
        break;
      }
      case MINECRAFT_1_16: {
        outputSerializer = ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_16);
        break;
      }
      default: {
        throw new IllegalArgumentException();
      }
    }

    StringBuilder motd = new StringBuilder("{\"players\":{\"max\":    0,\"online\":    1,\"sample\":[");

    pingInfo.getPlayers().ifPresent(players -> {
      List<ServerPing.SamplePlayer> sample = players.getSample();
      int lastIdx = sample.size() - 1;
      if (lastIdx != -1) {
        if (lastIdx > 9) {
          lastIdx = 9;
        }

        for (int i = 0; i < lastIdx; i++) {
          ServerPing.SamplePlayer samplePlayer = sample.get(i);
          motd.append("{\"id\":\"")
              .append(samplePlayer.getId().toString())
              .append("\",\"name\":\"")
              .append(toLegacy(inputSerializer, samplePlayer.getName())).append("\"},");
        }

        ServerPing.SamplePlayer lastPlayer = sample.get(lastIdx);
        motd.append("{\"id\":\"")
            .append(lastPlayer.getId().toString())
            .append("\",\"name\":\"")
            .append(toLegacy(inputSerializer, lastPlayer.getName()))
            .append("\"}");
      }
    });

    motd.append("]},\"description\":")
        .append(outputSerializer.serialize(pingInfo.getDescriptionComponent()))
        .append(",\"version\":{\"name\":\"")
        .append(pingInfo.getVersion().getName())
        .append("\",\"protocol\":        1}");

    pingInfo.getFavicon().ifPresent(favicon -> motd.append(",\"favicon\":\"")
        .append(favicon)
        .append("\""));

    motd.append("}");

    byte[] bytes = motd.toString().getBytes(StandardCharsets.UTF_8);
    int varIntLength = ProtocolUtils.varIntBytes(bytes.length);
    int length = bytes.length + varIntLength + 1;
    int lengthOfLength = ProtocolUtils.varIntBytes(length);
    varIntLength += lengthOfLength;

    ByteBuf byteBuf = Unpooled.directBuffer(length + lengthOfLength);
    ProtocolUtils.writeVarInt(byteBuf, length);
    byteBuf.writeByte(0);
    ProtocolUtils.writeVarInt(byteBuf, bytes.length);
    byteBuf.writeBytes(bytes);

    int maxOnlineDigit = Bytes.indexOf(bytes, "    0".getBytes(StandardCharsets.UTF_8)) + 1 + varIntLength;
    int onlineDigit = Bytes.indexOf(bytes, "    1".getBytes(StandardCharsets.UTF_8)) + 1 + varIntLength;
    int protocolDigit = Bytes.indexOf(bytes, "protocol\":        1}".getBytes(StandardCharsets.UTF_8)) + 19 + varIntLength;

    return new MOTDBytesHolder17(byteBuf, maxOnlineDigit, onlineDigit, protocolDigit);
  }

  protected MOTDBytesHolder17(ByteBuf byteBuf, int maxOnlineDigit, int onlineDigit, int protocolDigit) {
    super(byteBuf, maxOnlineDigit, onlineDigit, protocolDigit);
  }

  @Override
  protected void localReplaceOnline(ByteBuf byteBuf, int digit, int to) {
    byteBuf.setByte(digit, to >= 10000 ? (to / 10000 % 10) + '0' : ' ');
    byteBuf.setByte(digit + 1, to >= 1000 ? (to / 1000 % 10) + '0' : ' ');
    byteBuf.setByte(digit + 2, to >= 100 ? (to / 100 % 10) + '0' : ' ');
    byteBuf.setByte(digit + 3, to >= 10 ? (to / 10 % 10) + '0' : ' ');
    byteBuf.setByte(digit + 4, (to % 10) + '0');
  }

  @Override
  protected void replaceStrInt(ByteBuf buf, int startIndex, int endIndex, int toSet) {
    while (toSet > 0) {
      buf.setByte(startIndex--, (toSet % 10) + '0');
      toSet /= 10;
    }

    while (startIndex != endIndex) {
      buf.setByte(startIndex--, ' ');
    }
  }

  private static String toLegacy(ComponentSerializer<Component, Component, String> inputSerializer, String from) {
    return LegacyComponentSerializer.legacySection().serialize(inputSerializer.deserialize(from));
  }
}
