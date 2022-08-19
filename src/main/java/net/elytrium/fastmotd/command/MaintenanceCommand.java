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

package net.elytrium.fastmotd.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import java.util.List;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import net.kyori.adventure.text.Component;

public class MaintenanceCommand implements SimpleCommand {

  private final FastMOTD plugin;
  private final Component usage;

  public MaintenanceCommand(FastMOTD plugin, Component usage) {
    this.plugin = plugin;
    this.usage = usage;
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    return ImmutableList.of("off", "on", "toggle");
  }

  @Override
  public void execute(Invocation invocation) {
    String[] args = invocation.arguments();
    CommandSource source = invocation.source();

    if (args.length < 1) {
      source.sendMessage(this.usage);
      return;
    }

    switch (args[0]) {
      case "off":
        Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED = false;
        break;

      case "on":
        Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED = true;
        break;

      case "toggle":
        Settings.IMP.MAINTENANCE.MAINTENANCE_ENABLED ^= true;
        break;

      default:
        source.sendMessage(this.usage);
        return;
    }

    Settings.IMP.save(this.plugin.getConfigFile());
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().getPermissionValue("fastmotd.maintenance") == Tristate.TRUE;
  }
}
