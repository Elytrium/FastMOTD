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

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetSocketAddress;
import net.elytrium.commons.utils.reflection.ReflectionException;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import org.jetbrains.annotations.NotNull;

public class ServerChannelInitializerHook extends ChannelInitializer<Channel> {

  private static final MethodHandle initChannel;
  private final FastMOTD plugin;
  private final ChannelInitializer<?> original;

  static {
    try {
      initChannel = MethodHandles.privateLookupIn(ChannelInitializer.class, MethodHandles.lookup())
          .findVirtual(ChannelInitializer.class, "initChannel", MethodType.methodType(void.class, Channel.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  public ServerChannelInitializerHook(FastMOTD plugin, ChannelInitializer<?> original) {
    this.plugin = plugin;
    this.original = original;
  }

  @Override
  protected void initChannel(@NotNull Channel ch) {
    if (Settings.IMP.SHUTDOWN_SCHEDULER.SHUTDOWN_SCHEDULER_ENABLED) {
      if (!Settings.IMP.SHUTDOWN_SCHEDULER.WHITELIST.contains(((InetSocketAddress) ch.remoteAddress()).getAddress().getHostAddress())) {
        ch.close();
        return;
      }
    }

    try {
      initChannel.invokeExact(this.original, ch);
    } catch (Throwable e) {
      throw new ReflectionException(e);
    }

    MinecraftConnection connection = (MinecraftConnection) ch.pipeline().get(Connections.HANDLER);
    connection.setActiveSessionHandler(connection.getState(), new HandshakeSessionHandlerHook(
            this.plugin, connection, ch, (HandshakeSessionHandler) connection.getActiveSessionHandler()));
  }
}
