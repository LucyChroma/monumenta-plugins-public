package com.playmonumenta.plugins.abilities.meleescout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.MeleeScout;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

public class AdrenalineRush extends Ability {

	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_SKILL,
		DamageEvent.DamageType.MELEE_ENCH
	);
	private static final double DAMAGE_BOOST = 0.2;
	private static final double SPEED_BOOST_ON_HIT = 0.2;
	private static final double SPEED_BOOST_ON_SPAWNER_BREAK = 0.1;
	private static final int ON_HIT_DURATION = 3 * 20;
	private static final int ON_SPAWNER_BREAK_DURATION = 6 * 20;
	private static final double LEVEL_TWO_COOLDOWN_REFRESH = 0.05;
	public static final String PERCENT_DAMAGE_EFFECT_NAME = "AdrenalineRushPercentDamageEffect";
	public static final String PERCENT_SPEED_EFFECT_NAME = "AdrenalineRushPercentSpeedEffect";

	public static final String CHARM_DAMAGE_BOOST = "Adrenaline Rush Damage Amplifier";
	public static final String CHARM_SPEED_BOOST_ON_HIT = "Adrenaline Rush Speed Amplifier On Hit";
	public static final String CHARM_SPEED_BOOST_ON_SPAWNER_BREAK = "Adrenaline Rush Speed Amplifier On Spawner Break";
	public static final String CHARM_DURATION = "Adrenaline Rush Effect Duration";
	public static final String CHARM_LEVEL_TWO_COOLDOWN_REFRESH = "Adrenaline Rush Cooldown Reduction";

	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private final double mDamageBoost;
	private final double mSpeedBoostOnHit;
	private final double mSpeedBoostOnSpawnerBreak;
    private final int mDuration;
    private final int mDurationSpawner;
    private final double mLevelTwoCooldownRefresh;

	public static final AbilityInfo<AdrenalineRush> INFO =
		new AbilityInfo<>(AdrenalineRush.class, "Adrenaline Rush", AdrenalineRush::new)
			.linkedSpell(ClassAbility.ADRENALINE_RUSH)
			.descriptions(
				String.format("Attacking an enemy with a fully charged valid melee weapon attack " +
                                "gives you %s%% Speed and +%s%% melee damage for %s seconds. Breaking a spawner " +
                                "gives you %s%% Speed for %s seconds.",
						StringUtils.multiplierToPercentage(SPEED_BOOST_ON_HIT),
						StringUtils.multiplierToPercentage(DAMAGE_BOOST),
						StringUtils.ticksToSeconds(ON_HIT_DURATION),
						StringUtils.multiplierToPercentage(SPEED_BOOST_ON_SPAWNER_BREAK),
						StringUtils.ticksToSeconds(ON_SPAWNER_BREAK_DURATION)
                ),
				String.format("Breaking a spawner reduces the cooldowns of your abilities by %s%%.",
                        StringUtils.multiplierToPercentage(LEVEL_TWO_COOLDOWN_REFRESH)
                ),
				"Double the effect and length of the Adrenaline enchantment."
            )
			.scoreboardId("AdrenalineRush")
			.simpleDescription("Gain speed by fighting and breaking spawners.")
			.displayItem(Material.SUGAR);

	public AdrenalineRush(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBoost = DAMAGE_BOOST + CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE_BOOST);
		mSpeedBoostOnHit = SPEED_BOOST_ON_HIT + CharmManager.getLevelPercentDecimal(player, CHARM_SPEED_BOOST_ON_HIT);
		mSpeedBoostOnSpawnerBreak = SPEED_BOOST_ON_SPAWNER_BREAK + CharmManager.getLevelPercentDecimal(player, CHARM_SPEED_BOOST_ON_SPAWNER_BREAK);
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, ON_HIT_DURATION);
        mDurationSpawner = CharmManager.getDuration(player, CHARM_DURATION, ON_SPAWNER_BREAK_DURATION);
		mLevelTwoCooldownRefresh = LEVEL_TWO_COOLDOWN_REFRESH + CharmManager.getLevelPercentDecimal(player, CHARM_LEVEL_TWO_COOLDOWN_REFRESH);
	}

	public boolean cast() {
		return false;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		if (event.getType() == DamageEvent.DamageType.MELEE && mPlayer.getCooledAttackStrength(0.5f) > 0.9 &&
			(ItemStatUtils.getAttributeAmount(mainhand, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) > 0 || ItemStatUtils.getAttributeAmount(mainhand, AttributeType.ATTACK_SPEED, Operation.ADD, Slot.MAINHAND) != 0)) {

			Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(mDuration, mDamageBoost, AFFECTED_DAMAGE_TYPES)));

			new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerBuff(mPlayer);
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(mDuration, mSpeedBoostOnHit, PERCENT_SPEED_EFFECT_NAME));
		}
		return false;
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (ItemUtils.isPickaxe(mPlayer.getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
				return false;
			}
			new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerBuff(mPlayer);
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(mDurationSpawner, mSpeedBoostOnSpawnerBreak, PERCENT_SPEED_EFFECT_NAME));

			if (isLevelTwo()) {
				MeleeScout tempMeleeScout = (new MeleeScout());
				ArrayList<AbilityInfo<?>> mAbilityInfos = tempMeleeScout.mAbilities;
				if(AbilityUtils.getSpecNum(mPlayer) == tempMeleeScout.mSpecOne.mSpecialization) {
					mAbilityInfos.addAll(tempMeleeScout.mSpecOne.mAbilities);
				} else if (AbilityUtils.getSpecNum(mPlayer) == tempMeleeScout.mSpecTwo.mSpecialization) {
					mAbilityInfos.addAll(tempMeleeScout.mSpecTwo.mAbilities);
				}
				Ability[] mAbilities = mAbilityInfos.stream()
					.map(AbilityInfo::getAbilityClass)
					.map(clazz -> mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, clazz))
					.filter(Objects::nonNull)
					.toArray(Ability[]::new);
				for (Ability ability : mAbilities) {
					ClassAbility linkedSpell = ability.getInfo().getLinkedSpell();
					if (linkedSpell != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), linkedSpell)) {
						int cooldownRefresh = (int) (ability.getModifiedCooldown() * mLevelTwoCooldownRefresh);
						mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, cooldownRefresh);
					}
				}
			}
			return true;
		}
		return false;
	}
}
