package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.PlaceholderAbility;
import com.playmonumenta.plugins.abilities.scout.*;
import com.playmonumenta.plugins.abilities._unused.HuntingCompanion;
import com.playmonumenta.plugins.abilities._unused.Swiftness;
import com.playmonumenta.plugins.abilities._unused.Versatile;
import com.playmonumenta.plugins.abilities.scout.bombardier.GravityBombOverworld;
import com.playmonumenta.plugins.abilities.scout.bombardier.Munitions;
import com.playmonumenta.plugins.abilities.scout.bombardier.RendingRazor;
import com.playmonumenta.plugins.abilities.scout.bombardier.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorMissile;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;


public class Scout extends PlayerClass {

	public static final int CLASS_ID = 6;
	public static final int BOMBARDIER_SPEC_ID = 11;
	public static final int HUNTER_SPEC_ID = 12;

	public Scout() {
		mAbilities.add(Quickdraw.INFO);
		mAbilities.add(PlaceholderAbility.INFO); // specialist's quiver
		mAbilities.add(EagleEye.INFO);
		mAbilities.add(Volley.INFO);
		mAbilities.add(Sharpshooter.INFO);
		mAbilities.add(PlaceholderAbility.INFO); // distant visions
		mAbilities.add(ScrapshotOverworld.INFO); // scrapshot
		mAbilities.add(FireworkBlastOverworld.INFO); // firework blast
		mClass = CLASS_ID;
		mClassName = "Scout";
		mClassColor = TextColor.fromHexString("#59B4EB");
		mClassGlassFiller = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
		mDisplayItem = Material.BOW;
		mClassDescription = "Scouts are agile masters of archery and exploration.";
		mClassPassiveDescription = String.format("You gain %d%% of your Projectile Damage %% as Attack Damage and you gain %d%% of your Attack Damage %% as Projectile Damage.",
			(int) (Versatile.DAMAGE_MULTIPLY_MELEE * 100), (int) (Versatile.DAMAGE_MULTIPLY_PROJ * 100));
		mClassPassiveName = "Versatile";

		mSpecOne.mAbilities.add(WindBomb.INFO);
		mSpecOne.mAbilities.add(GravityBombOverworld.INFO); // gbomb
		mSpecOne.mAbilities.add(RendingRazor.INFO); // razor
		mSpecOne.mSpecQuestScoreboard = "Quest103";
		mSpecOne.mSpecialization = BOMBARDIER_SPEC_ID;
		mSpecOne.mSpecName = "Bombardier";
		mSpecOne.mDisplayItem = Material.GUNPOWDER;
		mSpecOne.mDescription = "Bombardier description goes here. Replace me";

		mSpecTwo.mAbilities.add(PinningShot.INFO);
		mSpecTwo.mAbilities.add(SplitArrow.INFO);
		mSpecTwo.mAbilities.add(PredatorMissile.INFO); // predator missile
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = HUNTER_SPEC_ID;
		mSpecTwo.mSpecName = "Hunter";
		mSpecTwo.mDisplayItem = Material.LEATHER;
		mSpecTwo.mDescription = "Hunters are precise masters of archery that have dedicated themselves to projectile weaponry.";

		mTriggerOrder = ImmutableList.of(
			EagleEye.INFO,
			Swiftness.INFO,
			HuntingCompanion.INFO, // after wind bomb
			ScrapshotOverworld.INFO,
			FireworkBlastOverworld.INFO,

			Munitions.INFO,
			WindBomb.INFO,
			GravityBombOverworld.INFO,
			RendingRazor.INFO,

			PredatorMissile.INFO,

			Quickdraw.INFO
		);
	}
}
