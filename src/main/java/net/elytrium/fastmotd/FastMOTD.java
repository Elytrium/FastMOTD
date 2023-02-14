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

package net.elytrium.fastmotd;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ServerChannelInitializerHolder;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.elytrium.fastmotd.command.MaintenanceCommand;
import net.elytrium.fastmotd.command.ReloadCommand;
import net.elytrium.fastmotd.dummy.DummyPlayer;
import net.elytrium.fastmotd.holder.MOTDHolderType;
import net.elytrium.fastmotd.injection.ServerChannelInitializerHook;
import net.elytrium.fastmotd.listener.CompatPingListener;
import net.elytrium.fastmotd.listener.DisconnectOnZeroPlayersListener;
import net.elytrium.fastmotd.utils.MOTDGenerator;
import net.elytrium.fastprepare.PreparedPacket;
import net.elytrium.fastprepare.PreparedPacketFactory;
import net.elytrium.java.commons.reflection.ReflectionException;
import net.elytrium.java.commons.updates.UpdatesChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

@Plugin(
    id = "fastmotd",
    name = "FastMOTD",
    version = BuildConstants.VERSION,
    description = "MOTD plugin that uses FastPrepareAPI.",
    url = "https://elytrium.net/",
    authors = {
        "Elytrium (https://elytrium.net/)",
    }
)
public class FastMOTD {

  private static final Field connectionManager;
  private static final Field initializer;

