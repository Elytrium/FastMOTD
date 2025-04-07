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

package net.elytrium.fastmotd.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import net.elytrium.fastmotd.FastMOTD;
import net.kyori.adventure.text.Component;

public class DisconnectOnZeroPlayersListener {

  private final FastMOTD plugin;

  public DisconnectOnZeroPlayersListener(FastMOTD plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    if (this.plugin.getServer().getAllPlayers().size() == 0) {
      this.plugin.getServer().shutdown(Component.text("FastMOTD -> shutdown on zero players"));
    }
  }
}
