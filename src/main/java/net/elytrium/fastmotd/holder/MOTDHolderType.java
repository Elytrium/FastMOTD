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
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import net.elytrium.fastmotd.FastMOTD;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public enum MOTDHolderType {

  MINECRAFT_1_3(MOTDBytesHolder13::initialize, LegacyMinecraftPingVersion.MINECRAFT_1_3),
  MINECRAFT_1_4(MOTDBytesHolder13::initialize, LegacyMinecraftPingVersion.MINECRAFT_1_4),
  MINECRAFT_1_6(MOTDBytesHolder13::initialize, LegacyMinecraftPingVersion.MINECRAFT_1_6),
  MINECRAFT_1_7(MOTDBytesHolder17::initialize, EnumSet.range(ProtocolVersion.LEGACY, ProtocolVersion.MINECRAFT_1_15_2)),
  MINECRAFT_1_16(MOTDBytesHolder17::initialize, EnumSet.range(ProtocolVersion.MINECRAFT_1_16, ProtocolVersion.MAXIMUM_VERSION));

  private static final EnumMap<LegacyMinecraftPingVersion, MOTDHolderType> LEGACY_VERSION_TO_TYPE
      = new EnumMap<>(LegacyMinecraftPingVersion.class);
  private static final EnumMap<ProtocolVersion, MOTDHolderType> VERSION_TO_TYPE = new EnumMap<>(ProtocolVersion.class);
  private final MOTDBytesHolder.Initializer initializer;
  private final Set<LegacyMinecraftPingVersion> legacyVersions;
  private final Set<ProtocolVersion> versions;

  MOTDHolderType(MOTDBytesHolder.Initializer initializer, LegacyMinecraftPingVersion... versions) {
    this.initializer = initializer;
    this.legacyVersions = EnumSet.copyOf(Arrays.asList(versions));
    this.versions = Collections.emptySet();
  }

  MOTDHolderType(MOTDBytesHolder.Initializer initializer, Set<ProtocolVersion> versions) {
    this.initializer = initializer;
    this.legacyVersions = Collections.emptySet();
    this.versions = versions;
  }

  public MOTDBytesHolder initialize(FastMOTD plugin, MOTDHolderType type,
      ComponentSerializer<Component, Component, String> inputSerializer, ServerPing pingInfo) {
    return this.initializer.initialize(plugin, type, inputSerializer, pingInfo);
  }

  static {
    for (MOTDHolderType version : MOTDHolderType.values()) {
      for (LegacyMinecraftPingVersion protocolVersion : version.legacyVersions) {
        LEGACY_VERSION_TO_TYPE.put(protocolVersion, version);
      }
      for (ProtocolVersion protocolVersion : version.versions) {
        VERSION_TO_TYPE.put(protocolVersion, version);
      }
    }
  }

  public static MOTDHolderType map(ProtocolVersion protocolVersion) {
    return VERSION_TO_TYPE.get(protocolVersion);
  }

  public static MOTDHolderType map(LegacyMinecraftPingVersion protocolVersion) {
    return LEGACY_VERSION_TO_TYPE.get(protocolVersion);
  }
}
