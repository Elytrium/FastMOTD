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

package net.elytrium.fastmotd;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializer;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import java.io.File;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.elytrium.fastmotd.command.ReloadCommand;
import net.elytrium.fastmotd.dummy.DummyPlayer;
import net.elytrium.fastmotd.injection.ServerChannelInitializerHook;
import net.elytrium.fastmotd.utils.MOTDGenerator;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.java.commons.mc.serialization.Serializers;
import net.elytrium.java.commons.updates.UpdatesChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "fastmotd",
    name = "FastMOTD",
    version = "1.0.0",
    description = "MOTD plugin that uses FastPrepareAPI.",
    url = "ely.su",
    authors = {
        "hevav",
        "mdxd44"
    }
)
public class FastMOTD {

  private static Field connectionManager;
  private static Field initializer;

  private final Logger logger;
  private final VelocityServer server;
  private final Metrics.Factory metricsFactory;
  private final File configFile;
  private PreparedPacketFactory preparedPacketFactory;
  private MOTDGenerator motdGenerator;
  private MOTDGenerator maintenanceMOTDGenerator;
  private ScheduledTask updater;
  private PreparedPacket kickReason;
  private Set<InetAddress> kickWhitelist;

  static {
    try {
      connectionManager = VelocityServer.class.getDeclaredField("cm");
      connectionManager.setAccessible(true);

      initializer = ServerChannelInitializerHolder.class.getDeclaredField("initializer");
      initializer.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  @Inject
  public FastMOTD(Logger logger, ProxyServer server, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
    this.logger = logger;
    this.server = (VelocityServer) server;
    this.metricsFactory = metricsFactory;
    this.configFile = dataDirectory.resolve("config.yml").toFile();
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    try {
      ConnectionManager cm = (ConnectionManager) connectionManager.get(this.server);
      ServerChannelInitializer oldInitializer = (ServerChannelInitializer) initializer.get(cm.serverChannelInitializer);
      initializer.set(cm.serverChannelInitializer, new ServerChannelInitializerHook(this, this.server, oldInitializer));
      this.logger.info("Hooked into ServerChannelInitializer");
    } catch (IllegalAccessException e) {
      this.logger.info("Error while hooking into ServerChannelInitializer");
      e.printStackTrace();
    }

    this.preparedPacketFactory =
        new PreparedPacketFactory(PreparedPacket::new, StateRegistry.LOGIN, false, 1, 1);

    this.reload();
  }

  public void reload() {
    Settings.IMP.reload(this.configFile);

    if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/FastMOTD/master/VERSION", Settings.IMP.VERSION)) {
      this.logger.error("****************************************");
      this.logger.warn("The new FastMOTD update was found, please update.");
      this.logger.error("https://github.com/Elytrium/FastMOTD/releases/");
      this.logger.error("****************************************");
    }
    this.metricsFactory.make(this, 15640);

    ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Settings.IMP.SERIALIZER).getSerializer();
    if (serializer == null) {
      this.logger.error("Incorrect serializer set: {}", Settings.IMP.SERIALIZER);
      return;
    }

    if (this.motdGenerator != null) {
      this.motdGenerator.dispose();
      this.maintenanceMOTDGenerator.dispose();
    }

    this.server.getCommandManager().unregister("fastmotdreload");
    this.server.getCommandManager().register("fastmotdreload", new ReloadCommand(this));

    if (this.updater != null) {
      this.updater.cancel();
    }

    Component kickReasonComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.KICK_MESSAGE);
    this.kickReason = this.preparedPacketFactory
        .createPreparedPacket(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION)
        .prepare(version -> Disconnect.create(kickReasonComponent, version))
        .build();

    this.kickWhitelist = Settings.IMP.MAINTENANCE.KICK_WHITELIST.stream().map((String host) -> {
      try {
        return InetAddress.getByName(host);
      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

    this.motdGenerator =
        new MOTDGenerator(this, serializer, Settings.IMP.MAIN.VERSION_NAME,
            Settings.IMP.MAIN.DESCRIPTIONS, Settings.IMP.MAIN.FAVICONS, Settings.IMP.MAIN.INFORMATION);
    this.motdGenerator.generate();
    this.maintenanceMOTDGenerator =
        new MOTDGenerator(this, serializer, Settings.IMP.MAINTENANCE.VERSION_NAME,
            Settings.IMP.MAINTENANCE.DESCRIPTIONS, Settings.IMP.MAINTENANCE.FAVICONS, Settings.IMP.MAINTENANCE.INFORMATION);
    this.maintenanceMOTDGenerator.generate();

    this.updater = this.server.getScheduler()
        .buildTask(this, this::updateMOTD)
        .repeat(Settings.IMP.MAIN.UPDATE_RATE, TimeUnit.MILLISECONDS)
        .schedule();
  }

  private void updateMOTD() {
    int online = this.getOnline();
    int max = this.getMax(online);
    this.motdGenerator.update(max, online);

    if (Settings.IMP.MAINTENANCE.OVERRIDE_MAX_ONLINE != -1) {
      max = Settings.IMP.MAINTENANCE.OVERRIDE_MAX_ONLINE;
    }

    if (Settings.IMP.MAINTENANCE.OVERRIDE_ONLINE != -1) {
      online = Settings.IMP.MAINTENANCE.OVERRIDE_ONLINE;
    }

    this.maintenanceMOTDGenerator.update(max, online);
  }

  private int getOnline() {
    int online = this.server.getPlayerCount() + Settings.IMP.MAIN.FAKE_ONLINE_ADD_SINGLE;
    return online * (Settings.IMP.MAIN.FAKE_ONLINE_ADD_PERCENT + 100) / 100;
  }

  private int getMax(int online) {
    int max;
    MaxCountType type = MaxCountType.valueOf(Settings.IMP.MAIN.MAX_COUNT_TYPE);
    switch (type) {
      case ADD_SOME:
        max = online + Settings.IMP.MAIN.MAX_COUNT;
        break;
      case VARIABLE:
        max = Settings.IMP.MAIN.MAX_COUNT;
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }

    return max;
  }

  public ByteBuf getNext(ProtocolVersion version) {
    if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
      return this.maintenanceMOTDGenerator.getNext(version, !Settings.IMP.MAINTENANCE.SHOW_VERSION);
    } else {
      return this.motdGenerator.getNext(version, true);
    }
  }

  public void inject(MinecraftConnection connection, ChannelPipeline pipeline) {
    this.preparedPacketFactory.inject(DummyPlayer.INSTANCE, connection, pipeline);
  }

  public boolean checkKickWhitelist(InetAddress inetAddress) {
    return this.kickWhitelist.contains(inetAddress);
  }

  public VelocityServer getServer() {
    return this.server;
  }

  public Logger getLogger() {
    return this.logger;
  }

  public PreparedPacket getKickReason() {
    return this.kickReason;
  }

  private enum MaxCountType {

    VARIABLE,
    ADD_SOME
  }
}
