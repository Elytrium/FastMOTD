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

package net.elytrium.fastmotd.injection;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.network.ServerChannelInitializer;
import io.netty.channel.Channel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.elytrium.fastmotd.FastMOTD;

public class ServerChannelInitializerHook extends ServerChannelInitializer {

  private static Method initChannel;
  private final FastMOTD plugin;
  private final ServerChannelInitializer oldHook;

  static {
    try {
      initChannel = ServerChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
      initChannel.setAccessible(true);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  public ServerChannelInitializerHook(FastMOTD plugin, VelocityServer server, ServerChannelInitializer oldHook) {
    super(server);
    this.plugin = plugin;
    this.oldHook = oldHook;
  }

  @Override
  protected void initChannel(Channel ch) {
    try {
      initChannel.invoke(this.oldHook, ch);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    MinecraftConnection connection = (MinecraftConnection) ch.pipeline().get(Connections.HANDLER);
    MinecraftSessionHandler handshakeSessionHandlerHook = (MinecraftSessionHandler) Proxy.newProxyInstance(
        ServerChannelInitializer.class.getClassLoader(),
        new Class[] { MinecraftSessionHandler.class },
        new HandshakeSessionHandlerHook(this.plugin, connection, ch, (HandshakeSessionHandler) connection.getSessionHandler()));
    connection.setSessionHandler(handshakeSessionHandlerHook);
  }
}
