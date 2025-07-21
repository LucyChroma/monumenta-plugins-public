package com.playmonumenta.plugins.abilities.scout.bombardier;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.*;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Munitions extends MultipleChargeAbility {

	private enum SelectedSkill {
		WIND_BOMB(WindBomb.class),
		GRAVITY_BOMB(GravityBombOverworld.class),
		RENDING_RAZOR(RendingRazor.class),
		NONE(null);

		final Class<? extends Ability> mSkillClass;

		<T extends Ability> SelectedSkill(Class<T> skillClass) {
			mSkillClass = skillClass;
		}

		public void castSkill(Munitions munitions){
			if(this == NONE) return;
			if(this == WIND_BOMB) munitions.mWindBomb.cast();
			if(this == GRAVITY_BOMB) munitions.mGravityBomb.cast();
			if(this == RENDING_RAZOR) munitions.mRendingRazor.cast();
		}

		public SelectedSkill swapSkill(Player player, boolean suppressError){
			SelectedSkill temp = this;
			for(int i=0;i<3;i++){
				temp = temp.nextSkill();
				Ability ability = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, temp.mSkillClass);
				if(ability != null) return temp;
			}
			if(!suppressError) player.sendMessage(Component.text("You do not have any Bombardier skills!", NamedTextColor.RED));
			return NONE;
		}

		public SelectedSkill nextSkill(){
			return switch(this){
				case WIND_BOMB -> GRAVITY_BOMB;
				case GRAVITY_BOMB -> RENDING_RAZOR;
				case RENDING_RAZOR, NONE -> WIND_BOMB;
			};
		}

		public Ability getSkillInstance(Munitions munitions){
			return switch(this){
				case WIND_BOMB -> munitions.mWindBomb;
				case GRAVITY_BOMB -> munitions.mGravityBomb;
				case RENDING_RAZOR -> munitions.mRendingRazor;
				default -> null;
			};
		}
	}

	public static final int MAX_CHARGES = 3;
	public static final int COOLDOWN = 6 * 20;

	public static final String CHARM_MAX_CHARGES = "Munitions Max Charges";
	public static final String CHARM_COOLDOWN = "Munitions Recharge Delay";

	public static final AbilityInfo<Munitions> INFO =
		new AbilityInfo<>(Munitions.class, "Munitions", Munitions::new)
			.linkedSpell(ClassAbility.MUNITIONS)
			.scoreboardId("Munitions")
			.shorthandName("Mun")
			.descriptions(
				String.format("Placeholder ability level %s.",
					1),
				String.format("Placeholder ability level %s.",
					2),
				String.format("Placeholder ability enhancement. :%s",
					3))
			.simpleDescription("Placeholder.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("swap", "swap selected skill", Munitions::swap,
				new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true), AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast selected skill", Munitions::cast,
				new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true), AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.TNT)
			.canUse(player -> AbilityUtils.getSpecNum(player) == Scout.BOMBARDIER_SPEC_ID);

	SelectedSkill mSelectedSkill;
	WindBomb mWindBomb;
	GravityBombOverworld mGravityBomb;
	RendingRazor mRendingRazor;

	public Munitions(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		// cooldown system doesnt like it if you try to set something on cooldown with 0 points in the skill
		// this doesnt need to be cleaned up when the skill is deselected because whether or not you have
		// the score doesnt actually matter if youre not a bombardier
		ScoreboardUtils.setScoreboardValue(player, INFO.getScoreboard(), 1);

		mCharges = getTrackedCharges();
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_MAX_CHARGES);
		mSelectedSkill = SelectedSkill.NONE;
		// why we need runtask: https://discord.com/channels/1134995398595977227/1394016576944078922/1394039011168751772
		Bukkit.getScheduler().runTask(plugin, () -> {
			mSelectedSkill = SelectedSkill.NONE.swapSkill(player, true);
			mWindBomb = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, WindBomb.class);
			mGravityBomb = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, GravityBombOverworld.class);
			mRendingRazor = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, RendingRazor.class);
		});
	}

	public boolean cast() {
		if (consumeCharge()) {
			mSelectedSkill.castSkill(this);
			putOnCooldown();
			return true;
		}
		return false;
	}

	public boolean swap() {
		mSelectedSkill = mSelectedSkill.swapSkill(mPlayer, false);
		Ability selectedAbility = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, mSelectedSkill.mSkillClass);
		if(selectedAbility == null) {
			MMLog.severe("Munitions somehow selected an ability that the player doesn't have: "+mPlayer.getName()+" selected "+mSelectedSkill.toString());
			return false;
		}
		sendActionBarMessage("Selected: "+selectedAbility.getInfo().getDisplayName());
		return true;
	}

	@Override
	public Component getHotbarMessage(){
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text("Mun", INFO.getActionBarColor()))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		Ability skillInstance = mSelectedSkill.getSkillInstance(this);
		if (skillInstance != null && skillInstance.getInfo().getHotbarName() != null) {
			// output = output.append(Component.text(" ", NamedTextColor.YELLOW));
			output = output.append(Component.text(skillInstance.getInfo().getHotbarName(), NamedTextColor.GRAY));
			output = output.append(Component.text(" ", NamedTextColor.YELLOW));
		}

		int remainingCooldown = mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), ClassAbility.MUNITIONS);

		if (mCharges > 0 && mMaxCharges > 1) {
			output = output.append(Component.text(mCharges + "/" + mMaxCharges, (mCharges >= mMaxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));
		} else if (AbilityUtils.isSilenced(mPlayer)) {
			output = output.append(Component.text(((int) Math.ceil(AbilityUtils.getSilenceDuration(mPlayer) / 20.0)) + "s", NamedTextColor.RED));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}
}
