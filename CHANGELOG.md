# Changelog

All notable changes to this project are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-10-26

First release. Minecraft 1.12.2, Forge; runs on Cleanroom. Required on the server and on every client.

### Added

- Sets of named pieces, each accepting items by id, metadata, NBT rules, or ore dictionary name, at armor, hand, hotbar, inventory, single-slot, Baubles, or addon locations.
- Bonuses at a piece count of one or more sets, with built-in attribute modifier and potion effects, built-in conditions (`setworks:health_below`, `setworks:sneaking`), per-bonus condition intervals, and the visibility modes visible, discovered, and hidden; discovery is saved per player.
- Cumulative and highest-only threshold modes per set.
- Tooltips: one block per set an item belongs to, with the worn count, the bonus lines colored green once the pieces are on, and the piece list while Shift is held.
- ZenScript surface for CraftTweaker 4.1.20 or later: `mods.setworks.Setworks` with helpers, handles, script-function conditions, player and item queries, and bonus events; the `setworks` script loader; replace, disable, and modify of definitions from other sources; parse-error scope reported by `/setworks validate`.
- Java API (`com.mahghuuuls.setworks.api`) for sets, bonuses, addon effect kinds, condition kinds, and locations, with queries, listeners, and Forge bus events.
- Baubles 1.5.2 or later: the six Baubles slots as locations.
- Operator commands `/setworks list`, `/setworks validate`, `/setworks inspect <player>`.
- Configuration: strict validation, condition interval, example sets, diagnostic log, client tooltip switch.
- Example scripts under `examples/`, including the migration of a CTSetBonus-style set.

### Known Issues

- A player's own potion of the same type as a set potion is topped up to the set's level and duration while the bonus is active; its own longer duration is kept.
