# TemplatePlugin

[![Jitpack latest version](https://jitpack.io/v/fr.xpdustry/TemplatePlugin.svg)](https://jitpack.io/#fr.xpdustry/TemplatePlugin)
[![Build status](https://github.com/Xpdustry/TemplatePlugin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/TemplatePlugin/actions/workflows/build.yml)
[![Mindustry 5.0 | 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-5.0%20%7C%206.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)

## Description

Template stolen from **Anuken/ExamplePlugin** lol...

This template features some cool stuff such as:
- [Jitpack](https://jitpack.io/) support.
- Gradle tasks for testing:
  - `gradlew moveJar` Move the output jar to your server mod directory.
  - `gradlew runServer` Start the server in a new cmd.
  - `gradlew deployJar` Executes `moveJar` and `runServer`.
- GitHub action for easier release and Jitpack usage:
   - You just have to run the `Release` workflow manually,
     it will automatically take the plugin version in your plugin.json file and upload the jar.

## Tips and nice things to know

- When you use this template, make sure to edit `plugin.json` and `gradle.properties`.
  
- The plugin compiles to java 8 for maximum compatibility,
  but nothing keeps you to change the compiler target or source to a higher jdk.

Thank you for using this template !
