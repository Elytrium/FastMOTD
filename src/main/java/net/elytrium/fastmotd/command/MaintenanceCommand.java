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

package net.elytrium.fastmotd.command;

import com.google.common.base.Joiner;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.stream.Collectors;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import net.elytrium.serializer.placeholders.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public class MaintenanceCommand {
  /**
   * This method creates a {@link com.velocitypowered.api.command.BrigadierCommand} for the maintenance command.
   *
   * @param plugin The FastMOTD plugin instance.
   * @param serializer The component serializer.
   * @return The maintenance command.
  */
  public static BrigadierCommand createBrigadierCommand(final FastMOTD plugin, final ComponentSerializer<Component, Component, String> serializer) {
    final Component usageComponent = serializer.deserialize(String.join("\n", Settings.IMP.MAINTENANCE.MESSAGES.USAGE));
    final Component maintenanceOnComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.ON);
    final Component maintenanceOffComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.OFF);
    final String listString = Settings.IMP.MAINTENANCE.MESSAGES.LIST;
    final String listPlayerFormat = Settings.IMP.MAINTENANCE.MESSAGES.LIST_PLAYER_FORMAT;
    final Component successfullyAddComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.SUCCESSFULLY_ADDED);
    final Component successfullyRemoveComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.SUCCESSFULLY_REMOVED);
    final Component invalidInputComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.INVALID_INPUT);
    final Component alreadyInComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.ALREADY_IN);
    final Component notInWhitelistComponent = serializer.deserialize(Settings.IMP.MAINTENANCE.MESSAGES.NOT_IN_WHITELIST);

    LiteralCommandNode<CommandSource> maintenanceNode = BrigadierCommand.literalArgumentBuilder("maintenance")
        .requires(source -> source.hasPermission("fastmotd.maintenance"))
        .executes(context -> {
          context.getSource().sendMessage(usageComponent);
          return Command.SINGLE_SUCCESS;
        })
        // off subcommand
        .then(BrigadierCommand.literalArgumentBuilder("off")
          .executes(context -> {
            Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED = false;
            Settings.IMP.save(plugin.getConfigPath());
            context.getSource().sendMessage(maintenanceOffComponent);
            return Command.SINGLE_SUCCESS;
          })
        )
        // on subcommand
        .then(BrigadierCommand.literalArgumentBuilder("on")
          .executes(context -> {
            Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED = true;
            Settings.IMP.save(plugin.getConfigPath());
            plugin.kickNotWhitelisted();
            context.getSource().sendMessage(maintenanceOnComponent);
            return Command.SINGLE_SUCCESS;
          })
        )
        // toggle subcommand
        .then(BrigadierCommand.literalArgumentBuilder("toggle")
          .executes(context -> {
            Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED ^= true;
            Settings.IMP.save(plugin.getConfigPath());
            if (Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
              plugin.kickNotWhitelisted();
              context.getSource().sendMessage(maintenanceOnComponent);
            } else {
              context.getSource().sendMessage(maintenanceOffComponent);
            }
            return Command.SINGLE_SUCCESS;
          })
        )
        // list subcommand
        .then(BrigadierCommand.literalArgumentBuilder("list")
          .then(BrigadierCommand.literalArgumentBuilder("-p")
            .executes(context -> {
              context.getSource().sendMessage(serializer.deserialize(Placeholders.replace(
                      listString,
                      Joiner.on(", ").join(plugin.getServer().getAllPlayers().stream()
                              .filter(player -> plugin.checkKickWhitelist(player.getRemoteAddress().getAddress()))
                              .map(player -> listPlayerFormat
                                      .replace("{PLAYER}", player.getUsername())
                                      .replace("{IP}", player.getRemoteAddress().getAddress().getHostAddress()))
                              .collect(Collectors.toSet()))
                      )));
              return Command.SINGLE_SUCCESS;
            })
          )
          .executes(context -> {
            context.getSource().sendMessage(serializer.deserialize(Placeholders.replace(
                    listString,
                    Joiner.on(", ").join(plugin.getKickWhitelist().stream()
                            .map(InetAddress::getHostAddress)
                            .collect(Collectors.toList()))
            )));
            return Command.SINGLE_SUCCESS;
          })
        )
        // add subcommand
        .then(BrigadierCommand.literalArgumentBuilder("add")
          .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.string())
            .suggests((ctx, builder) -> {
              plugin.getServer().getAllPlayers().stream()
                        .filter(player -> !plugin.checkKickWhitelist(player.getRemoteAddress().getAddress()))
                        .forEach(player -> {
                          builder.suggest(player.getUsername());
                          builder.suggest(player.getRemoteAddress().getAddress().getHostAddress());
                        });
              return builder.buildFuture();
            })
            .executes(context -> {
              final CommandSource source = context.getSource();
              final InetAddress address = getInetAddressFromString(plugin, context.getArgument("player", String.class));
              if (address == null) {
                source.sendMessage(invalidInputComponent);
                return Command.SINGLE_SUCCESS;
              }
              if (plugin.checkKickWhitelist(address)) {
                source.sendMessage(alreadyInComponent);
                return Command.SINGLE_SUCCESS;
              }
              plugin.getKickWhitelist().add(address);
              Settings.IMP.MAINTENANCE.KICK_WHITELIST.add(address.getHostAddress());
              Settings.IMP.save(plugin.getConfigPath());
              source.sendMessage(successfullyAddComponent);
              return Command.SINGLE_SUCCESS;
            })
          )
          .executes(context -> {
            context.getSource().sendMessage(usageComponent);
            return Command.SINGLE_SUCCESS;
          })
        )
        // remove subcommand
        .then(BrigadierCommand.literalArgumentBuilder("remove")
          .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.string())
            .suggests((ctx, builder) -> {
              plugin.getServer().getAllPlayers().stream()
                      .filter(player -> plugin.checkKickWhitelist(player.getRemoteAddress().getAddress()))
                      .forEach(player -> {
                        builder.suggest(player.getUsername());
                      });
              plugin.getKickWhitelist().forEach(address -> builder.suggest(address.getHostAddress()));
              return builder.buildFuture();
            })
            .executes(context -> {
              final CommandSource source = context.getSource();
              final InetAddress address = getInetAddressFromString(plugin, context.getArgument("player", String.class));
              if (address == null) {
                source.sendMessage(invalidInputComponent);
                return Command.SINGLE_SUCCESS;
              }
              if (!plugin.checkKickWhitelist(address)) {
                source.sendMessage(notInWhitelistComponent);
                return Command.SINGLE_SUCCESS;
              }
              plugin.getKickWhitelist().remove(address);
              Settings.IMP.MAINTENANCE.KICK_WHITELIST.remove(address.getHostAddress());
              Settings.IMP.save(plugin.getConfigPath());
              source.sendMessage(successfullyRemoveComponent);
              return Command.SINGLE_SUCCESS;
            })
          )
          .executes(context -> {
            context.getSource().sendMessage(usageComponent);
            return Command.SINGLE_SUCCESS;
          })
        )
        .build();
    return new BrigadierCommand(maintenanceNode);
  }

  /**
   * Retrieves the InetAddress from a given string.
   *
   * @param plugin The FastMOTD plugin instance.
   * @param str The string with the player's name or IP address.
   * @return The InetAddress object corresponding to the given string, or null if the string cannot be resolved.
  */
  private static InetAddress getInetAddressFromString(final FastMOTD plugin, final String str) {
    Optional<Player> playerOptional = plugin.getServer().getPlayer(str);
    if (playerOptional.isPresent()) {
      return playerOptional.get().getRemoteAddress().getAddress();
    } else {
      try {
        return InetAddress.getByName(str);
      } catch (UnknownHostException e) {
        return null;
      }
    }
  }
}
