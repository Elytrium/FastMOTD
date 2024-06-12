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

import java.util.List;
import java.util.Map;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.Final;
import net.elytrium.serializer.language.object.YamlSerializable;

public class Settings extends YamlSerializable {

  public static final Settings IMP = new Settings();

  @Final
  public String VERSION = BuildConstants.VERSION;

  @Comment({
      @CommentValue("Available serializers:"),
      @CommentValue("LEGACY_AMPERSAND - \"&c&lExample &c&9Text\"."),
      @CommentValue("LEGACY_SECTION - \"§c§lExample §c§9Text\"."),
      @CommentValue("MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\". (https://webui.adventure.kyori.net/)"),
      @CommentValue("GSON - \"[{\"text\":\"Example\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \",\"bold\":true},{\"text\":\"Text\",\"bold\":true,\"color\":\"blue\"}]\". (https://minecraft.tools/en/json_text.php/)"),
      @CommentValue("GSON_COLOR_DOWNSAMPLING - Same as GSON, but uses downsampling."),
  })
  public Serializers SERIALIZER = Serializers.MINIMESSAGE;

  public MAIN MAIN = new MAIN();

  public static class MAIN {
    public boolean ENABLE_UPDATES = true;
    public String VERSION_NAME = "Elytrium";
    public List<String> DESCRIPTIONS = List.of("<bold><red>FastMOTD</red></bold>{NL} -> Really fast.");
    public List<String> FAVICONS = List.of("server-icon.png");
    public List<String> INFORMATION = List.of("This is the", "<bold>best server</bold>", "<gradient:green:red>made ever</gradient>", "trust me");
    @Comment(@CommentValue("How frequently online player count will be updated (in ms)"))
    public long UPDATE_RATE = 3000;
    @Comment({
        @CommentValue("VARIABLE - from max-count parameter"),
        @CommentValue("ADD_SOME - will add up the number from max-count parameter to current online players amount")
    })
    public FastMOTD.MaxCountType MAX_COUNT_TYPE = FastMOTD.MaxCountType.VARIABLE;
    public int MAX_COUNT = 4444;
    public int FAKE_ONLINE_ADD_SINGLE = 5;
    public int FAKE_ONLINE_ADD_PERCENT = 20;
    @Comment({
        @CommentValue("Accepted values: from 0.0 to 1.0."),
        @CommentValue("Keep this value as low as possible"),
        @CommentValue("Set -1 to disable PNG recompression")
    })
    public double PNG_QUALITY = 0.0;
    public boolean LOG_PINGS = false;
    public boolean LOG_IMPROPER_PINGS = false;
    @Comment({
        @CommentValue("Enabling this will allow non-vanilla ping packets sequence,"),
        @CommentValue("but will open your server to nullping attacks")
    })
    public boolean ALLOW_IMPROPER_PINGS = false;

    public VERSIONS VERSIONS = new VERSIONS();

    @Comment({
        @CommentValue("Separate MOTDs/favicons/information for different protocol versions"),
        @CommentValue("See https://wiki.vg/Protocol_version_numbers"),
    })
    public static class VERSIONS {
      @Comment(@CommentValue("{} = disabled"))
      public Map<String, List<String>> DESCRIPTIONS = Map.of("757-759", List.of("<bold><red>FastMOTD</red></bold>{NL} -> Supports separate MOTDs for different versions."));
      @Comment(@CommentValue("{} = disabled"))
      public Map<String, List<String>> FAVICONS = Map.of("756-758", List.of("second-server-icon.png"));
      @Comment(@CommentValue("{} = disabled"))
      public Map<String, List<String>> INFORMATION = Map.of("757-758", List.of("Your", "protocol", "version", "is 757 or 758"));
    }

    public Map<String, DOMAIN_MOTD_NODE> DOMAINS = Map.of("example.com:25565", new DOMAIN_MOTD_NODE());
  }

  public MAINTENANCE MAINTENANCE = new MAINTENANCE();

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
    @Comment(@CommentValue("-1 = disabled"))
    public int OVERRIDE_ONLINE = -1;
    @Comment(@CommentValue("-1 = disabled"))
    public int OVERRIDE_MAX_ONLINE = -1;

    public VERSIONS VERSIONS = new VERSIONS();

    @Comment({
        @CommentValue("Separate MOTDs/favicons/information for different protocol versions"),
        @CommentValue("See https://wiki.vg/Protocol_version_numbers"),
    })
    public static class VERSIONS {
      @Comment(@CommentValue("{} = disabled"))
      public Map<String, List<String>> DESCRIPTIONS = Map.of("757-759", List.of("<bold><red>FastMOTD</red></bold>{NL} -> Really Fast."));
      @Comment(@CommentValue("{} = disabled"))
      public Map<String, List<String>> FAVICONS = Map.of("758-758", List.of("second-server-icon.png"));
      @Comment(@CommentValue("{} = disabled"))
      public Map<String, List<String>> INFORMATION = Map.of("756-759", List.of("Server is", "under", "maintenance"));
    }

    public Map<String, DOMAIN_MOTD_NODE> DOMAINS = Map.of("example.com:25565", new DOMAIN_MOTD_NODE());

    public COMMAND COMMAND = new COMMAND();

    public static class COMMAND {
      public String USAGE = "FastMOTD <gold>>></gold> Usage: <gold>/maintenance <off|on|toggle></gold>";
    }
  }

  public SHUTDOWN_SCHEDULER SHUTDOWN_SCHEDULER = new SHUTDOWN_SCHEDULER();

  public static class SHUTDOWN_SCHEDULER {
    @Comment(@CommentValue("Server will stop accepting new connections"))
    public boolean SHUTDOWN_SCHEDULER_ENABLED = false;
    @Comment(@CommentValue("Server will shut down after everyone has left the server"))
    public boolean SHUTDOWN_ON_ZERO_PLAYERS = false;
    public List<String> WHITELIST = List.of("127.0.0.1");
  }

  public static class DOMAIN_MOTD_NODE {

    public List<String> DESCRIPTION = List.of("Description for example.com");
    public List<String> FAVICON = List.of("example-com-server-icon.png");
    public List<String> INFORMATION = List.of("Information for example.com");
  }
}
