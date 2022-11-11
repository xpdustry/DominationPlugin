# DominationPlugin

[![Build status](https://github.com/Xpdustry/DominationPlugin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/DominationPlugin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/releases/fr/xpdustry/domination-plugin?color=00FFFF&name=DominationPlugin&prefix=v)](https://github.com/Xpdustry/DominationPlugin/releases)

## Description

This plugin is a special game mode based "capture the zone" like games like TF2 or Robocraft.

The rules are really simple, the team that captures all the zones win.

To capture a zone, just send units within its radius. Be sure to enable the effects to see it...

## Building

- `./gradlew jar` for a simple jar that contains only the plugin.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

[distributor-core](https://github.com/Xpdustry/Distributor) is required as a dependency.

If you run on V6 or V7 up to v135, you will need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin).
