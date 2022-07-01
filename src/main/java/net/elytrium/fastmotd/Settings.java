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

import java.util.List;
import net.elytrium.java.commons.config.YamlConfig;

public class Settings extends YamlConfig {

  @Ignore
  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = BuildConstants.VERSION;

  @Comment({
      "Available serializers:",
      "LEGACY_AMPERSAND - \"&c&lExample &c&9Text\".",
      "LEGACY_SECTION - \"§c§lExample §c§9Text\".",
      "MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)",
      "GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)",
      "GSON_COLOR_DOWNSAMPLING - Same as GSON, but uses downsampling."
  })
  public String SERIALIZER = "MINIMESSAGE";

  @Create
  public MAIN MAIN;

  public static class MAIN {
    public String VERSION_NAME = "Elytrium";
    public List<String> DESCRIPTIONS = List.of("<bold><red>FastMOTD</red></bold>{NL} -> Really fast.");
    public List<String> FAVICONS = List.of("server-icon.png");
    public List<String> INFORMATION = List.of("This is the", "<bold>best server</bold>", "<gradient:green:red>made ever</gradient>", "trust me");
    @Comment("How frequently online player count will be updated (in ms)")
    public long UPDATE_RATE = 3000;
    @Comment({
        "VARIABLE - from max-count parameter",
        "ADD_SOME - will add up the number from max-count parameter to current online players amount"
    })
    public String MAX_COUNT_TYPE = "VARIABLE";
    public int MAX_COUNT = 4444;
    public int FAKE_ONLINE_ADD_SINGLE = 5;
    public int FAKE_ONLINE_ADD_PERCENT = 20;
    @Comment({
        "Accepted values: from 0.0 to 1.0.",
        "Keep this value as low as possible"
    })
    public double PNG_QUALITY = 0.0;
    public boolean LOG_PINGS = false;
  }
}
