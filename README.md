# Setworks

Setworks lets modpack authors and mods define equipment sets with partial and full set bonuses, through ZenScript or a Java API. It ships no sets of its own and shows set membership and active bonuses in item tooltips.

Minecraft 1.12.2, Forge. Runs on Cleanroom.

This repository is in initial development. Documentation is written with the first release.

## Building

Gradle wrapper, Java 25 for the build, Java 8 bytecode output:

```text
./gradlew build
```

## License

MIT. See `LICENSE`. Build scripts derive from CleanroomMC ForgeDevEnv; see `THIRD-PARTY-NOTICES.md`.
