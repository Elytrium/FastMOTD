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
import java.util.Map;
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
  public String PREFIX = "FastMOTD <gold>>></gold>";

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

    @Create
    public VERSIONS VERSIONS;

    @Comment({
        "Separate MOTDs/favicons/information for different protocol versions",
        "See https://wiki.vg/Protocol_version_numbers",
    })
    public static class VERSIONS {
      @Comment("null = disabled")
      public Map<String, List<String>> DESCRIPTIONS = Map.of("757-759", List.of("<bold><red>FastMOTD</red></bold>{NL} -> Supports separate MOTDs for different versions."));
      @Comment("null = disabled")
      public Map<String, List<String>> FAVICONS = Map.of("756-758", List.of("second-server-icon.png"));
      @Comment("null = disabled")
      public Map<String, List<String>> INFORMATION = Map.of("757-758", List.of("Your", "protocol", "version", "is 757 or 758"));
    }
  }

  @Create
  public MAINTENANCE MAINTENANCE;

  public static class MAINTENANCE {
    public boolean MAINTENANCE_ENABLED = false;
    public boolean SHOW_VERSION = true;
    public boolean SHOULD_KICK_ON_JOIN = true;
    public List<String> KICK_WHITELIST = List.of("127.0.0.1");
    public String KICK_MESSAGE = "<red>Try to join the server later</red>";
    public String VERSION_NAME = "MAINTENANCE MODE ENABLED!!";
    public List<String> DESCRIPTIONS = List.of("<bold><red>FastMOTD</red></bold>{NL} -> Really fast. (in maintenance mode too)");
    public List<String> FAVICONS = List.of("server-icon.png");
    public List<String> INFORMATION = List.of("Contact support: https://elytrium.net/discord");
    @Comment("-1 = disabled")
    public int OVERRIDE_ONLINE = -1;
    @Comment("-1 = disabled")
    public int OVERRIDE_MAX_ONLINE = -1;

    @Create
    public VERSIONS VERSIONS;

    @Comment({
        "Separate MOTDs/favicons/information for different protocol versions",
        "See https://wiki.vg/Protocol_version_numbers",
    })
    public static class VERSIONS {
      @Comment("null = disabled")
      public Map<String, List<String>> DESCRIPTIONS = Map.of("757-759", List.of("<bold><red>FastMOTD</red></bold>{NL} -> Really Fast."));
      @Comment("null = disabled")
      public Map<String, List<String>> FAVICONS = Map.of("758-758", List.of("second-server-icon.png"));
      @Comment("null = disabled")
      public Map<String, List<String>> INFORMATION = Map.of("756-759", List.of("Server is", "under", "maintenance"));
    }

    @Create
    public COMMAND COMMAND;

    public static class COMMAND {
      public String USAGE = "{PRFX} Usage: <gold>/maintenance <off|on|toggle></gold>";
    }
  }
}
