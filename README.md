<img src="https://elytrium.net/src/img/elytrium.webp" alt="Elytrium" align="right">

# FastMOTD

[![Join our Discord](https://img.shields.io/discord/775778822334709780.svg?logo=discord&label=Discord)](https://ely.su/discord)
[![Proxy Stats](https://img.shields.io/bstats/servers/15640?logo=minecraft&label=Servers)](https://bstats.org/plugin/velocity/FastMOTD/15640)
[![Proxy Stats](https://img.shields.io/bstats/players/15640?logo=minecraft&label=Players)](https://bstats.org/plugin/velocity/FastMOTD/15640)

A MOTD plugin for Velocity that caches network packets. This helps it be the fastest one of the MOTD plugins. <br>
Test server: [``ely.su``](https://hotmc.ru/minecraft-server-203216)

## Features

- Fake online (percent + static sum)
- Multiple descriptions/favicons support
- Set information (custom text in the player list)
- Caching of network packets
- Max count "just add up" support
- PNG built-in compression

## Comparison with other MOTD plugins

Intel Core i9-9700K, DDR4 (a server that is not running any programs):

| Plugin               | Pings per second count                 |
|----------------------|----------------------------------------|
| FastMOTD             | 1 700 000 - 2 000 000 pings per second |
| Without MOTD plugins | 900 000 - 1 100 000 pings per second   |
| MiniMOTD             | 480 000 - 580 000 pings per second     |

Intel Xeon E3-1270, DDR3 (a PC with several applications running):
| Plugin | Pings per second count |
| - | - |
| FastMOTD | 840 000 - 1 000 000 pings per second |
| Without MOTD plugins | 330 000 - 430 000 pings per second |
| MiniMOTD | 150 000 - 200 000 pings per second |

## Commands and permissions

### Admin

- ***fastmotd.info* | /fastmotd info** - The command to get general information about the current state of the plugin
- ***fastmotd.reload* | /fastmotd reload** - Reload Plugin Command
- ***fastmotd.maintenance* | /maintenance** - Maintenance Mode Setting Command