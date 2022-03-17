# TemplatePlugin

[![Build status](https://github.com/Xpdustry/TemplatePlugin/actions/workflows/commit.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/TemplatePlugin/actions/workflows/commit.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/snapshots/fr/xpdustry/template-plugin?color=00FFFF&name=TemplatePlugin&prefix=v)](https://github.com/Xpdustry/TemplatePlugin/releases)

## Description

This plugin is a special game mode based "capture the zone" like games like TF2 or Robocraft.

The rules are really simple, be the team with the most captured zones or the highest percent of captured zones to win.

To capture a zone, just stand within its radius. Be sure to enable the effects to see it...

If more than 2 teams are even when the time is up, the showdown is triggered, which means the other teams are killed.

Here is the command tree :

- `domination` :

  - `start <map>` : Start a new game of Domination on the specified map, can work during an already running game and in the console. 

  - `edit` : Enable/Disable the edit mode, where each click can make a zone appear or disappear, works even in a non-domination game.

  - `settings` : The settings for the current map, works even in a non-domination game.

    - `zone-radius [zone-radius]` : Edit/See the zone radius. The default value is `5` blocks.

    - `capture-rate [capture-rate]` : Edit/See the capture rate, the number of percent captured per second. The default value is `5.0` percent.

    - `game-duration [game-duration]` : Edit/See the game duration. The default value is `30.0` minutes.

    - `showdown-duration [showdown-duration]` : Edit/See the showdown duration. The default value is `5.0` minutes.

The map settings are saved in the json format in the created `./distributor/plugins/xpdustry-domination-plugin` directory.

## Building

- `./gradlew jar` for a simple jar that contains only the plugin.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

[distributor-core](https://github.com/Xpdustry/Distributor) is required as a dependency.

If you run on V6 or V7 up to v135, you will need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin).
