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

package net.elytrium.fastmotd.injection;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import com.velocitypowered.proxy.protocol.packet.LegacyHandshake;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HandshakeSessionHandlerHook extends HandshakeSessionHandler {

  private final FastMOTD plugin;
  private final MinecraftConnection connection;
  private final Channel channel;
  private final HandshakeSessionHandler original;
  private final LegacyDisconnect legacyDisconnect = LegacyDisconnect.from(Component.text(
      "Your client is extremely old. Please update to a newer version of Minecraft.",
      NamedTextColor.RED)
  );
  private ProtocolVersion protocolVersion;
  private String serverAddress;

  public HandshakeSessionHandlerHook(FastMOTD plugin, MinecraftConnection connection, Channel channel, HandshakeSessionHandler original) {
    super(connection, plugin.getServer());
    this.plugin = plugin;
    this.connection = connection;
    this.channel = channel;
    this.original = original;
  }

  @Override
  public boolean handle(LegacyPing packet) {
    this.channel.pipeline().remove(Connections.FRAME_ENCODER);
    InetSocketAddress address = packet.getVhost();
    if (address != null) {
      this.serverAddress = address.getHostString() + ":" + address.getPort();
    }

    this.connection.closeWith(this.plugin.getNext(packet.getVersion(), this.serverAddress));
    return true;
  }

  @Override
  public boolean handle(LegacyHandshake packet) {
    this.connection.closeWith(this.legacyDisconnect);
    return true;
  }

  @Override
  public boolean handle(Handshake handshake) {
    if (handshake.getNextStatus() == StateRegistry.STATUS_ID) {
      this.protocolVersion = handshake.getProtocolVersion();
      this.serverAddress = handshake.getServerAddress() + ":" + handshake.getPort();
      this.channel.pipeline().remove(Connections.FRAME_ENCODER);
      this.channel.pipeline().get(MinecraftDecoder.class).setState(StateRegistry.STATUS);

      if (Settings.IMP.MAIN.LOG_PINGS) {
        this.plugin.getLogger().info("{} is pinging the server with version {}", this.connection.getRemoteAddress(), this.protocolVersion);
      }
      return true;
    } else if (handshake.getNextStatus() == StateRegistry.LOGIN_ID
        && Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED && Settings.IMP.MAINTENANCE.SHOULD_KICK_ON_JOIN
        && !this.plugin.checkKickWhitelist(((InetSocketAddress) this.connection.getRemoteAddress()).getAddress())) {
      this.connection.setProtocolVersion(handshake.getProtocolVersion());
      this.channel.pipeline().remove(Connections.FRAME_ENCODER);
      this.plugin.inject(this.connection, this.channel.pipeline());
      this.connection.closeWith(this.plugin.getKickReason());
    }

    return this.original.handle(handshake);
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof StatusPing) {
      if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
        this.connection.close();
        return;
      }

      ByteBuf buf = Unpooled.directBuffer(11);
      buf.writeByte(9);
      buf.writeByte(1);
      packet.encode(buf, null, null);
      this.channel.writeAndFlush(buf);
      this.connection.close();
    } else if (packet instanceof StatusRequest) {
      this.channel.writeAndFlush(this.plugin.getNext(this.protocolVersion, this.serverAddress));
    } else {
      this.original.handleGeneric(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    this.original.handleUnknown(buf);
  }
}
