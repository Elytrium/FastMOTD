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
import com.velocitypowered.api.proxy.server.ServerPing;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import net.elytrium.fastmotd.FastMOTD;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MOTDBytesHolder13 extends MOTDBytesHolder {

  private static final String SECTION_STRING = Character
      .toString(LegacyComponentSerializer.SECTION_CHAR);

  public static MOTDBytesHolder initialize(FastMOTD plugin, MOTDHolderType type,
      ComponentSerializer<Component, Component, String> inputSerializer, ServerPing pingInfo) {
    String disconnectData;
    switch (type) {
      case MINECRAFT_1_3: {
        disconnectData = String.join(
            SECTION_STRING,
            cleanSectionSymbol(getFirstLine(PlainTextComponentSerializer.plainText().serialize(
                pingInfo.getDescriptionComponent()))),
            "00000",
            "00001"
        );
        break;
      }
      case MINECRAFT_1_4:
      case MINECRAFT_1_6: {
        disconnectData = String.join(
            "\0",
            SECTION_STRING + "1",
            Integer.toString(pingInfo.getVersion().getProtocol()),
            pingInfo.getVersion().getName(),
            getFirstLine(LegacyComponentSerializer.legacySection().serialize(pingInfo.getDescriptionComponent())),
            "00000",
            "00001"
        );
        break;
      }
      default: {
        throw new IllegalArgumentException();
      }
    }

    ByteBuf byteBuf = Unpooled.directBuffer();
    byteBuf.writeByte(0xff);
    byteBuf.writeShort(disconnectData.length());
    byteBuf.writeCharSequence(disconnectData, StandardCharsets.UTF_16BE);

    byte[] bytes = disconnectData.getBytes(StandardCharsets.UTF_16BE);
    int maxOnlineDigit = Bytes.indexOf(bytes, "00000".getBytes(StandardCharsets.UTF_16BE)) + 1;
    int onlineDigit = Bytes.indexOf(bytes, "00001".getBytes(StandardCharsets.UTF_16BE)) + 1;

    return new MOTDBytesHolder13(byteBuf, maxOnlineDigit, onlineDigit);
  }

  private static String cleanSectionSymbol(String string) {
    return string.replace(SECTION_STRING, "");
  }

  private static String getFirstLine(String legacyMOTD) {
    int newline = legacyMOTD.indexOf('\n');
    return newline == -1 ? legacyMOTD : legacyMOTD.substring(0, newline);
  }

  protected MOTDBytesHolder13(ByteBuf byteBuf, int maxOnlineDigit, int onlineDigit) {
    super(byteBuf, maxOnlineDigit, onlineDigit, Integer.MIN_VALUE);
  }

  @Override
  protected void localReplaceOnline(ByteBuf byteBuf, int digit, int to) {
    byteBuf.setByte(digit + 1, (to / 10000 % 10) + '0');
    byteBuf.setByte(digit + 3, (to / 1000 % 10) + '0');
    byteBuf.setByte(digit + 5, (to / 100 % 10) + '0');
    byteBuf.setByte(digit + 7, (to / 10 % 10) + '0');
    byteBuf.setByte(digit + 9, (to % 10) + '0');
  }

  @Override
  protected void replaceStrInt(ByteBuf buf, int startIndex, int endIndex, int toSet) {
    throw new UnsupportedOperationException();
  }
}