  private final Logger logger;
  private final VelocityServer server;
  private final Metrics.Factory metricsFactory;
  private final File configFile;
  private final List<MOTDGenerator> motdGenerators = new ArrayList<>();
  private final List<MOTDGenerator> maintenanceMOTDGenerators = new ArrayList<>();
  private final Int2IntMap protocolPointers = new Int2IntOpenHashMap();
  private final Int2IntMap maintenanceProtocolPointers = new Int2IntOpenHashMap();
  private final Map<String, MOTDGenerator> domainMOTD = new HashMap<>();
  private final Map<String, MOTDGenerator> domainMaintenanceMOTD = new HashMap<>();
  private PreparedPacketFactory loginPreparedPacketFactory;
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
      throw new ReflectionException(e);
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
      ChannelInitializer<?> oldInitializer = (ChannelInitializer<?>) initializer.get(cm.serverChannelInitializer);
      initializer.set(cm.serverChannelInitializer, new ServerChannelInitializerHook(this, oldInitializer));
      this.logger.info("Hooked into ServerChannelInitializer");
    } catch (IllegalAccessException e) {
      this.logger.info("Error while hooking into ServerChannelInitializer");
      throw new ReflectionException(e);
    }

    this.loginPreparedPacketFactory =
        new PreparedPacketFactory(PreparedPacket::new, StateRegistry.LOGIN, false, 1, 1, false);

    this.reload();
  }

  public void reload() {
    Settings.IMP.reload(this.configFile, Settings.IMP.PREFIX);

    if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/FastMOTD/master/VERSION", Settings.IMP.VERSION)) {
      this.logger.error("****************************************");
      this.logger.warn("The new FastMOTD update was found, please update.");
      this.logger.error("https://github.com/Elytrium/FastMOTD/releases/");
      this.logger.error("****************************************");
    }
    this.metricsFactory.make(this, 15640);

    ComponentSerializer<Component, Component, String> serializer = Settings.IMP.SERIALIZER.getSerializer();
    if (serializer == null) {
      this.logger.error("Incorrect serializer set: {}", Settings.IMP.SERIALIZER);
      return;
    }

    this.motdGenerators.forEach(MOTDGenerator::dispose);
    this.maintenanceMOTDGenerators.forEach(MOTDGenerator::dispose);
    this.domainMOTD.values().forEach(MOTDGenerator::dispose);
    this.domainMaintenanceMOTD.values().forEach(MOTDGenerator::dispose);

    this.protocolPointers.clear();
    this.motdGenerators.clear();
    this.domainMOTD.clear();

    this.maintenanceProtocolPointers.clear();
    this.maintenanceMOTDGenerators.clear();
    this.domainMaintenanceMOTD.clear();

    CommandManager commandManager = this.server.getCommandManager();
    commandManager.unregister("fastmotdreload");
    commandManager.unregister("maintenance");

    commandManager.register("fastmotdreload", new ReloadCommand(this));
    commandManager.register("maintenance",
        new MaintenanceCommand(this, serializer.deserialize(Settings.IMP.MAINTENANCE.COMMAND.USAGE)));

    EventManager eventManager = this.server.getEventManager();
    eventManager.unregisterListeners(this);
    eventManager.register(this, new CompatPingListener(this));

    if (Settings.IMP.SHUTDOWN_SCHEDULER.SHUTDOWN_SCHEDULER_ENABLED && Settings.IMP.SHUTDOWN_SCHEDULER.SHUTDOWN_ON_ZERO_PLAYERS) {
      eventManager.register(this, new DisconnectOnZeroPlayersListener(this));
    }

    if (this.updater != null) {
      this.updater.cancel();
    }

    if (this.kickReason != null) {
      this.kickReason.release();
    }

    Component kickReasonComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.KICK_MESSAGE);
    this.kickReason = this.loginPreparedPacketFactory
        .createPreparedPacket(ProtocolVersion.MINIMUM_VERSION, ProtocolVersion.MAXIMUM_VERSION)
        .prepare(version -> Disconnect.create(kickReasonComponent, version))
        .build();

    this.kickWhitelist = Settings.IMP.MAINTENANCE.KICK_WHITELIST.stream().map((String host) -> {
      try {
        return InetAddress.getByName(host);
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException(e);
      }
    }).collect(Collectors.toSet());

    this.generateMOTDGenerators(serializer, Settings.IMP.MAIN.VERSION_NAME, Settings.IMP.MAIN.DESCRIPTIONS,
            Settings.IMP.MAIN.FAVICONS, Settings.IMP.MAIN.INFORMATION, this.motdGenerators, this.protocolPointers,
            Settings.IMP.MAIN.VERSIONS.DESCRIPTIONS, Settings.IMP.MAIN.VERSIONS.FAVICONS, Settings.IMP.MAIN.VERSIONS.INFORMATION,
            Settings.IMP.MAIN.DOMAINS, this.domainMOTD);

    this.generateMOTDGenerators(serializer, Settings.IMP.MAINTENANCE.VERSION_NAME, Settings.IMP.MAINTENANCE.DESCRIPTIONS,
            Settings.IMP.MAINTENANCE.FAVICONS, Settings.IMP.MAINTENANCE.INFORMATION, this.maintenanceMOTDGenerators,
            this.maintenanceProtocolPointers, Settings.IMP.MAINTENANCE.VERSIONS.DESCRIPTIONS,
            Settings.IMP.MAINTENANCE.VERSIONS.FAVICONS, Settings.IMP.MAINTENANCE.VERSIONS.INFORMATION,
            Settings.IMP.MAINTENANCE.DOMAINS, this.domainMaintenanceMOTD);

    this.updater = this.server.getScheduler()
        .buildTask(this, this::updateMOTD)
        .repeat(Settings.IMP.MAIN.UPDATE_RATE, TimeUnit.MILLISECONDS)
        .schedule();
  }

  private void generateMOTDGenerators(
          ComponentSerializer<Component, Component, String> serializer,
          String versionName, List<String> defaultDescriptions, List<String> defaultFavicons,
          List<String> defaultInformation, List<MOTDGenerator> dest, Int2IntMap destPointers,
          Map<String, List<String>> descriptionVersions, Map<String, List<String>> faviconVersions,
          Map<String, List<String>> informationVersions, Map<String, Settings.DOMAIN_MOTD_NODE> domainMotd,
          Map<String, MOTDGenerator> domainDest) {
    descriptionVersions = Objects.requireNonNullElseGet(descriptionVersions, HashMap::new);
    faviconVersions = Objects.requireNonNullElseGet(faviconVersions, HashMap::new);
    informationVersions = Objects.requireNonNullElseGet(informationVersions, HashMap::new);
    List<String> nonNullDefaultDescriptions = Objects.requireNonNullElseGet(defaultDescriptions, Collections::emptyList);
    List<String> nonNullDefaultFavicons = Objects.requireNonNullElseGet(defaultFavicons, Collections::emptyList);
    List<String> nonNullDefaultInformation = Objects.requireNonNullElseGet(defaultInformation, Collections::emptyList);

    MOTDGenerator defaultMotdGenerator =
        new MOTDGenerator(this, serializer, versionName, nonNullDefaultDescriptions, nonNullDefaultFavicons, nonNullDefaultInformation);
    defaultMotdGenerator.generate(EnumSet.range(MOTDHolderType.MINECRAFT_1_3, MOTDHolderType.MINECRAFT_1_16));
    dest.add(defaultMotdGenerator);

    Int2ObjectMap<List<String>> protocolDescriptions = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<List<String>> protocolIcons = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<List<String>> protocolInformation = new Int2ObjectOpenHashMap<>();

    this.sortByProtocolVersion(descriptionVersions, protocolDescriptions);
    this.sortByProtocolVersion(faviconVersions, protocolIcons);
    this.sortByProtocolVersion(informationVersions, protocolInformation);

    IntSet allProtocols = new IntOpenHashSet();
    allProtocols.addAll(protocolDescriptions.keySet());
    allProtocols.addAll(protocolIcons.keySet());
    allProtocols.addAll(protocolInformation.keySet());

    Map<List<String>, IntSet> protocolsByData = new HashMap<>();

    allProtocols.forEach(protocol -> {
      List<String> key = new ArrayList<>();
      key.addAll(protocolDescriptions.getOrDefault(protocol, nonNullDefaultDescriptions));
      key.addAll(protocolIcons.getOrDefault(protocol, nonNullDefaultFavicons));
      key.addAll(protocolInformation.getOrDefault(protocol, nonNullDefaultInformation));
      protocolsByData.computeIfAbsent(key, k -> new IntOpenHashSet()).add(protocol);
    });

    protocolsByData.values().forEach(identical -> {
      final int idx = dest.size();
      final int key = identical.iterator().nextInt();
      MOTDGenerator motdGenerator = new MOTDGenerator(this, serializer, versionName,
              protocolDescriptions.getOrDefault(key, nonNullDefaultDescriptions),
              protocolIcons.getOrDefault(key, nonNullDefaultFavicons),
              protocolInformation.getOrDefault(key, nonNullDefaultInformation));
      motdGenerator.generate(EnumSet.range(MOTDHolderType.MINECRAFT_1_7, MOTDHolderType.MINECRAFT_1_16));
      dest.add(motdGenerator);
      identical.forEach(p -> destPointers.put(p, idx));
    });

    domainMotd.forEach((domain, motdNode) -> {
      MOTDGenerator motdGenerator = new MOTDGenerator(this, serializer, versionName,
              motdNode.DESCRIPTION, motdNode.FAVICON, motdNode.INFORMATION);
      motdGenerator.generate(EnumSet.range(MOTDHolderType.MINECRAFT_1_6, MOTDHolderType.MINECRAFT_1_16));
      domainDest.put(domain, motdGenerator);
    });
  }

  private void sortByProtocolVersion(Map<String, List<String>> src, Int2ObjectMap<List<String>> dest) {
    src.forEach((key, value) -> {
      IntStream range;
      if (key.contains("-")) {
        String[] parts = key.split("-");
        range = IntStream.rangeClosed(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } else {
        range = IntStream.of(Integer.parseInt(key));
      }
      range.forEach(protocol -> dest.computeIfAbsent(protocol, p -> new ArrayList<>()).addAll(value));
    });
  }

  private void updateMOTD() {
    int online = this.getOnline();
    int max = this.getMax(online);

    for (MOTDGenerator generator : this.motdGenerators) {
      generator.update(max, online);
    }

    for (MOTDGenerator generator : this.domainMOTD.values()) {
      generator.update(max, online);
    }

    if (Settings.IMP.MAINTENANCE.OVERRIDE_MAX_ONLINE != -1) {
      max = Settings.IMP.MAINTENANCE.OVERRIDE_MAX_ONLINE;
    }

    if (Settings.IMP.MAINTENANCE.OVERRIDE_ONLINE != -1) {
      online = Settings.IMP.MAINTENANCE.OVERRIDE_ONLINE;
    }

    for (MOTDGenerator generator : this.maintenanceMOTDGenerators) {
      generator.update(max, online);
    }

    for (MOTDGenerator generator : this.domainMaintenanceMOTD.values()) {
      generator.update(max, online);
    }
  }

  private int getOnline() {
    int online = this.server.getPlayerCount() + Settings.IMP.MAIN.FAKE_ONLINE_ADD_SINGLE;
    return online * (Settings.IMP.MAIN.FAKE_ONLINE_ADD_PERCENT + 100) / 100;
  }

  private int getMax(int online) {
    int max;
    MaxCountType type = Settings.IMP.MAIN.MAX_COUNT_TYPE;
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

  public ByteBuf getNext(LegacyMinecraftPingVersion version, String serverAddress) {
    if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
      return this.domainMaintenanceMOTD.getOrDefault(serverAddress, this.maintenanceMOTDGenerators.get(0)).getNext(version);
    } else {
      return this.domainMOTD.getOrDefault(serverAddress, this.motdGenerators.get(0)).getNext(version);
    }
  }

  public ByteBuf getNext(ProtocolVersion version, String serverAddress) {
    if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
      return this.domainMaintenanceMOTD.getOrDefault(serverAddress, this.maintenanceMOTDGenerators.get(
              this.maintenanceProtocolPointers.getOrDefault(version.getProtocol(), 0)))
              .getNext(version, !Settings.IMP.MAINTENANCE.SHOW_VERSION);
    } else {
      return this.domainMOTD.getOrDefault(serverAddress, this.motdGenerators.get(
              this.protocolPointers.getOrDefault(version.getProtocol(), 0)))
              .getNext(version, true);
    }
  }

  public ServerPing getNextCompat(ProtocolVersion version, String serverAddress) {
    if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
      return this.domainMaintenanceMOTD.getOrDefault(serverAddress, this.maintenanceMOTDGenerators.get(
              this.maintenanceProtocolPointers.getOrDefault(version.getProtocol(), 0)))
              .getNextCompat(version, !Settings.IMP.MAINTENANCE.SHOW_VERSION);
    } else {
      return this.domainMOTD.getOrDefault(serverAddress, this.motdGenerators.get(
              this.protocolPointers.getOrDefault(version.getProtocol(), 0)))
              .getNextCompat(version, true);
    }
  }

  public void inject(MinecraftConnection connection, ChannelPipeline pipeline) {
    this.loginPreparedPacketFactory.inject(DummyPlayer.INSTANCE, connection, pipeline);
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

  public File getConfigFile() {
    return this.configFile;
  }

  public enum MaxCountType {

    VARIABLE,
    ADD_SOME
  }
}
