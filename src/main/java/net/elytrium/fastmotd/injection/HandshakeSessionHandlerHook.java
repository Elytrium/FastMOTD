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

package net.elytrium.fastmotd.injection;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.LegacyHandshakePacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusRequestPacket;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import java.net.InetSocketAddress;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;

public class HandshakeSessionHandlerHook extends HandshakeSessionHandler {

  private static final QuietRuntimeException UNEXPECTED_STATE =
      new QuietRuntimeException("unexpected state");

  private enum State {
    REQUEST, PING, DONE
  }

  private final FastMOTD plugin;
  private final MinecraftConnection connection;
  private final Channel channel;
  private final HandshakeSessionHandler original;
  private ProtocolVersion protocolVersion;
  private String serverAddress;
  private State state = State.REQUEST;

  public HandshakeSessionHandlerHook(FastMOTD plugin, MinecraftConnection connection, Channel channel, HandshakeSessionHandler original) {
    super(connection, plugin.getServer());
    this.plugin = plugin;
    this.connection = connection;
    this.channel = channel;
    this.original = original;
  }

  private static String cleanHost(String hostname) {
    String cleaned = hostname;
    int zeroIdx = cleaned.indexOf(0);
    if (zeroIdx > -1) {
      cleaned = hostname.substring(0, zeroIdx);
    }

    if (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) == '.') {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }

    return cleaned;
  }

  private void switchState(State oldState, State newState) {
    if (Settings.IMP.MAIN.ALLOW_IMPROPER_PINGS) {
      return;
    }

    if (this.state != oldState) {
      if (Settings.IMP.MAIN.LOG_IMPROPER_PINGS) {
        this.plugin.getLogger().warn("{} has failed to ping this proxy due to improper packet order: from {} to {}->{}",
            this.connection.getRemoteAddress(), this.state, oldState, newState);
      }

      throw UNEXPECTED_STATE;
    }

    this.state = newState;
  }

  private void sendPacket(ByteBuf packet, boolean constant) {
    if (Settings.IMP.MAIN.DIRECT_WRITE) {
      ChannelOutboundBuffer buffer = this.channel.unsafe().outboundBuffer();
      if (buffer == null) {
        packet.release(); // connection was closed already, no need to send the packet.
      } else {
        // .slice() constant packet to ensure that Netty do not modify its readerIndex
        if (constant) {
          packet = packet.slice();
        }

        // Send the packet
        buffer.addMessage(packet, packet.readableBytes(), this.channel.voidPromise());
        this.channel.flush();
      }
    } else {
      this.channel.writeAndFlush(packet);
    }
  }

  @Override
  public boolean handle(LegacyPingPacket packet) {
    this.connection.close();
    return true;
  }

  @Override
  public boolean handle(LegacyHandshakePacket packet) {
    this.connection.close();
    return true;
  }

  @Override
  public boolean handle(HandshakePacket handshake) {
    if (handshake.getNextStatus() == StateRegistry.STATUS_ID) {
      if (handshake.getProtocolVersion() == null || handshake.getProtocolVersion() == ProtocolVersion.UNKNOWN) {
        handshake.setProtocolVersion(ProtocolVersion.MAXIMUM_VERSION);

        if (Settings.IMP.MAIN.LOG_PINGS) {
          this.plugin.getLogger().info(
              "Unknown protocol version detected from {}, replaced with version {}",
              this.connection.getRemoteAddress(),
              ProtocolVersion.MAXIMUM_VERSION
          );
        }
      }

      this.protocolVersion = handshake.getProtocolVersion();
      this.serverAddress = cleanHost(handshake.getServerAddress()) + ":" + handshake.getPort();

      ChannelPipeline pipeline = this.channel.pipeline();
      pipeline.remove(Connections.FRAME_ENCODER);
      pipeline.get(MinecraftDecoder.class).setState(StateRegistry.STATUS);
      MinecraftVarintFrameDecoder frameDecoder = pipeline.get(MinecraftVarintFrameDecoder.class);
      if (frameDecoder != null) {
        frameDecoder.setState(StateRegistry.STATUS);
      }

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
    if (packet instanceof StatusPingPacket) {
      this.switchState(State.PING, State.DONE);
      if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
        this.connection.close();
        return;
      }

      ByteBuf buf = Unpooled.directBuffer(11);
      buf.writeByte(9);
      buf.writeByte(1);
      packet.encode(buf, null, null);
      this.sendPacket(buf, false);
      this.connection.close();
    } else if (packet instanceof StatusRequestPacket) {
      this.switchState(State.REQUEST, State.PING);
      this.sendPacket(this.plugin.getNext(this.protocolVersion, this.serverAddress), true);
    } else {
      this.original.handleGeneric(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    this.original.handleUnknown(buf);
  }
}
