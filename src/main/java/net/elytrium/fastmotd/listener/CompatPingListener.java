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
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import net.elytrium.fastmotd.FastMOTD;

public class CompatPingListener {

  private final FastMOTD plugin;

  public CompatPingListener(FastMOTD plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void onPing(ProxyPingEvent event) {
    InboundConnection connection = event.getConnection();
    event.setPing(this.plugin.getNextCompat(
        connection.getProtocolVersion(),
        connection.getVirtualHost()
            .map(address -> address.getHostName() + ":" + address.getPort())
            .orElse(null)));
  }
}
