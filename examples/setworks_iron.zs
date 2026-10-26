#loader setworks

// Migration of a CTSetBonus "Iron" set to Setworks.
//
// The original: four iron armor pieces; a weapon set of iron (and, with Spartan Weaponry, steel)
// weapons; a visible 2-piece bonus and a visible 4-piece bonus with descriptions; a hidden bonus
// that needs the full armor set plus one weapon; and an EntityLivingHurt handler that reads the
// bonuses with hasSetBonus(...). Here the same shape, with Setworks names:
//   - Setworks.armorSet / weaponSet define the sets in one call each.
//   - A bonus without effects is a scripted bonus: Setworks applies nothing, the script reads it.
//   - The hidden gate is .hidden(): never shown in a tooltip, still queryable.
//   - The queries are on IPlayer: player.hasSetworksBonus(id), player.setworksPieceCount(id).
//   - A script function can be a condition (.when(...)): it runs on the server only.
// This file runs in the Setworks loader (the #loader line above), at load complete, after every mod
// registered its items. Copy it to scripts/ and change the ids and items.

import crafttweaker.player.IPlayer;
import crafttweaker.entity.IEntity;
import crafttweaker.entity.IEntityLivingBase;
import crafttweaker.damage.IDamageSource;
import crafttweaker.event.EntityLivingHurtEvent;
import mods.setworks.Setworks;

val armor as string = "shiba:iron_armor";
val weapons as string = "shiba:iron_weapons";
val partialBonus as string = "shiba:iron_partial";
val fullBonus as string = "shiba:iron_full";
val weaponGate as string = "shiba:iron_weapon_gate";

val fullHealthPercentDamageBonus as double = 0.75;
val weaponPercentDamageBonus as double = 0.75;

// ---- definitions ----

Setworks.armorSet(armor, "Iron Armor",
    <minecraft:iron_helmet>, <minecraft:iron_chestplate>, <minecraft:iron_leggings>, <minecraft:iron_boots>);

// Add the steel weapons of your pack to this list, for example <spartanweaponry:longsword_steel>.
// An item that does not exist disables the whole set at startup, so list only what the pack has.
Setworks.weaponSet(weapons, "Iron Weapons", [<minecraft:iron_sword>, <minecraft:iron_axe>]);

// Scripted bonuses (no effects): the descriptions are what the tooltip shows.
Setworks.bonus(partialBonus, armor, 2,
    "Your attacks deal " + ((fullHealthPercentDamageBonus * 100.0) as int) + "% bonus damage to enemies at full health");
Setworks.fullSetBonus(fullBonus, armor,
    "Iron or steel melee attacks deal " + ((weaponPercentDamageBonus * 100.0) as int) + "% bonus damage");

// The hidden gate: full armor plus one weapon in the main hand. Not shown anywhere; the handler asks for it.
Setworks.bonus(weaponGate)
    .requiresAll(armor)
    .requires(weapons, 1)
    .description("Iron weapon gate")
    .hidden();

// A built-in effect with a script function as its condition: +1 armor while the wearer is at full health.
Setworks.bonus("shiba:iron_steady", armor, 2, "Iron Steady: +1 armor while at full health")
    .when(function(player as IPlayer) as bool {
        return player.health >= player.maxHealth;
    })
    .attribute("generic.armor", 1, "add");

// ---- behavior ----

events.onEntityLivingHurt(function(event as EntityLivingHurtEvent) {
    val source as IDamageSource = event.damageSource;
    val attackerEntity as IEntity = source.getTrueSource();
    val target as IEntityLivingBase = event.entityLivingBase;
    if (isNull(attackerEntity) || isNull(target) || !(attackerEntity instanceof IPlayer)) {
        return;
    }
    // An IEntity that is a player can be assigned to an IPlayer variable after the instanceof check.
    val attacker as IPlayer = attackerEntity;
    // Partial set: bonus damage against a target at full health.
    if (attacker.hasSetworksBonus(partialBonus) && target.health >= target.maxHealth) {
        event.amount = event.amount * (1.0 + fullHealthPercentDamageBonus);
        print("Setworks iron: full-health bonus applied by " + attacker.name);
    }
    // Full set plus weapon: bonus damage on melee hits.
    if (attacker.hasSetworksBonus(weaponGate) && !source.isProjectile()) {
        event.amount = event.amount * (1.0 + weaponPercentDamageBonus);
        print("Setworks iron: weapon bonus applied by " + attacker.name);
    }
});
