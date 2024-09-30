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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public class FastmotdCommand {
  /**
   * This method creates a {@link com.velocitypowered.api.command.BrigadierCommand} for the fastmotd command.
   *
   * @param plugin The FastMOTD plugin instance.
   * @param serializer The component serializer.
   * @return The maintenance command.
  */
  public static BrigadierCommand createBrigadierCommand(final FastMOTD plugin, final ComponentSerializer<Component, Component, String> serializer) {
    final Component usageComponent = serializer.deserialize(String.join("\n", Settings.IMP.MAIN.MESSAGES.USAGE));
    final Component reloadComponent = serializer.deserialize(Settings.IMP.MAIN.MESSAGES.RELOAD);
    final String infoString = String.join("\n", Settings.IMP.MAIN.MESSAGES.INFO);

    LiteralCommandNode<CommandSource> fastmotdNode = BrigadierCommand.literalArgumentBuilder("fastmotd")
        .requires(source -> source.hasPermission("fastmotd.reload") || source.hasPermission("fastmotd.info"))
        .executes(context -> {
          context.getSource().sendMessage(usageComponent);
          return Command.SINGLE_SUCCESS;
        })
        // info subcommand
        .then(BrigadierCommand.literalArgumentBuilder("info")
          .requires(source -> source.hasPermission("fastmotd.info"))
          .executes(context -> {
            context.getSource().sendMessage(serializer.deserialize(infoString
                            .replace(
                                    "{MAINTENANCE_ENABLED}",
                                    Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED ? Settings.IMP.MAIN.MESSAGES.YES : Settings.IMP.MAIN.MESSAGES.NO
                            )
                    ));
            return Command.SINGLE_SUCCESS;
          })
        )
        // reload subcommand
        .then(BrigadierCommand.literalArgumentBuilder("reload")
          .requires(source -> source.hasPermission("fastmotd.reload"))
          .executes(context -> {
            plugin.reload();
            context.getSource().sendMessage(reloadComponent);
            return Command.SINGLE_SUCCESS;
          })
        )
        .build();
    return new BrigadierCommand(fastmotdNode);
  }
}
