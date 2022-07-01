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

package net.elytrium.fastmotd.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.Favicon;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import net.elytrium.fastmotd.holder.MOTDHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public class MOTDGenerator {

  private final FastMOTD plugin;
  private final ComponentSerializer<Component, Component, String> serializer;
  private final int holdersAmount = Settings.IMP.MAIN.DESCRIPTIONS.size() * Settings.IMP.MAIN.FAVICONS.size();
  private final MOTDHolder[] holders = new MOTDHolder[this.holdersAmount];

  public MOTDGenerator(FastMOTD plugin, ComponentSerializer<Component, Component, String> serializer) {
    this.plugin = plugin;
    this.serializer = serializer;
  }

  public void generate() {
    List<String> descriptions = Settings.IMP.MAIN.DESCRIPTIONS;
    for (int i = 0, descriptionsSize = descriptions.size(); i < descriptionsSize; i++) {
      String description = descriptions.get(i);
      List<String> favicons = Settings.IMP.MAIN.FAVICONS;
      for (int j = 0, faviconsSize = favicons.size(); j < faviconsSize; j++) {
        String faviconLocation = favicons.get(j);
        try {
          String base64Favicon = Favicon.create(Path.of(faviconLocation)).getBase64Url();
          this.holders[j * descriptionsSize + i] = new MOTDHolder(this.serializer, description, base64Favicon);
        } catch (IOException e) {
          this.plugin.getLogger().error("Failed to load favicon " + faviconLocation);
          e.printStackTrace();
        }
      }
    }
  }

  public void update() {
    int online = this.plugin.getServer().getPlayerCount() + Settings.IMP.MAIN.FAKE_ONLINE_ADD_SINGLE;
    online = online * (Settings.IMP.MAIN.FAKE_ONLINE_ADD_PERCENT + 100) / 100;

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

    for (MOTDHolder holder : this.holders) {
      holder.replaceOnline(max, online);
    }
  }

  public ByteBuf getNext(ProtocolVersion version) {
    return this.holders[ThreadLocalRandom.current().nextInt(this.holdersAmount)].getByteBuf(version);
  }

  public void dispose() {
    for (MOTDHolder holder : this.holders) {
      holder.dispose();
    }
  }

  private enum MaxCountType {

    VARIABLE,
    ADD_SOME
  }
}
