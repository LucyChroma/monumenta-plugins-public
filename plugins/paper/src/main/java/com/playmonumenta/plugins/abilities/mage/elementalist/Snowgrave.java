package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.*;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class Snowgrave extends Ability implements AbilityWithChargesOrStacks {
	public static final String NAME = "Snowgrave";

	public static final double DAMAGE_1 = 0.4;
	public static final double DAMAGE_2 = 0.55;
	public static final int TICK_DELAY = 1;
	public static final int DURATION_TICKS = 3 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS = 30 * Constants.TICKS_PER_SECOND;
	public static final int STACK_REQUIREMENT = 30;
	public static final double AREA_DAMAGE_RATIO = 0.4;
	public static final double AREA_DAMAGE_RADIUS = 5;

	public static final String CHARM_DAMAGE = "Snowgrave Damage";
	public static final String CHARM_TICK_DELAY = "Snowgrave Tick Delay";
	public static final String CHARM_RANGE = "Snowgrave Range";
	public static final String CHARM_DURATION = "Snowgrave Duration";
	public static final String CHARM_SLOW = "Snowgrave Slowness Amplifier";
	public static final String CHARM_STACK_REQUIREMENT = "Snowgrave Stack Requirement";
	public static final String CHARM_AREA_DAMAGE_RATIO = "Snowgrave Area Damage Ratio";
	public static final String CHARM_AREA_DAMAGE_RADIUS = "Snowgrave Area Damage Radius";

	public static final AbilityInfo<Snowgrave> INFO =
		new AbilityInfo<>(Snowgrave.class, NAME, Snowgrave::new)
			.linkedSpell(ClassAbility.SNOWGRAVE)
			.scoreboardId(NAME)
			.shorthandName("Sg")
			.descriptions(
				String.format("For each spell you cast, gain a stack of Snowgrave for each mob damaged at least once by the spell. " +
					              "Upon reaching %s stacks, press swap while sneaking and looking at an enemy to inflict a DoT effect on the enemy that deals %s damage every tick for %s seconds [%s damage total]. " +
					              "The enemy is stunned and silenced while the spell is casting. Only the first tick of damage interacts with Spellshock.",
					STACK_REQUIREMENT,
					DAMAGE_1,
					StringUtils.ticksToSeconds(DURATION_TICKS),
					(DAMAGE_1 * DURATION_TICKS)),
				String.format("The damage per tick is increased to %s [%s damage total]. " +
					"If the enemy dies before the cast ends, deal %s%% of the remaining damage it would have taken to all mobs within %s blocks of the enemy.",
					DAMAGE_2,
					(DAMAGE_2 * DURATION_TICKS),
					(int) (AREA_DAMAGE_RATIO*100),
					AREA_DAMAGE_RADIUS))
			.simpleDescription("Cast spells to build up stacks, then unleash a powerful ice spell on an enemy.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Snowgrave::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("gainFreeStack", "gain free stack", Snowgrave::gainFreeStack, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK), null))
			.displayItem(Material.BLUE_ICE);

	private final float mLevelDamage;
	private final int mDuration;
	private final int mTickDelay;
	private final double mAreaDamageRatio;
	private final double mAreaDamageRadius;

	protected int mMaxCharges;
	protected int mCharges = 0;

	public Snowgrave(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS);
		mCharges = getCharges();
		mMaxCharges = STACK_REQUIREMENT + (int) CharmManager.getLevel(player, CHARM_STACK_REQUIREMENT);
		mTickDelay = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_TICK_DELAY, TICK_DELAY);
		mAreaDamageRatio = AREA_DAMAGE_RATIO + CharmManager.getLevelPercentDecimal(player, CHARM_AREA_DAMAGE_RATIO);
		mAreaDamageRadius = CharmManager.calculateFlatAndPercentValue(player, CHARM_AREA_DAMAGE_RADIUS, AREA_DAMAGE_RADIUS);
	}

	public boolean cast() {
		// mPlayer.sendRawMessage("cast attempted");
		LivingEntity entity = EntityUtils.getHostileEntityAtCursor(mPlayer, 15);

		if (entity == null) {
			return false;
		}

		if (!consumeCharges()) {
			return false;
		}

		// mPlayer.sendRawMessage("casting");

		GlowingManager.startGlowing(entity, NamedTextColor.AQUA, mDuration, GlowingManager.PLAYER_ABILITY_PRIORITY);
		EntityUtils.applyStun(mPlugin, mDuration, entity);
		EntityUtils.applySilence(mPlugin, mDuration, entity);
		float spellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			final LivingEntity mEntity = entity;

			@Override
			public void run() {
				mTicks++;

				if (mTicks % mTickDelay == 0) {
					DamageUtils.damage(mPlayer, mEntity, DamageType.MAGIC, spellDamage, mInfo.getLinkedSpell(), true);
				}

				if(mEntity.isDead() || !mEntity.isValid()) {
					if(isLevelTwo() && mTicks < mDuration) {
						// mPlayer.sendRawMessage("cold snap");
						int remainingTicks = (mDuration / mTickDelay - mTicks / mTickDelay); // weird division stuff bc int division
						if(remainingTicks <= 0) this.cancel(); // skip ahead if there's no damage to be dealt
						float remainingBaseDamage = remainingTicks * mLevelDamage;
						double areaSpellDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, remainingBaseDamage) * mAreaDamageRatio;
						// mPlayer.sendRawMessage(String.valueOf(remainingTicks) + " "+ String.valueOf(remainingBaseDamage) +" "+ String.valueOf(areaSpellDamage));
						Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mAreaDamageRadius);
						for (LivingEntity mob : hitbox.getHitMobs()) {
							DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, areaSpellDamage, mInfo.getLinkedSpell(), true );
						}
					}
					this.cancel();
				}

				if (mTicks >= mDuration) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake() || event.getAbility() == mInfo.getLinkedSpell() || event.getAbility() == ClassAbility.SPELLSHOCK) {
			return false;
		}

		incrementCharge();
		return false;
	}

	public boolean gainFreeStack(){
		incrementCharge();
		return true;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	public boolean incrementCharge() {
		if (mCharges < mMaxCharges) {
			mCharges++;
			if (mMaxCharges > 1) {
				showChargesMessage();
			} else {
				showOffCooldownMessage();
			}
			ClientModHandler.updateAbility(mPlayer, this);
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.SNOWGRAVE, mCharges);

			return true;
		}

		return false;
	}

	protected boolean consumeCharges() {
		if (mCharges == mMaxCharges) {
			mCharges = 0;
			ClientModHandler.updateAbility(mPlayer, this);
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.SNOWGRAVE, mCharges);
			return true;
		}

		return false;
	}

	public Component getHotbarMessage(){
		NamedTextColor numbersColor = mCharges == 0 ? NamedTextColor.GRAY : mCharges == mMaxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
		return Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text("Sg",NamedTextColor.AQUA))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE))
			.append(Component.text(mCharges + "/" + mMaxCharges, numbersColor));
	}
}
