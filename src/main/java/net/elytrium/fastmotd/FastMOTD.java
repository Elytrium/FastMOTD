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
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializer;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import net.elytrium.fastmotd.command.ReloadCommand;
import net.elytrium.fastmotd.injection.ServerChannelInitializerHook;
import net.elytrium.fastmotd.utils.MOTDGenerator;
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
  private MOTDGenerator motdGenerator;
  private ScheduledTask updater;

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

    if (this.motdGenerator != null) {
      this.motdGenerator.dispose();
    }

    this.server.getCommandManager().unregister("fastmotdreload");
    this.server.getCommandManager().register("fastmotdreload", new ReloadCommand(this));

    if (this.updater != null) {
      this.updater.cancel();
    }

    ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Settings.IMP.SERIALIZER).getSerializer();
    this.motdGenerator = new MOTDGenerator(this, serializer);
    this.motdGenerator.generate();

    this.updater = this.server.getScheduler()
        .buildTask(this, this.motdGenerator::update)
        .repeat(Settings.IMP.MAIN.UPDATE_RATE, TimeUnit.MILLISECONDS)
        .schedule();
  }

  public ByteBuf getNext(ProtocolVersion version) {
    return this.motdGenerator.getNext(version);
  }

  public VelocityServer getServer() {
    return this.server;
  }

  public Logger getLogger() {
    return this.logger;
  }
}
