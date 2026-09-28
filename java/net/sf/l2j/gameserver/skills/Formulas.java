package net.sf.l2j.gameserver.skills;

import java.util.logging.Logger;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns.CabalType;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Buffer;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.model.zone.type.L2MotherTreeZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkAccuracy;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkEvasion;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public final class Formulas
{
	protected static final Logger _log = Logger.getLogger(Formulas.class.getName());
	
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	public static final byte SKILL_REFLECT_FAILED = 0; // no reflect
	public static final byte SKILL_REFLECT_SUCCEED = 1; // normal reflect, some damage reflected some other not
	public static final byte SKILL_REFLECT_VENGEANCE = 2; // 100% of the damage affect both
	
	private static final byte MELEE_ATTACK_RANGE = 70;
	
	public static final int MAX_STAT_VALUE = 200;
	
	private static final double[] STR_COMPUTE = new double[]
	{
		1.036,
		34.845
	};
	private static final double[] INT_COMPUTE = new double[]
	{
		1.020,
		31.375
	};
	private static final double[] DEX_COMPUTE = new double[]
	{
		1.009,
		19.360
	};
	private static final double[] WIT_COMPUTE = new double[]
	{
		1.030,
		19.800
	};
	private static final double[] CON_COMPUTE = new double[]
	{
		1.030,
		27.552
	};
	private static final double[] MEN_COMPUTE = new double[]
	{
		1.010,
		-0.060
	};
	
	public static final double[] WIT_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] MEN_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] INT_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] STR_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] DEX_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] CON_BONUS = new double[MAX_STAT_VALUE];
	
	public static final double[] BASE_EVASION_ACCURACY = new double[MAX_STAT_VALUE];
	
	protected static final double[] SQRT_MEN_BONUS = new double[MAX_STAT_VALUE];
	protected static final double[] SQRT_CON_BONUS = new double[MAX_STAT_VALUE];
	
	static
	{
		for (int i = 0; i < STR_BONUS.length; i++)
			STR_BONUS[i] = Math.floor(Math.pow(STR_COMPUTE[0], i - STR_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < INT_BONUS.length; i++)
			INT_BONUS[i] = Math.floor(Math.pow(INT_COMPUTE[0], i - INT_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < DEX_BONUS.length; i++)
			DEX_BONUS[i] = Math.floor(Math.pow(DEX_COMPUTE[0], i - DEX_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < WIT_BONUS.length; i++)
			WIT_BONUS[i] = Math.floor(Math.pow(WIT_COMPUTE[0], i - WIT_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < CON_BONUS.length; i++)
			CON_BONUS[i] = Math.floor(Math.pow(CON_COMPUTE[0], i - CON_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < MEN_BONUS.length; i++)
			MEN_BONUS[i] = Math.floor(Math.pow(MEN_COMPUTE[0], i - MEN_COMPUTE[1]) * 100 + .5d) / 100;
		
		for (int i = 0; i < BASE_EVASION_ACCURACY.length; i++)
			BASE_EVASION_ACCURACY[i] = Math.sqrt(i) * 6;
		
		// Precompute square root values
		for (int i = 0; i < SQRT_CON_BONUS.length; i++)
			SQRT_CON_BONUS[i] = Math.sqrt(CON_BONUS[i]);
		for (int i = 0; i < SQRT_MEN_BONUS.length; i++)
			SQRT_MEN_BONUS[i] = Math.sqrt(MEN_BONUS[i]);
	}
	
	private static final double[] karmaMods =
	{
		0,
		0.772184315,
		2.069377971,
		2.315085083,
		2.467800843,
		2.514219611,
		2.510075822,
		2.532083418,
		2.473028945,
		2.377178509,
		2.285526643,
		2.654635163,
		2.963434737,
		3.266100461,
		3.561112664,
		3.847320291,
		4.123878064,
		4.390194136,
		4.645886341,
		4.890745518,
		5.124704707,
		6.97914069,
		7.270620642,
		7.548951721,
		7.81438302,
		8.067235867,
		8.307889422,
		8.536768399,
		8.754332624,
		8.961068152,
		9.157479758,
		11.41901707,
		11.64989746,
		11.87007991,
		12.08015809,
		12.28072687,
		12.47237891,
		12.65570177,
		12.83127553,
		12.99967093,
		13.16144786,
		15.6563607,
		15.84513182,
		16.02782135,
		16.20501182,
		16.37727218,
		16.54515749,
		16.70920885,
		16.86995336,
		17.02790439,
		17.18356182,
		19.85792061,
		20.04235517,
		20.22556446,
		20.40806338,
		20.59035551,
		20.77293378,
		20.95628115,
		21.1408714,
		21.3271699,
		21.51563446,
		24.3895427,
		24.61486587,
		24.84389213,
		25.07711247,
		25.31501442,
		25.55808296,
		25.80680152,
		26.06165297,
		26.32312062,
		26.59168923,
		26.86784604,
		27.15208178,
		27.44489172,
		27.74677676,
		28.05824444,
		28.37981005,
		28.71199773,
		29.05534154,
		29.41038662,
		29.77769028,
		
		30.04235517,
		30.22556446,
		30.40806338,
		30.59035551,
		30.77293378,
		30.95628115,
		31.14087141,
		31.32716991,
		31.51563446,
		34.38954271,
		34.61486587,
		34.84389213,
		35.07711247,
		35.31501442,
		35.55808296,
		35.80680152,
		36.06165297,
		36.32312062,
		36.59168923,
		36.86784604,
		37.15208178,
		37.44489172,
		37.74677676,
		38.05824444,
		38.37981005,
		38.71199773,
		39.05534154,
		39.41038662,
		39.77769028
	};
	
	/**
	 * @param cha The character to make checks on.
	 * @return the period between 2 regenerations task (3s for Creature, 5 min for L2DoorInstance).
	 */
	public static int getRegeneratePeriod(Creature cha)
	{
		if (cha instanceof Door)
			return HP_REGENERATE_PERIOD * 100; // 5 mins
			
		return HP_REGENERATE_PERIOD; // 3s
	}
	
	/**
	 * @param cha The character to make checks on.
	 * @return the HP regen rate (base + modifiers).
	 */
	public static final double calcHpRegen(Creature cha)
	{
		double init = cha.getTemplate().getBaseHpReg();
		
		if (init <= 0)
			return 0;
		
		double regen = cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null);
		
		if (regen <= 0)
			return 0;
		
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		
		if (cha.isChampion())
			hpRegenMultiplier *= Config.CHAMPION_HP_REGEN;
		
		if (cha instanceof Player)
		{
			Player player = (Player) cha;
			
			// Calculate correct baseHpReg value for certain level of PC
			// init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;
			regen += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;
			
			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			else if (calcSiegeRegenModifer(player))
				hpRegenMultiplier *= 1.5;
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHideoutId();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
							hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}
			
			if (player.isInCombat())
			{
				if (player.isRunning())
					hpRegenMultiplier *= 0.7; // Running
			}
			else
			{
				if (!player.isCursedWeaponEquipped())
				{
					if (player.isSitting()) // sitting
					{
						if (!player.isInOlympiadMode())
							hpRegenMultiplier *= 24;
						else
							hpRegenMultiplier *= 6;
					}
					else if (player.isRunning())
						hpRegenMultiplier *= 0.7; // Running
					else if (!player.isInOlympiadMode())
						hpRegenMultiplier *= 5; // standing still
				}
			}
			// Add CON bonus
			init *= cha.getLevelMod() * CON_BONUS[cha.getCON()];
		}
		
		return regen * hpRegenMultiplier + hpRegenBonus;
	}
	
	/**
	 * @param cha The character to make checks on.
	 * @return the MP regen rate (base + modifiers).
	 */
	public static final double calcMpRegen(Creature cha)
	{
		double init = cha.getTemplate().getBaseMpReg();
		
		if (init <= 0)
			return 0;
		
		double regen = cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null);
		
		if (regen <= 0)
			return 0;
		
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		
		if (cha instanceof Player)
		{
			final Player player = (Player) cha;
			
			if (!player.isInOlympiadMode())
			{
				// Calculate correct baseMpReg value for certain level of PC
				regen += 0.3 * ((player.getLevel(true) - 1) / 10.0);
				
				// Add MEN bonus
				regen *= cha.getLevelMod() * MEN_BONUS[cha.getMEN()];
				
				// SevenSigns Festival modifier
				if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
					mpRegenMultiplier *= calcFestivalRegenModifier(player);
				
				// Mother Tree effect is calculated at last
				if (player.isInsideZone(ZoneId.MOTHER_TREE))
					mpRegenBonus += 1;
				
				else if (player.getClan() != null)
				{
					if (player.isInsideZone(ZoneId.CLAN_HALL))
					{
						int clanHallIndex = player.getClan().getHideoutId();
						
						if (clanHallIndex > 0)
						{
							ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
							
							if (clansHall != null)
								if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
									mpRegenMultiplier *= 1 + (double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
				}
				
				final boolean combat = player.isInCombat();
				final boolean running = player.isRunning();
				final boolean moving = player.isMoving();
				final boolean sitting = player.isSitting();
				
				if (!player.isCursedWeaponEquipped())
				{
					if (sitting)
					{
						if (!player.isInOlympiadMode())
							mpRegenMultiplier *= 10;
						else
							mpRegenMultiplier *= 5;
					}
					else if (moving)
					{
						if (running)
							mpRegenMultiplier *= 0.5;
						else
							mpRegenMultiplier *= 1;
					}
					else // standing in place
					{
						mpRegenMultiplier *= 2;
					}
				}
				
				if (combat)
					mpRegenMultiplier *= 0.5;
				
				final double percRemaining = player.getCurrentMp() / player.getMaxMp();
				
				if (percRemaining < 0.02)
					mpRegenMultiplier *= 0.33;
				
				else if (percRemaining < 0.2)
					mpRegenMultiplier *= 0.5;
				
				else if (percRemaining < 0.5)
					mpRegenMultiplier *= 0.75;
			}
		}
		
		if (regen < 1)
			regen = 1;
		
		return regen * mpRegenMultiplier + mpRegenBonus;
	}
	
	/**
	 * @param player The player to make checks on.
	 * @return the CP regen rate (base + modifiers).
	 */
	public static final double calcCpRegen(Player player)
	{
		// Calculate correct baseHpReg value for certain level of PC
		double init = player.getTemplate().getBaseHpReg() /* + ((player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5) */;
		
		if (init <= 0)
			return 0;
		
		double regen = player.calcStat(Stats.REGENERATE_CP_RATE, init, null, null);
		
		if (regen <= 0)
			return 0;
		
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		
		if (!player.isInOlympiadMode())
		{
			// Calculate correct baseHpReg value for certain level of PC
			regen += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;
			
			if (!player.isCursedWeaponEquipped())
			{
				// Calculate Movement bonus
				if (player.isSitting() && !player.isInCombat())
				{
					if (!player.isInOlympiadMode())
						cpRegenMultiplier *= 18; // Sitting
					else
						cpRegenMultiplier *= 6;
				}
				else if (!player.isMoving())
					cpRegenMultiplier *= 1.5; // Staying
				else if (player.isRunning())
					cpRegenMultiplier *= 0.7; // Running
			}
		}
		
		// Apply CON bonus
		regen *= player.getLevelMod() * CON_BONUS[player.getCON()];
		
		if (regen < 1)
			regen = 1;
		
		return regen * cpRegenMultiplier;
	}
	
	public static final double calcFestivalRegenModifier(Player activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final CabalType oracle = CabalType.VALUES[festivalInfo[0]];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;
		
		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
			return 0;
		
		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == CabalType.DAWN)
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		else
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		
		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = activeChar.getPlanDistanceSq(festivalCenter[0], festivalCenter[1]);
		
		if (Config.DEVELOPER)
			_log.info("Distance: " + distToCenter + ", RegenMulti: " + (distToCenter * 2.5) / 50);
		
		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}
	
	/**
	 * @param activeChar the player to test on.
	 * @return true if the player is near one of his clan HQ (+50% regen boost).
	 */
	public static final boolean calcSiegeRegenModifer(Player activeChar)
	{
		if (activeChar == null || activeChar.getClan() == null)
			return false;
		
		final Siege siege = CastleManager.getInstance().getActiveSiege(activeChar);
		if (siege == null || !siege.checkSide(activeChar.getClan(), SiegeSide.ATTACKER))
			return false;
		
		return MathUtil.checkIfInRange(200, activeChar, activeChar.getClan().getFlag(), true);
	}
	
	/**
	 * @param attacker The attacker, from where the blow comes from.
	 * @param target The victim of the blow.
	 * @param skill The skill used.
	 * @param shld True if victim was wearign a shield.
	 * @param ss True if ss were activated.
	 * @return blow damage based on cAtk
	 */
	public static double calcBlowDamage(Creature attacker, Creature target, L2Skill skill, byte shld, boolean ss)
	{
		if (skill == null)
		{
			_log.warning("LOL WTF calc blow dmg but no skill sent");
			return 0;
		}
		
		double damage = attacker.getPAtk(target);
		
		damage += calcValakasAttribute(attacker, target, skill);
		
		damage *= 1.5;
		
		double power = skill.getPower(attacker);
		
		if (skill.isStaticPower())
			power = 6000;
		
		else if (skill.getSSBoost() > 1)
		{
			damage += damage * (skill.getSSBoost() - 1) * 0.3;
			power += power * (skill.getSSBoost() - 1) * 0.7;
		}
		
		double defence = target.getPDef(attacker);
		
		final int pdefIgnore = (int) attacker.calcStat(Stats.PDEF_IGNORE, 0, target, skill);
		
		if (pdefIgnore > 0)
		{
			if (defence > pdefIgnore)
				defence = pdefIgnore;
		}
		
		defence = attacker.calcStat(Stats.PDEF_REDUCE, defence, target, skill);
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		final boolean isPvP = attacker instanceof Playable && target instanceof Playable;
		
		damage = (damage + power) * attacker.getCriticalDmg(target, 1, skill);
		damage *= calcElemental(attacker, target, skill);
		damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 7;
		damage *= target.calcStat(Stats.CRIT_VULN, 1, target, skill);
		
		// defence modifier depending of the attacker weapon
		final Weapon weapon = attacker.getActiveWeaponItem();
		
		Stats stat = null;
		
		if (weapon == null)
		{
			if (attacker instanceof Summon)
			{
				stat = Stats.BLUNT_WPN_VULN;
				
				try
				{
					if (attacker.getName().contains("Feline King"))
						stat = Stats.SWORD_WPN_VULN;
					else if (attacker.getName().contains("Spectral Lord"))
						stat = Stats.DAGGER_WPN_VULN;
					else
						stat = Stats.BLUNT_WPN_VULN;
				}
				catch (Exception e)
				{
				}
			}
		}
		
		boolean dagger = false;
		
		if (weapon != null)
		{
			damage *= calcWeaponResistanceModifier(weapon, target, skill);
			
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					dagger = true;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
			}
		}
		
		if (stat != null)
		{
			damage *= target.calcStat(stat, 1, target, null);
			
			if (target instanceof Npc)
			{
				// get the natural vulnerability for the template
				damage *= ((Npc) target).getTemplate().getVulnerability(stat);
			}
		}
		
		damage *= 70. / defence;
		
		damage *= attacker.getRandomDamageMultiplier(target);
		
		// Dmg bonusses in PvP fight
		if (isPvP)
		{
			damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, attacker, null);
			
			damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_DMG, 0.86, null, null);
			
			damage *= skill.getPvpMulti();
		}
		
		else
		{
			if (attacker instanceof Playable && target instanceof Monster)
			{
				switch (((Npc) target).getTemplate().getRace())
				{
					case UNDEAD:
						damage *= attacker.getPAtkUndead(target);
						break;
					case DEMON:
						damage *= attacker.getPAtkDemons(target);
						break;
					case ANGEL:
						damage *= attacker.getPAtkAngels(target);
						break;
					case BEAST:
						damage *= attacker.getPAtkMonsters(target);
						break;
					case ANIMAL:
						damage *= attacker.getPAtkAnimals(target);
						break;
					case PLANT:
						damage *= attacker.getPAtkPlants(target);
						break;
					case DRAGON:
						damage *= attacker.getPAtkDragons(target);
						break;
					case BUG:
						damage *= attacker.getPAtkInsects(target);
						break;
					case GIANT:
						damage *= attacker.getPAtkGiants(target);
						break;
					case MAGICCREATURE:
					case SPIRIT:
						damage *= attacker.getPAtkMagicCreatures(target);
						break;
					default:
						break;
				}
				
				if (!dagger)
					damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 2.0, null, null); // the 2 here already factors in the PVM skill boost
				else
					damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 1.23, null, null); // the 2 here already factors in the PVM skill boost
					
				damage *= skill.getPvmMulti();
			}
			else if (attacker instanceof Monster && (target instanceof Playable || attacker instanceof Summon))
				damage *= target.getActingPlayer().calcStat(Stats.PVM_DAMAGE_VUL, 1, null, null);
		}
		
		damage *= attacker.calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		
		double hpdam = attacker.calcStat(Stats.INC_DAM_HP, 0, target, skill);
		
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentHp() / target.getMaxHp()) * hpdam;
		
		hpdam = attacker.calcStat(Stats.INC_DAM_MP, 0, target, skill);
		
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentMp() / target.getMaxMp()) * hpdam;
		
		if (target instanceof Player)
		{
			hpdam = attacker.calcStat(Stats.INC_DAM_CP, 0, target, skill);
			
			if (hpdam != 0)
				damage += damage * (1 - target.getCurrentCp() / target.getMaxCp()) * hpdam;
			
			final Player targetPlayer = (Player) target;
			
			if (targetPlayer.isWearingHeavyArmor())
			{
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, target, skill);
			}
			else if (targetPlayer.isWearingLightArmor())
			{
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, target, skill);
			}
			else
			{
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, target, skill);
				damage += 150;
			}
		}
		
		if (attacker.isBehind(target))
			damage = target.calcStat(Stats.POWER_DEFENCE_BEHIND, damage, attacker, skill);
		
		if (attacker instanceof Attackable && target instanceof Player && target.getActingPlayer().isSitting())
			damage *= 3;
		
		damage = Math.max(damage, target.calcStat(Stats.PDAM_MAX, 0, attacker, skill));
		
		damage += attacker.calcStat(Stats.DMG_ADD, 0, target, skill);
		damage -= target.calcStat(Stats.DMG_REMOVE, 0, attacker, skill);
		
		if (shld > 0)
			damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, skill);
		
		return damage < 1 ? 1. : damage;
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target, called separatly for each weapon, if dual-weapon is used.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill skill used.
	 * @param shld target was using a shield or not.
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @return damage points
	 */
	public static final double calcPhysDam(Creature attacker, Creature target, L2Skill skill, byte shld, boolean crit, boolean ss)
	{
		if (attacker instanceof Player)
		{
			Player pcInst = (Player) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}
		
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		final int pdefIgnore = (int) attacker.calcStat(Stats.PDEF_IGNORE, 0, target, skill);
		
		if (pdefIgnore > 0)
		{
			if (defence > pdefIgnore)
				defence = pdefIgnore;
		}
		
		defence = attacker.calcStat(Stats.PDEF_REDUCE, defence, target, skill);

		damage += calcValakasAttribute(attacker, target, skill);
		
		if (ss)
			damage *= 2;
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}
		
		final boolean isPvP = attacker instanceof Playable && target instanceof Playable;
		
		if (skill != null)
		{
			if (skill.isStaticPower())
				damage = 6000;
			
			double power = skill.getPower(attacker);
			float ssboost = skill.getSSBoost();
			
			if (ssboost <= 1)
				damage += power;
			else
			{
				damage += damage * (skill.getSSBoost() - 1) * 0.25;
				power += power * (skill.getSSBoost() - 1) * 0.72;
				damage += power;
			}
			
			if (target instanceof Monster)
			{
				switch (skill.getSkillType())
				{
					case PDAM:
					case FATAL:
					case CHARGEDAM:
						damage *= 1.5;
				}
			}
			
			damage *= attacker.calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		}
		else
		{
			if (attacker instanceof Player)
			{
				int charges = attacker.getActingPlayer().getCharges();
				
				if (charges >= 1)
				{
					final double damMulti = attacker.calcStat(Stats.INC_PHYSDAM_CHARGES, 0, null, null);
					
					if (damMulti != 0)
						damage *= 1 + (damMulti * charges);
				}
			}
		}
		
		// defence modifier depending of the attacker weapon
		Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		
		if (weapon != null)
		{
			damage *= calcWeaponResistanceModifier(weapon, target, skill);
			
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
			}
		}
		else
		{
			if (attacker instanceof Summon)
			{
				stat = Stats.BLUNT_WPN_VULN;
				
				try
				{
					if (attacker.getName().contains("Feline King"))
						stat = Stats.SWORD_WPN_VULN;
					else if (attacker.getName().contains("Spectral Lord"))
						stat = Stats.DAGGER_WPN_VULN;
					else
						stat = Stats.BLUNT_WPN_VULN;
				}
				catch (Exception e)
				{
				}
			}
		}
		
		if (crit)
		{
			// Crit dmg add is almost useless in normal hits...
			damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 2;
			
			// Finally retail like formula
			damage = attacker.getCriticalDmg(target, 1.66, skill) * target.calcStat(Stats.CRIT_VULN, 1, target, null) * (Config.PHYSICAL_DAMAGE_BALANCE * damage / defence);
		}
		else
			damage = Config.PHYSICAL_DAMAGE_BALANCE * damage / defence;
		
		if (stat != null)
		{
			// get the vulnerability due to skills (buffs, passives, toggles, etc)
			damage *= target.calcStat(stat, 1, target, null);
			
			if (target instanceof Npc)
			{
				// get the natural vulnerability for the template
				damage *= ((Npc) target).getTemplate().getVulnerability(stat);
				
				if (stat == Stats.BOW_WPN_VULN && target instanceof RaidBoss)
					damage *= 0.75;
			}
			
			float rangedAtkBoost = 0;
			
			if (skill == null)
				rangedAtkBoost = (float) attacker.calcStat(Stats.RANGE_DMG_DIST_BOOST, 0, target, null); // given in PERCENT, non skill
			else
				rangedAtkBoost = (float) attacker.calcStat(Stats.RANGE_DMG_DIST_BOOST_SKILL, 0, target, skill); // given in PERCENT
				
			if (rangedAtkBoost > 0)
			{
				int distToTarg = (int) Util.calculateDistance(attacker, target, false);
				
				if (distToTarg > 40)
				{
					distToTarg -= 40;
					distToTarg = (int) Math.pow(distToTarg, 1.15);
					damage *= distToTarg * rangedAtkBoost + 1;
				}
			}
		}
		
		damage += Rnd.nextDouble() * damage / 10;
		
		if (shld > 0 && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
				damage = 0;
		}
		
		if (target instanceof Npc)
		{
			double multiplier;
			switch (((Npc) target).getTemplate().getRace())
			{
				case BEAST:
					multiplier = 1 + ((attacker.getPAtkMonsters(target) - target.getPDefMonsters(target)) / 100);
					damage *= multiplier;
					break;
				
				case ANIMAL:
					multiplier = 1 + ((attacker.getPAtkAnimals(target) - target.getPDefAnimals(target)) / 100);
					damage *= multiplier;
					break;
				
				case PLANT:
					multiplier = 1 + ((attacker.getPAtkPlants(target) - target.getPDefPlants(target)) / 100);
					damage *= multiplier;
					break;
				
				case DRAGON:
					multiplier = 1 + ((attacker.getPAtkDragons(target) - target.getPDefDragons(target)) / 100);
					damage *= multiplier;
					break;
				
				case BUG:
					multiplier = 1 + ((attacker.getPAtkInsects(target) - target.getPDefInsects(target)) / 100);
					damage *= multiplier;
					break;
				
				case GIANT:
					multiplier = 1 + ((attacker.getPAtkGiants(target) - target.getPDefGiants(target)) / 100);
					damage *= multiplier;
					break;
				
				case MAGICCREATURE:
					multiplier = 1 + ((attacker.getPAtkMagicCreatures(target) - target.getPDefMagicCreatures(target)) / 100);
					damage *= multiplier;
					break;
			}
		}
		else if (target instanceof Player)
		{
			if (((Player) target).isWearingHeavyArmor())
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, null, skill);
			
			else if (((Player) target).isWearingLightArmor())
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, null, skill);
			
			else
			{
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, null, skill);
				
				if (weapon != null)
				{
					if (weapon.getItemType() == WeaponType.BOW)
					{
						damage *= 1.06;
						damage += 170;
					}
				}
				
				if (attacker instanceof RaidBoss)
					damage *= 2;
			}
		}
		
		if (damage > 0 && damage < 1)
			damage = 1;
		
		else if (damage < 0)
			damage = 0;
		
		// Dmg bonuses in PvP fight
		if (isPvP)
		{
			if (skill == null)
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYSICAL_DMG, 0.87, null, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYSICAL_VUL, 1, attacker, null);
			}
			else
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_DMG, 0.86, null, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, attacker, skill);
				damage *= skill.getPvpMulti();
			}
		}
		
		else
		{
			if ((attacker instanceof Playable || attacker instanceof Summon) && target instanceof Monster)
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 1, target, skill);
				
				if (skill != null)
					damage *= skill.getPvmMulti();
			}
			else if (attacker instanceof Monster && (target instanceof Playable || target instanceof Summon))
			{
				damage *= target.getActingPlayer().calcStat(Stats.PVM_DAMAGE_VUL, 1, target, skill);
				
				if (target instanceof Summon)
				{
					if (attacker instanceof RaidBoss)
						damage *= 2.2;
					else
						damage *= 1.5;
					
					damage += 100;
				}
			}
		}
		
		if (target instanceof Player)
		{
			final Player targetPlayer = (Player) target;
			
			if (targetPlayer.isWearingHeavyArmor())
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, target, skill);
			
			else if (targetPlayer.isWearingLightArmor())
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, target, skill);
			else
			{
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, target, skill);
				
				if (attacker instanceof RaidBoss)
					damage *= 2;
			}
		}
		
		// Weapon elemental damages
		damage += calcElemental(attacker, target, null);
		
		double hpdam = attacker.calcStat(Stats.INC_DAM_HP, 0, target, skill);
		
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentHp() / target.getMaxHp()) * hpdam;
		
		hpdam = attacker.calcStat(Stats.INC_DAM_MP, 0, target, skill);
		
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentMp() / target.getMaxMp()) * hpdam;
		
		if (target instanceof Player)
		{
			hpdam = attacker.calcStat(Stats.INC_DAM_CP, 0, target, skill);
			
			if (hpdam != 0)
				damage += damage * (1 - target.getCurrentCp() / target.getMaxCp()) * hpdam;
		}
		
		if (attacker.isBehind(target))
			damage = target.calcStat(Stats.POWER_DEFENCE_BEHIND, damage, attacker, skill);
		
		if (attacker instanceof Attackable && target instanceof Player && target.getActingPlayer().isSitting())
			damage *= 3;
		
		damage = Math.max(damage, target.calcStat(Stats.PDAM_MAX, 0, attacker, skill));
		
		if (skill != null)
		{
			damage += attacker.calcStat(Stats.DMG_ADD, 0, target, skill);
			damage -= target.calcStat(Stats.DMG_REMOVE, 0, attacker, skill);
			
			if (shld > 0)
				damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, skill);
		}
		
		if (attacker instanceof Player)
		{
			if (weapon != null)
			{
				Player player = (Player) attacker;
				if (weapon.getItemType() == WeaponType.BOW)
					damage = damage * 0.9;
				if (player.isProphet() || player.isDoom())
					damage = damage * 1.1;
			}
		}
		
		if (damage < 0)
			return 0;
		
		return damage;
	}
	
	public static final double calcMagicDam(Creature attacker, Creature target, L2Skill skill, byte shld, boolean ss, boolean bss, boolean mcrit)
	{
		if (attacker instanceof Player)
		{
			Player pcInst = (Player) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}
		
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		
		final int mDefIgnore = (int) attacker.calcStat(Stats.MDEF_IGNORE, 0, target, skill);
		
		if (mDefIgnore > 0)
		{
			if (mDef > mDefIgnore)
				mDef = mDefIgnore;
		}
		
		mDef = attacker.calcStat(Stats.MDEF_REDUCE, mDef, target, skill);
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}
		
		if (bss)
			mAtk *= 3.5;
		else if (ss)
			mAtk *= 2;
		
		if (skill.isStaticPower())
			mAtk = 14000;
		
		double damage = 95 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker);
		
		if ((bss || ss) && skill.getSSBoost() > 0)
			damage *= skill.getSSBoost();
		
		// Failure calculation
		if (Config.MAGIC_FAILURES && !calcMagicSuccess())
		{
			if (attacker instanceof Player)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
				else
					attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
				
				damage /= 2;
			}
		}
		else if (mcrit)
			damage *= attacker.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 1.75, target, skill);
		
		damage += Rnd.nextDouble() * attacker.getRandomDamage(target);
		
		// Pvp bonuses for dmg
		if (attacker instanceof Playable && target instanceof Playable)
		{
			if (skill.isMagic())
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_MAGICAL_DMG, 0.9, null, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_MAGICAL_VUL, 1, attacker, null);
			}
			else
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_DMG, 0.86, null, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, attacker, null);
			}
			
			damage *= skill.getPvpMulti();
		}
		else
		{
			if ((attacker instanceof Playable || attacker instanceof Summon) && target instanceof Monster)
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 1, target, null);// damage *= skill.getPvmMulti();
				damage *= skill.getPvmMulti();
			}
			else if (attacker instanceof Monster && (target instanceof Playable || attacker instanceof Summon))
				damage *= target.getActingPlayer().calcStat(Stats.PVM_DAMAGE_VUL, 1, attacker, null);
		}
		
		if (target instanceof Player)
		{
			if (((Player) target).isWearingHeavyArmor())
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, null, skill);
			
			else if (((Player) target).isWearingLightArmor())
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, null, skill);
			
			else
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, null, skill);
		}
		
		float rangedAtkBoost = (float) attacker.calcStat(Stats.RANGE_DMG_DIST_BOOST_SKILL, 0, target, skill); // given in PERCENT;
		
		if (rangedAtkBoost > 0)
		{
			int distToTarg = (int) Util.calculateDistance(attacker, target, false);
			
			if (distToTarg > 40)
			{
				distToTarg -= 40;
				distToTarg = (int) Math.pow(distToTarg, 1.15);
				damage *= distToTarg * rangedAtkBoost + 1;
			}
		}
		
		damage *= attacker.calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		
		// random magic damage
		float rnd = Rnd.get(-20, 20) / 100 + 1;
		damage *= rnd;
		
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, skill);
		
		if (attacker.getActiveWeaponItem() != null)
			damage *= calcWeaponResistanceModifier(attacker.getActiveWeaponItem(), target, skill);
		
		damage *= calcElemental(attacker, target, skill);
		
		double hpdam = attacker.calcStat(Stats.INC_DAM_HP, 0, target, skill);
		
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentHp() / target.getMaxHp()) * hpdam;
		
		hpdam = attacker.calcStat(Stats.INC_DAM_MP, 0, target, skill);
		
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentMp() / target.getMaxMp()) * hpdam;
		
		if (target instanceof Player)
		{
			hpdam = attacker.calcStat(Stats.INC_DAM_CP, 0, target, skill);
			
			if (hpdam != 0)
				damage += damage * (1 - target.getCurrentCp() / target.getMaxCp()) * hpdam;
		}
		
		if (attacker.isBehind(target))
			damage = target.calcStat(Stats.MAGIC_DEFENCE_BEHIND, damage, attacker, skill);
		
		if (attacker instanceof Attackable && target instanceof Player && target.getActingPlayer().isSitting())
			damage *= 3;
		
		damage = Math.max(damage, target.calcStat(Stats.MDAM_MAX, 0, attacker, skill));
		
		damage += attacker.calcStat(Stats.DMG_ADD, 0, target, skill);
		damage -= target.calcStat(Stats.DMG_REMOVE, 0, attacker, skill);
		
		if (shld > 0)
			damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, skill);
		
		if (damage < 0)
			return 0;
		
		return damage;
	}
	
	public static final double calcMagicDam(Cubic attacker, Creature target, L2Skill skill, boolean mcrit, byte shld)
	{
		double mAtk = attacker.getMAtk();
		
		if (skill.isStaticPower())
			mAtk = 10000;
		double mDef = target.getMDef(attacker.getOwner(), skill);
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		Player owner = attacker.getOwner();
		
		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(owner);
		
		// Failure calculation
		if (Config.MAGIC_FAILURES && !calcMagicSuccess())
		{
			if (skill.getSkillType() == L2SkillType.DRAIN)
				owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
			else
				owner.sendPacket(SystemMessageId.ATTACK_FAILED);
			
			damage /= 2;
		}
		else if (mcrit)
			damage *= 2.5;
		
		if (target instanceof Playable)
			damage *= skill.getPvpMulti();
		else if (target instanceof Attackable)
			damage *= skill.getPvmMulti();
		
		damage *= attacker.getOwner().calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, skill);
		
		damage *= calcElemental(owner, target, skill);
		
		if (owner.isBehind(target))
			damage = target.calcStat(Stats.MAGIC_DEFENCE_BEHIND, damage, attacker.getOwner(), skill);
		
		damage = Math.max(damage, owner.calcStat(Stats.MDAM_MAX, 0, null, skill));
		
		return damage;
	}
	
	/**
	 * Returns true in case of critical hit
	 * @param attacker
	 * @param rate
	 * @param target
	 * @param skill
	 * @return
	 */
	public static final boolean calcCrit(final Creature attacker, double rate, Creature target, L2Skill skill)
	{
		if (rate < 1)
			return false;
		
		int chance = Rnd.get(1000);
		
		if (attacker.isBehind(target, 60))
			chance -= 60;
		
		else if (!attacker.isInFrontOf(target))
			chance -= 35;
		
		final boolean success = rate > chance;
		// support for critical damage evasion
		if (success)
		{
			if (target == null)
				return false; // no effect
			if (skill == null)
			{
				final int critDmgEvas = (int) target.calcStat(Stats.CRIT_DAMAGE_EVASION, 0, attacker, null);
				
				if (critDmgEvas == 0)
					return true;
				return critDmgEvas <= Rnd.get(100);
			}
			
			final int critDmgEvas = (int) target.calcStat(Stats.SKILL_CRIT_DAMAGE_EVASION, 0, attacker, skill);
			
			if (critDmgEvas == 0)
				return true;
			
			return critDmgEvas <= Rnd.get(100);
		}
		return false;
	}
	
	public static final boolean calcCrit(final Creature attacker, double rate, Creature target)
	{
		return calcCrit(attacker, rate, target, null);
	}
	
	// ADDED BY VEGA
	public static final int calcBlowChance(Creature activeChar, Creature target, int chance)
	{
		if (target instanceof Player && target.getActingPlayer().isInOlympiadMode())
		{
			if (target.getActingPlayer().isWearingHeavyArmor())
			{
				if (activeChar.isBehind(target))
					return 100;
			}
		}
		else if (target instanceof Attackable)
			return (int) activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 19) / 100), target, null);
		
		return (int) activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 21) / 100), target, null);
	}
	
	/**
	 * Calcul value of blow success
	 * @param activeChar The character delaing the blow.
	 * @param target The victim.
	 * @param chance The base chance of landing a blow.
	 * @return true if successful, false otherwise
	 */
	public static final boolean calcBlow(Creature activeChar, Creature target, int chance)
	{
		// return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null) > Rnd.get(100);
		if (target instanceof Player && target.getActingPlayer().isInOlympiadMode())
		{
			if (target.getActingPlayer().isWearingHeavyArmor())
			{
				if (activeChar.isBehind(target))
					return true;
				else if (Rnd.get(100) < 50)
					return true;
			}
		}
		else if (target instanceof Attackable)
			return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 19) / 100), target, null) > Rnd.get(100);
		
		return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 21) / 100), target, null) > Rnd.get(100);
		
	}
	
	/**
	 * Calcul value of lethal chance
	 * @param activeChar The character delaing the blow.
	 * @param target The victim.
	 * @param baseLethal The base lethal chance of the skill.
	 * @param magiclvl
	 * @return
	 */
	public static final double calcLethal(Creature activeChar, Creature target, int baseLethal, int magiclvl)
	{
		double chance = 0;
		int level = 80;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + level) / 2) - 1 - level;
			
			// delta [-3,infinite)
			if (delta >= -3)
				chance = (baseLethal * ((double) level / level));
			
			// delta [-9, -3[
			else if (delta < -3 && delta >= -9)
			{
				// baseLethal
				// chance = -1 * -----------
				// (delta / 3)
				chance = (-3) * (baseLethal / (delta));
			}
			// delta [-infinite,-9[
			else
				chance = baseLethal / 15;
		}
		else
			chance = (baseLethal * ((double) level / level));
		
		return 10 * activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
	}
	
	public static final boolean calcLethalHit(Creature activeChar, Creature target, L2Skill skill)
	{
		if (target.calcStat(Stats.LETHAL_IMMUNITY, 0, null, null) > 0)
			return false;
		
		if (target instanceof Playable)
		{
			if (target.getActingPlayer().isInvul())
				return false;
			
			if (activeChar instanceof Playable)
			{
				if (!activeChar.getActingPlayer().isGM())
				{
					if (!target.getActingPlayer().isDebuffable(activeChar.getActingPlayer()))
						return false;
				}
			}
			
			final int chance = Rnd.get(1000);
			
			// 2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
			if (skill.getLethalChance2() > 0 && chance < calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
				
				if (target instanceof Player) // If is a active player set his HP and CP to 1
				{
					target.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
					
					if (!target.getActingPlayer().isCursedWeaponEquipped())
						target.setCurrentHp(1);
					else
						target.setCurrentHp(target.getCurrentHp() / 2);
				}
				else
					target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar, skill);
			}
			else if (skill.getLethalChance1() > 0 && chance < calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.HALF_KILL));
				
				if (target instanceof Player)
				{
					target.sendPacket(new SystemMessage(SystemMessageId.CP_DISAPPEARS_WHEN_HIT_WITH_A_HALF_KILL_SKILL));
					
					if (!target.getActingPlayer().isCursedWeaponEquipped())
						target.setCurrentCp(1); // Set CP to 1
					else
						target.setCurrentCp(target.getCurrentCp() / 2);
				}
				else // If is a monster remove first damage and after 50% of current hp
					target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar, skill);
			}
			else
				return false;
		}
		
		return false;
	}
	
	public static final boolean calcMCrit(double mRate, Creature target)
	{
		final int evade = (int) target.calcStat(Stats.M_CRIT_DAMAGE_EVASION, 0, null, null);
		
		boolean evaded = evade >= 100;
		
		if (!evaded && evade > 0)
			
			evaded = evade > Rnd.get(100);
		
		return !evaded && mRate > Rnd.get(1000);
	}
	
	/**
	 * Calculate delay (in milliseconds) before next ATTACK.
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return delay in ms.
	 */
	public static final int calcPAtkSpd(Creature attacker, Creature target, double rate)
	{
		if (rate < 2)
			return 2700;
		
		return (int) (561000 / rate);
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast.
	 * @param attacker
	 * @param skill used to know if skill is magic or no.
	 * @param skillTime
	 * @return delay in ms.
	 */
	public static final int calcAtkSpd(Creature attacker, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			if (attacker instanceof Player)
			{
				if (!attacker.getActingPlayer().isMageClass())
				{
					if (attacker.getActingPlayer().isInOlympiadMode())
						return (int) (skillTime * 320 / attacker.getMAtkSpd(skill));
					
					return (int) (skillTime * 170 / attacker.getMAtkSpd(skill));
				}
			}
			else
				return (int) (skillTime * 160 / attacker.getMAtkSpd(skill));
			
			return (int) (skillTime * 290 / attacker.getMAtkSpd(skill));
		}
		
		if (attacker instanceof Attackable)
			return (int) (skillTime * 350 / attacker.getPAtkSpd(skill));
		
		return (int) (skillTime * 440 / attacker.getPAtkSpd(skill));
	}
	
	public static boolean calcHitMiss(Creature attacker, Creature target)
	{
		return calcHitMiss(attacker, target, 0);
	}
	
	/**
	 * Calculate the hit/miss chance.
	 * @param attacker : The attacker to make checks on.
	 * @param target : The target to make checks on.
	 * @param accuracyPenalty
	 * @return true if hit is missed, false if it evaded.
	 */
	public static boolean calcHitMiss(Creature attacker, Creature target, int accuracyPenalty)
	{
		final int absolute_evasion = (int) target.calcStat(Stats.EVASION_ABSOLUTE, 0, attacker, null);
		
		if (absolute_evasion > 0)
		{
			if (Rnd.get(100) < absolute_evasion)
				return true;
		}
		
		final int delta = attacker.getAccuracy(target) - target.getEvasionRate(attacker) - accuracyPenalty;
		
		int chance;
		
		if (delta >= 10)
			chance = 980;
		else
		{
			switch (delta)
			{
				case 9:
					chance = 975;
					break;
				case 8:
					chance = 970;
					break;
				case 7:
					chance = 965;
					break;
				case 6:
					chance = 960;
					break;
				case 5:
					chance = 955;
					break;
				case 4:
					chance = 945;
					break;
				case 3:
					chance = 935;
					break;
				case 2:
					chance = 925;
					break;
				case 1:
					chance = 915;
					break;
				case 0:
					chance = 905;
					break;
				case -1:
					chance = 890;
					break;
				case -2:
					chance = 875;
					break;
				case -3:
					chance = 860;
					break;
				case -4:
					chance = 845;
					break;
				case -5:
					chance = 830;
					break;
				case -6:
					chance = 815;
					break;
				case -7:
					chance = 800;
					break;
				case -8:
					chance = 785;
					break;
				case -9:
					chance = 770;
					break;
				case -10:
					chance = 755;
					break;
				case -11:
					chance = 735;
					break;
				case -12:
					chance = 715;
					break;
				case -13:
					chance = 695;
					break;
				case -14:
					chance = 675;
					break;
				case -15:
					chance = 655;
					break;
				case -16:
					chance = 625;
					break;
				case -17:
					chance = 595;
					break;
				case -18:
					chance = 565;
					break;
				case -19:
					chance = 535;
					break;
				case -20:
					chance = 505;
					break;
				case -21:
					chance = 455;
					break;
				case -22:
					chance = 405;
					break;
				case -23:
					chance = 355;
					break;
				case -24:
					chance = 305;
					break;
				default:
				{
					if (target.calcStat(Stats.IMPROVED_EVASION, 0, attacker, null) > 0)
					{
						switch (delta)
						{
							case -25:
								chance = 255;
								break;
							case -26:
								chance = 205;
								break;
							case -27:
								chance = 155;
								break;
							case -28:
								chance = 105;
								break;
							case -29:
								chance = 55;
								break;
							default:
								return true;
						}
					}
					else
						chance = 255;
				}
			}
		}
		
		if (!attacker.isInFrontOf(target))
		{
			if (attacker.isBehind(target))
				chance *= 1.2;
			else
				// side
				chance *= 1.1;
			
			if (chance > 980)
				chance = 980;
		}
		
		return chance < Rnd.get(1000);
	}
	
	public static final boolean calcAtkBreak(Creature attacker, Creature target, int damage)
	{
		if (attacker == null || target == null || attacker == target)
			return false;
		
		if (target.isRaid())
			return false; // No attack break
			
		final int dmgThreashold = 1700;
		
		if (attacker instanceof Attackable || damage >= dmgThreashold)
		{
			double init = 0;
			
			if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow() && target.canAbortCast())
			{
				final L2Skill skill = target.getLastSkillCast();
				
				if (!(skill != null && skill.isMagic()))
					return false;
				
				if (skill.getSkillType() == L2SkillType.FUSION)
					return true;
				
				init = Config.SPELL_CANCEL_CHANCE;
				
				if (attacker instanceof Attackable)
				{
					if (skill.isHeal())
					{
						if (target.getActingPlayer().isWearingMagicArmor())
							init += 60;
						else
							init += 30;
					}
					else if (damage < dmgThreashold)
						return false;
				}
				
				init += attacker.calcStat(Stats.SPELL_CANCEL_ADD, 0, target, null);
			}
			else if (Config.ALT_GAME_CANCEL_BOW && damage >= dmgThreashold && target.isAttackingNow() && target.getActiveWeaponItem() != null && target.getActiveWeaponItem().getItemType() == WeaponType.BOW)
			{
				init = Config.ATTACK_CANCEL_CHANCE;
				init += attacker.calcStat(Stats.ATTACK_CANCEL_ADD, 0, target, null);
			}
			else
				return false;
			
			init -= target.calcStat(Stats.SPELL_CANCEL_RES, 0, attacker, null);
			
			if (init < 1)
				return false;
			if (init > 99)
				return true;
			
			return Rnd.get(100) < init;
		}
		
		return false;
	}
	
	/**
	 * Test the shield use.
	 * @param attacker The attacker.
	 * @param target The victim ; make check about his shield.
	 * @param skill The skill the attacker has used.
	 * @param sendSysMsg
	 * @return 0 = shield defense doesn't succeed<br>
	 *         1 = shield defense succeed<br>
	 *         2 = perfect block
	 */
	public static byte calcShldUse(Creature attacker, Creature target, L2Skill skill, boolean sendSysMsg)
	{
		// Ignore shield skills types bypass the shield use.
		if (skill != null && skill.ignoreShield())
			return 0;
		
		if (target.isStunned() || target.isSleeping() || target.isParalyzed() || target.isAfraid() || target.isConfused())
			return 0;
		
		if (attacker.calcStat(Stats.IGNORE_SHIELD, 0, null, null) > Rnd.get(100))
			return 0;
		
		final byte overpower = calcOverpower(attacker, target, skill);
		
		if (overpower > 0)
		{
			if (overpower == 1)
			{
				if (target instanceof Player)
					((Player) target).sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
				
				return SHIELD_DEFENSE_SUCCEED;
			}
			if (target instanceof Player)
				((Player) target).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			
			return SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		
		double shldRate = target.getShldRate(attacker, skill);
		
		if (shldRate < 1)
			return 0;
		
		final int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null);
		
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside)))
			return 0;
		
		if (target.isAttackingNow() || target.isCastingNow())
			shldRate /= 1.28;
		
		shldRate = Math.min(shldRate, target.calcStat(Stats.BLOCK_RATE_MAX, 80, attacker, skill));
		
		if (attacker instanceof RaidBoss)
			shldRate /= 1.7;
		
		if (target.isMoving() && target.isRunning())
			shldRate /= 1.5;
		
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside)))
			return 0;
		
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		
		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		if (attacker.getAttackType() == WeaponType.BOW)
			shldRate *= 1.33;
		
		if (shldRate > 0 && 100 - Config.PERFECT_SHIELD_BLOCK_RATE - target.calcStat(Stats.PERF_BLOCK_ADD, 0, null, null) < Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		else if (shldRate > Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}
		
		if (sendSysMsg && target instanceof Player)
		{
			Player enemy = (Player) target;
			
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					enemy.sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}
		
		return shldSuccess;
	}
	
	public static byte calcShldUse(Creature attacker, Creature target, L2Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}
	
	public static byte calcShldUse(Creature attacker, Creature target)
	{
		return calcShldUse(attacker, target, null, true);
	}
	
	public static boolean calcMagicAffected(Creature actor, Creature target, L2Skill skill)
	{
		L2SkillType type = skill.getSkillType();
		double defence = 0;
		
		if (target.isRaid() && !calcRaidAffected(type))
			return false;
		
		if (skill.isActive() && skill.isOffensive() && !skill.isNeutral())
			defence = target.getMDef(actor, skill);
		
		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(actor, target, skill, type);
		double d = (attack - defence) / (attack + defence);
		
		if (target.isRaid())
		{
			switch (type)
			{
				case CONFUSION:
				case MUTE:
				case PARALYZE:
				case ROOT:
				case FEAR:
				case SLEEP:
				case STUN:
				case DEBUFF:
				case AGGDEBUFF:
					if (d > 0 && Rnd.get(1000) == 1)
						return true;
					return false;
				default:
					break;
			}
		}
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	public static double calcSkillVulnerability(Creature attacker, Creature target, L2Skill skill, L2SkillType type)
	{
	    double multiplier = 1;
	    
	    // Get the elemental damages.
	    if (skill.getElement() > 0)
	        multiplier *= Math.sqrt(calcElemental(attacker, target, skill));
	    
	    // Use a nova função calcSkillTypeVulnerability
	    multiplier = calcSkillTypeVulnerability(multiplier, target, type);
	    
	    // Return a multiplier (exemple with resist shock : 1 + (-0,4 stun vuln) = 0,6%
	    return multiplier;
	}
	
	public static double calcSkillProficiency(L2Skill skill, Creature attacker, Creature target)
	{
	    double multiplier = 1; // initialize...
	    
	    if (skill != null)
	    {
	        // Calculate skilltype vulnerabilities
	        L2SkillType type = skill.getSkillType();
	        
	        // For additional effects on PDAM and MDAM skills (like STUN, SHOCK, PARALYZE...)
	        if (type != null && (type == L2SkillType.PDAM || type == L2SkillType.MDAM || type == L2SkillType.DRAIN || 
	                             type == L2SkillType.BLOW || type == L2SkillType.DEATHLINK || type == L2SkillType.CHARGEDAM || 
	                             type == L2SkillType.CPDAMPERCENT || type == L2SkillType.FATAL))
	            type = skill.getEffectType();
	        
	        multiplier = calcSkillTypeProficiency(multiplier, attacker, target, type);
	    }
	    
	    return multiplier;
	}
	
	public static double calcSkillTypeProficiency(double multiplier, Creature attacker, Creature target, L2SkillType type)
	{
	    if (type != null)
	    {
	        switch (type)
	        {
	            case BLEED:
	                multiplier = attacker.calcStat(Stats.BLEED_PROF, multiplier, target, null);
	                break;
	            case POISON:
	                multiplier = attacker.calcStat(Stats.POISON_PROF, multiplier, target, null);
	                break;
	            case STUN:
	                multiplier = attacker.calcStat(Stats.STUN_PROF, multiplier, target, null);
	                break;
	            case PARALYZE:
	                multiplier = attacker.calcStat(Stats.PARALYZE_PROF, multiplier, target, null);
	                break;
	            case ROOT:
	                multiplier = attacker.calcStat(Stats.ROOT_PROF, multiplier, target, null);
	                break;
	            case SLEEP:
	                multiplier = attacker.calcStat(Stats.SLEEP_PROF, multiplier, target, null);
	                break;
	            case MUTE:
	            case FEAR:
	            case BETRAY:
	            case AGGREDUCE_CHAR:
	            case ERASE:
	            case AGGDEBUFF:
	                multiplier = attacker.calcStat(Stats.DERANGEMENT_PROF, multiplier, target, null);
	                break;
	            case CONFUSION:
	                multiplier = attacker.calcStat(Stats.CONFUSION_PROF, multiplier, target, null);
	                break;
	            case DEBUFF:
	            case WEAKNESS:
	                multiplier = attacker.calcStat(Stats.DEBUFF_PROF, multiplier, target, null);
	                break;
	            default:
	                break;
	        }
	    }
	    
	    return multiplier;
	}
	
	public static double calcSkillTypeVulnerability(double multiplier, Creature target, L2SkillType type)
	{
	    if (type != null)
	    {
	        switch (type)
	        {
	            case BLEED:
	                multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
	                break;
	            case POISON:
	                multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
	                break;
	            case STUN:
	                multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
	                break;
	            case PARALYZE:
	                multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
	                break;
	            case ROOT:
	                multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
	                break;
	            case SLEEP:
	                multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
	                break;
	            case DISARM:
	                multiplier = target.calcStat(Stats.DISARM_VULN, multiplier, target, null);
	                break;
	            case MUTE:
	            case FEAR:
	            case BETRAY:
	            case AGGREDUCE_CHAR:
	            case ERASE:
	            case AGGDEBUFF:
	                multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
	                break;
	            case CONFUSION:
	                multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
	                break;
	            case DEBUFF:
	            case WEAKNESS:
	                multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
	                break;
	            case CANCEL:
	                multiplier = target.calcStat(Stats.CANCEL_VULN, multiplier, target, null);
	                break;
	        }
	    }
	    return multiplier;
	}
	
	public static double calcSkillStatModifier(L2SkillType type, Creature target)
	{
		double multiplier = 1;
		
		if (type == null)
			return multiplier;
		
		try
		{
			switch (type)
			{
				case STUN:
				case BLEED:
				case POISON:
					multiplier = 2 - SQRT_CON_BONUS[target.getStat().getCON()];
					break;
				
				case SLEEP:
				case DEBUFF:
				case WEAKNESS:
				case ERASE:
				case ROOT:
				case MUTE:
				case FEAR:
				case BETRAY:
				case CONFUSION:
				case AGGREDUCE_CHAR:
				case PARALYZE:
					multiplier = 2 - SQRT_MEN_BONUS[target.getStat().getMEN()];
					break;
				default:
					return multiplier;
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			_log.warning("Character " + target.getName() + " has been set (by a GM?) a MEN or CON stat value out of accepted range");
		}
		
		if (multiplier < 0)
			multiplier = 0;
		
		return multiplier;
	}
	
	public static double getSTRBonus(Creature activeChar)
	{
		return STR_BONUS[activeChar.getSTR()];
	}
	
	public static boolean calcEffectSuccess(Creature attacker, Creature target, EffectTemplate effect, L2Skill skill, byte shld)
	{
		int rate = calcEffectSuccessChance(attacker, target, effect, skill, shld);
		if (rate <= 0)
			return false;
		
		return Rnd.get(100) < rate;
	}
	
	@SuppressWarnings("null")
	public static int calcEffectSuccessChance(Creature attacker, Creature target, EffectTemplate effect, L2Skill skill, byte shld)
	{
	    if (shld == SHIELD_DEFENSE_PERFECT_BLOCK && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) != null)
	        return 0;
	    
	    else if (skill.isOffensive() && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) != null)
	        return 0;
	    
	    else
	    {
	        L2SkillType type = effect.effectType != null ? effect.effectType : skill.getSkillType();
	        
	        // Guarda as resistências para aplicar DEPOIS
	        double resModifier = calcSkillTypeVulnerability(1, target, type);
	        // REMOVIDO: if (resModifier <= 0) return 0;
	        
	        int value = (int) effect.effectPower;
	        value = ((int) attacker.calcStat(Stats.EFFECT_POWER_BOOST, value, (Creature) null, skill));
	        
	        // SÓ statmodifier aqui (resistência base por stats)
	        double statmodifier = calcSkillStatModifier(type, target);
	        
	        int ssmodifier = 160;
	        
	        if (attacker instanceof Monster)
	        {
	            if (attacker instanceof RaidBoss || attacker.getLevel() >= 85)
	                return 100;
	            
	            if (attacker.getLevel() < 80)
	                return 0;
	        }
	        else if (skill.getMagicLevel() > 0 && skill.getMagicLevel() < 74)
	            statmodifier *= 0.5D;
	        
	        // Cálculo base
	        int rate = (int) (value * statmodifier);
	        
	        // Ajuste M.Atk vs M.Def
	        if (skill.isMagic())
	        {
	            if (attacker instanceof Player && !attacker.getActingPlayer().isMageClass())
	                rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + (double) (9000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0))), 0.2D));
	            else
	                rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + (double) (4000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0))), 0.2D));
	        }
	        
	        // SS modifier
	        if (rate > 10000 / (100 + ssmodifier))
	            rate = 100 - (100 - rate) * 100 / ssmodifier;
	        else
	            rate = rate * ssmodifier / 100;
	        
	        // Caps básicos
	        if (rate > 90)
	            rate = 90;
	        else if (rate < 10)
	            rate = 10;
	        
	        // APLICA AS RESISTÊNCIAS
	        rate = (int) (rate * resModifier * calcSkillTypeProficiency(1, attacker, target, type));
	        
	        int maxLandChance = skill.getMaxLandChance();
	        int minLandChance = skill.getMinLandChance();
	        boolean isRB = false;
	        if (target instanceof Attackable && (target instanceof RaidBoss || target.getLevel() >= 86))
	            isRB = true;
	        
	        boolean lionheart = false;
	        if (target != null && (target.getFirstEffect(287) != null || target instanceof Player && target.getActingPlayer().getCurrentSkill() != null && target.getActingPlayer().getCurrentSkill().getSkillId() == 246))
	            lionheart = true;
	        
	        int maxChance = maxLandChance == 0 ? (int) attacker.calcStat(Stats.MAX_LAND_RATE, 69.0D, target, skill) : (int) attacker.calcStat(Stats.MAX_LAND_RATE, maxLandChance, target, skill);
	        int minChance = minLandChance == 0 ? (int) target.calcStat(Stats.MIN_LAND_RATE, 21.0D, attacker, skill) : (int) target.calcStat(Stats.MIN_LAND_RATE, minLandChance, attacker, skill);
	        
	        if (rate > maxChance)
	            rate = maxChance;
	        else if (rate < minChance && !isRB && !lionheart)
	            rate = minChance;
	        
	        if (isRB)
	            rate = (int) (rate / 1.5D);
	        
	        if (skill.getOlyNerf() > 0 && target instanceof Player && target.getActingPlayer().isInOlympiadMode())
	        {
	            if (skill.isDamage())
	                rate -= skill.getOlyNerf() / 100;
	            else
	                rate -= skill.getOlyNerf();
	            
	            if (rate < 21)
	                rate = 21;
	        }
	        
	        if (target instanceof RaidBoss)
	            rate = 0;
	        
	        return rate;
	    }
	}
	
	public static boolean calcSkillSuccess(Creature attacker, Creature target, L2Skill skill, byte shld)
	{
		int rate = calcSkillSuccessChance(attacker, target, skill, shld);
		if (rate <= 0)
			return false;
		return Rnd.get(100) < rate;
	}
	
	public static int calcSkillSuccessChance(Creature attacker, Creature target, L2Skill skill, byte shld)
	{
	    if (skill.ignoreResists())
	        return (int) skill.getPower(attacker);
	    
	    else if (shld == SHIELD_DEFENSE_PERFECT_BLOCK && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) != null)
	        return 0;
	    
	    else if (skill.isOffensive() && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) != null)
	        return 0;
	    
	    else
	    {
	        L2SkillType type = skill.getEffectType();
	        
	        // Guarda as resistências para aplicar DEPOIS
	        double resModifier = calcSkillTypeVulnerability(1, target, type);
	        // REMOVIDO: if (resModifier <= 0) return 0;
	        
	        int value = (int) skill.getPower(attacker);
	        
	        if (value == 0)
	            value = type == L2SkillType.PARALYZE ? 40 : (type == L2SkillType.FEAR ? 50 : 92);
	        
	        // SÓ statmodifier aqui
	        double statmodifier = calcSkillStatModifier(type, target);
	        int ssmodifier = 163;
	        
	        if (attacker instanceof Monster)
	        {
	            if (attacker instanceof RaidBoss || attacker.getLevel() >= 85)
	                return 100;
	            
	            if (attacker.getLevel() < 80)
	                return 0;
	        }
	        else if (skill.getMagicLevel() > 0 && skill.getMagicLevel() < 74)
	            statmodifier *= 0.5D;
	        
	        // Cálculo base
	        int rate = (int) (value * statmodifier);
	        
	        // Ajuste M.Atk vs M.Def
	        if (skill.isMagic())
	        {
	            if (attacker instanceof Player && !attacker.getActingPlayer().isMageClass())
	                rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + (double) (9000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0))), 0.2D));
	            else
	                rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + (double) (5000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0))), 0.2D));
	        }
	        
	        // SS modifier
	        if (ssmodifier != 100)
	        {
	            if (rate > 10000 / (100 + ssmodifier))
	                rate = 100 - (100 - rate) * 100 / ssmodifier;
	            else
	                rate = rate * ssmodifier / 100;
	        }
	        
	        // Caps básicos
	        if (rate > 95)
	            rate = 95;
	        else if (rate < 10)
	            rate = 10;
	        
	        int maxLandChance = skill.getMaxLandChance();
	        int minLandChance = skill.getMinLandChance();
	        boolean isRB = false;
	        boolean isLonis = false;
	        
	        if (target instanceof Attackable && (target instanceof RaidBoss || target.getLevel() >= 86))
	            isRB = true;
	        
	        if (target instanceof Attackable && target instanceof RaidBoss)
	            isLonis = true;
	        
	        boolean lionheart = false;
	        if (target.getFirstEffect(287) != null || target instanceof Player && target.getActingPlayer().getCurrentSkill() != null && target.getActingPlayer().getCurrentSkill().getSkillId() == 246)
	            lionheart = true;
	        
	        int maxChance = maxLandChance == 0 ? (int) attacker.calcStat(Stats.MAX_LAND_RATE, 69.0D, target, skill) : (int) attacker.calcStat(Stats.MAX_LAND_RATE, maxLandChance, target, skill);
	        
	        if (minLandChance > maxChance)
	            minLandChance = maxChance / 2;
	        
	        // Primeiro cap
	        if (rate > maxChance)
	            rate = maxChance;
	        
	        // APLICA AS RESISTÊNCIAS
	        rate = (int) (rate * resModifier * calcSkillProficiency(skill, attacker, target));
	        
	        // Segundo cap
	        if (rate > maxChance)
	            rate = maxChance;
	        
	        int minChance = minLandChance == 0 ? (int) target.calcStat(Stats.MIN_LAND_RATE, 21.0D, attacker, skill) : (int) target.calcStat(Stats.MIN_LAND_RATE, minLandChance, attacker, skill);
	        
	        if (minChance < 21)
	            minChance = 21;
	        
	        if (rate < minChance && !isRB && !lionheart)
	            rate = minChance;
	        
	        if (isRB)
	            rate = (int) (rate / 1.5D);
	        
	        if (skill.getOlyNerf() > 0 && target instanceof Player && target.getActingPlayer().isInOlympiadMode())
	        {
	            if (skill.isDamage())
	                rate -= skill.getOlyNerf() / 100;
	            else
	                rate -= skill.getOlyNerf();
	            
	            if (rate < 10)
	                rate = 10;
	        }
	        
	        if (target instanceof Monster && !isLonis && rate < 25)
	            rate = 25;
	        
	        if (rate == 69)
	            rate = 55;
	        
	        if ((target instanceof RaidBoss) 
	            && (type == L2SkillType.PARALYZE || type == L2SkillType.STUN || type == L2SkillType.FEAR 
	            || type == L2SkillType.CONFUSION || type == L2SkillType.ROOT || type == L2SkillType.SLEEP
	            || type == L2SkillType.MUTE || type == L2SkillType.WEAKNESS || type == L2SkillType.PARALYZE
	            || type == L2SkillType.SWITCH  || type == L2SkillType.AGGREDUCE || type == L2SkillType.AGGREMOVE))
	            rate = 0;
	        
	        return rate;
	    }
	}
	
	public static boolean calcCubicSkillSuccess(Cubic attacker, Creature target, L2Skill skill, byte shld)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) != null)
			return false;
		
		else if (skill.isOffensive() && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) != null)
			return false;
		
		else
		{
			L2SkillType type = skill.getSkillType();
			double resmodifier = calcSkillVulnerability(attacker.getOwner(), target, skill, type);
			
			if (resmodifier <= 0.0D)
				return false;
			
			if (target.isRaid())
			{
				switch (type)
				{
					case STUN:
					case SLEEP:
					case DEBUFF:
					case ROOT:
					case MUTE:
					case FEAR:
					case CONFUSION:
					case PARALYZE:
					case AGGDEBUFF:
						return false;
					case BLEED:
					case POISON:
					case WEAKNESS:
					case ERASE:
					case BETRAY:
					case AGGREDUCE_CHAR:
				}
			}
			
			if (calcSkillReflect((Creature) null, target, skill) != 0)
				return false;
			
			int value = (int) skill.getPower();
			int lvlDepend = skill.getLevelDepend();
			
			if (value == 0)
				value = (type == L2SkillType.PARALYZE) ? 50 : (type == L2SkillType.FEAR) ? 40 : 80;
			
			if (lvlDepend == 0)
				lvlDepend = (type == L2SkillType.PARALYZE || type == L2SkillType.FEAR) ? 1 : 2;
			
			double statmodifier = calcSkillStatModifier(type, target);
			
			int rate = (int) (value * statmodifier * resmodifier);
			
			if (skill.isMagic())
				rate += (int) (Math.pow((double) attacker.getMAtk() / (double) (target.getMDef(attacker.getOwner(), skill) + (shld == 1 ? target.getShldDef() : 0)), 0.2D) * 100.0D) - 100;
			
			if (rate > 99)
				rate = 99;
			
			else if (rate < 1)
				rate = 1;
			
			if (skill.getOlyNerf() > 0 && target instanceof Player && target.getActingPlayer().isInOlympiadMode())
			{
				if (skill.isDamage())
					rate -= skill.getOlyNerf() / 100;
				else
					rate -= skill.getOlyNerf();
				
				if (rate < 10)
					rate = 10;
			}
			
			return Rnd.get(100) < rate;
		}
	}
	
	public static boolean calcMagicSuccess()
	{
		return (Rnd.get(100) > 6);
	}
	
	public static double calcManaDam(Creature attacker, Creature target, L2Skill skill, boolean ss, boolean bss)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		
		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;
		
		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker) * (mp / 97)) / mDef;
		final L2SkillType type = skill.getSkillType();
		
		damage *= calcSkillVulnerability(attacker, target, skill, type);
		return damage;
	}
	
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster)
	{
		if (baseRestorePercent == 0 || baseRestorePercent == 100)
			return baseRestorePercent;
		
		double restorePercent = baseRestorePercent * WIT_BONUS[caster.getWIT()];
		if (restorePercent - baseRestorePercent > 20.0)
			restorePercent += 20.0;
		
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		
		return restorePercent;
	}
	
	public static boolean calcPhysicalSkillEvasion(Creature caster, Creature target, L2Skill skill)
	{
		if (skill.isMagic() && skill.getSkillType() != L2SkillType.BLOW)
			return false;
		
		int bonus = 0;
		
		if (caster.isBehind(target))
			bonus = 6;
		
		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill) - caster.calcStat(Stats.P_SKILL_EVASION_REDUCTION, 0, target, skill) - bonus;
	}
	
	public static boolean calcMagicalSkillEvasion(Creature caster, Creature target, L2Skill skill)
	{
		if (!skill.isMagic())
			return false;
		
		int bonus = 0;
		
		if (caster.isBehind(target))
			bonus = 6;
		
		return Rnd.get(100) < target.calcStat(Stats.M_SKILL_EVASION, 0, caster, skill) - caster.calcStat(Stats.M_SKILL_EVASION_REDUCTION, 0, target, skill) - bonus;
	}
	
	public static boolean calcSkillMastery(Creature actor, L2Skill sk)
	{
		// Pointless check for Creature other than players, as initial value will stay 0.
		if (!(actor instanceof Player))
			return false;
		
		if (actor instanceof Buffer)
			return false;
		
		if (sk.getSkillType() == L2SkillType.FISHING)
			return false;
		
		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);
		
		if (((Player) actor).isMageClass())
			val *= INT_BONUS[actor.getINT()];
		else
			val *= STR_BONUS[actor.getSTR()];
		
		return Rnd.get(100) < val;
	}
	
	public static double calcValakasAttribute(Creature attacker, Creature target, L2Skill skill)
	{
		double calcPower = 0;
		double calcDefen = 0;
		
		if (skill != null && skill.getAttributeName().contains("valakas"))
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
		}
		else
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			if (calcPower > 0)
			{
				calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
				calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
			}
		}
		return calcPower - calcDefen;
	}
	
	/**
	 * Calculate elemental modifier. There are 2 possible cases :
	 * <ul>
	 * <li>the check emanates from a skill : the result will be a multiplier, including an amount of attacker element, and the target vuln/prof.</li>
	 * <li>the check emanates from a weapon : the result is an addition of all elements, lowered/enhanced by the target vuln/prof</li>
	 * </ul>
	 * @param attacker : The attacker used to retrieve elemental attacks.
	 * @param target : The victim used to retrieve elemental protections.
	 * @param skill : If different of null, it will be considered as a skill resist check.
	 * @return A multiplier or a sum of damages.
	 */
	public static double calcElemental(Creature attacker, Creature target, L2Skill skill)
	{
		int calcPower = 0;
		int calcDefen = 0;
		int calcTotal = 0;
		double result = 1.0;
		byte element;
		
		if (skill != null)
		{
			element = skill.getElement();
			if (element >= 0)
			{
				calcPower = (int) (skill.getElementPower() + attacker.calcStat(Stats.ELEMENTAL_POWER, 1, target, skill));
				calcDefen = target.getDefenseElementValue(element);
				
				if (attacker.getAttackElement() == element)
				{
					calcPower += attacker.getAttackElementValue(element);
				}
				
				calcTotal = calcPower - calcDefen;
				
				if (calcTotal > 0)
				{
					if (calcTotal < 75)
						result += calcTotal * 0.0035;
					else if (calcTotal < 150)
						result = 1.28;
					else if (calcTotal < 200)
						result = 1.31;
					else if (calcTotal < 250)
						result = 1.34;
					else if (calcTotal < 290)
						result = 1.37;
					else if (calcTotal < 300)
						result = 1.4;
					else if (calcTotal < 330)
						result = 1.43;
					else if (calcTotal < 360)
						result = 1.46;
					else if (calcTotal < 410)
						result = 1.49;
					else if (calcTotal < 500)
						result = 1.52;
					else if (calcTotal < 700)
						result = 1.55;
					else
						result = 1.58;
				}
				else if (calcTotal < -110)
				{
					if (attacker instanceof Npc)
					{
						if (calcTotal <= -170)
						{
							result = 0.8;
						}
						else
						{
							result = 1 - ((170 + calcTotal) * 0.0033d);
						}
					}
				}
				
				if (Config.DEVELOPER)
				{
					_log.info(skill.getName() + ": " + calcPower + ", " + calcDefen + ", " + result + " Total: " + calcTotal);
				}
			}
		}
		else
		{
			element = attacker.getAttackElement();
			if (element >= 0)
			{
				calcPower = (int) (attacker.getAttackElementValue(element) + attacker.calcStat(Stats.ELEMENTAL_POWER, 1, target, null));
				calcDefen = target.getDefenseElementValue(element);
				
				calcTotal = calcPower - calcDefen;
				if (calcTotal < -110)
				{
					if (attacker instanceof Npc)
					{
						if (calcTotal <= -170)
						{
							result = 0.8;
						}
						else
						{
							result = 1 - ((170 + calcTotal) * 0.0033d);
						}
					}
				}
				else if (calcTotal > 0)
				{
					if (calcTotal < 75)
						result += calcTotal * 0.0035;
					else if (calcTotal < 150)
						result = 1.28;
					else if (calcTotal < 200)
						result = 1.31;
					else if (calcTotal < 250)
						result = 1.34;
					else if (calcTotal < 290)
						result = 1.37;
					else if (calcTotal < 300)
						result = 1.4;
					else if (calcTotal < 330)
						result = 1.43;
					else if (calcTotal < 360)
						result = 1.46;
					else if (calcTotal < 410)
						result = 1.49;
					else if (calcTotal < 500)
						result = 1.52;
					else if (calcTotal < 700)
						result = 1.55;
					else
						result = 1.58;
				}
				
				if (Config.DEVELOPER)
				{
					_log.info("Hit: " + calcPower + ", " + calcDefen + ", " + result + " Total: " + calcTotal);
				}
			}
		}
		return result;
	}
	
	/**
	 * Calculate skill reflection according to these three possibilities:
	 * <ul>
	 * <li>Reflect failed</li>
	 * <li>Normal reflect (just effects).</li>
	 * <li>Vengeance reflect (100% damage reflected but damage is also dealt to actor).</li>
	 * </ul>
	 * @param attacker
	 * @param target : The skill's target.
	 * @param skill : The skill to test.
	 * @return SKILL_REFLECTED_FAILED, SKILL_REFLECT_SUCCEED or SKILL_REFLECT_VENGEANCE
	 */
	@SuppressWarnings(
	{
		"incomplete-switch",
		"null"
	})
	public static byte calcSkillReflect(Creature attacker, Creature target, L2Skill skill)
	{
		// Some special skills (like hero debuffs...) or ignoring resistances skills can't be reflected.
		if (skill == null || skill.ignoreResists() || !skill.canBeReflected() || (attacker != null && attacker.isInvul()))
			return SKILL_REFLECT_FAILED;
		
		// Only magic and melee skills can be reflected.
		if (!skill.isMagic() && (skill.getCastRange(attacker) == -1 || skill.getCastRange(attacker) > MELEE_ATTACK_RANGE))
			return SKILL_REFLECT_FAILED;
		
		if (attacker instanceof Monster)
		{
			if (attacker.getLevel() >= 85 || attacker instanceof RaidBoss)
				return SKILL_REFLECT_FAILED;
		}
		
		final int range = (int) Util.calculateDistance(attacker, target, false);
		boolean rangedPhysicalSkill = range > MELEE_ATTACK_RANGE;
		
		byte reflect = SKILL_REFLECT_FAILED;
		
		// Check for non-reflected skilltypes, need additional retail check.
		switch (skill.getSkillType())
		{
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
				return SKILL_REFLECT_FAILED;
			
			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
			case MANADAM:
			case FATAL:
				final Stats stat = skill.isMagic() ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : rangedPhysicalSkill ? Stats.VENGEANCE_RANGED_SKILL_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE;
				final double venganceChance = target.getStat().calcStat(stat, 0, target, skill);
				if (venganceChance > Rnd.get(100))
				{
					reflect |= SKILL_REFLECT_VENGEANCE;
					
					if (attacker != null && target != null && target instanceof Player && skill.isDamage())
						target.sendMessage("You reflected " + attacker.getName() + "'s " + skill.getName() + "'s damage");
				}
				break;
		}
		
		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : rangedPhysicalSkill ? Stats.VENGEANCE_RANGED_SKILL_DAMAGE : Stats.REFLECT_SKILL_PHYSIC, 0, attacker, skill);
		
		if (Rnd.get(100) < reflectChance)
		{
			reflect |= SKILL_REFLECT_SUCCEED;
			
			if (attacker != null && target != null && target instanceof Player && (skill.isDebuff() || (skill.isOffensive() && !skill.isDamage())))
				target.sendMessage("You reflected " + attacker.getName() + "'s " + skill.getName());
		}
		
		return reflect;
	}
	
	/**
	 * @param cha : The character affected.
	 * @param fallHeight : The height the NPC fallen.
	 * @return the damage, based on max HPs and falling height.
	 */
	public static double calcFallDam(Creature cha, int fallHeight)
	{
		if (!Config.ENABLE_FALLING_DAMAGE || fallHeight < 0)
			return 0;
		
		return cha.calcStat(Stats.FALL, fallHeight * cha.getMaxHp() / 1000, null, null);
	}
	
	/**
	 * @param type : The L2SkillType to test.
	 * @return true if the L2SkillType can affect a raid boss, false otherwise.
	 */
	@SuppressWarnings("incomplete-switch")
	public static boolean calcRaidAffected(L2SkillType type)
	{
		switch (type)
		{
			case MANADAM:
			case MDOT:
				return true;
			
			case CONFUSION:
			case ROOT:
			case STUN:
			case MUTE:
			case FEAR:
			case DEBUFF:
			case PARALYZE:
			case SLEEP:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
				if (Rnd.get(1000) == 1)
					return true;
		}
		return false;
	}
	
	/**
	 * Calculates karma lost upon death.
	 * @param playerLevel : The level of the PKer.
	 * @param exp : The amount of xp earned.
	 * @return The amount of karma player has lost.
	 */
	public static int calculateKarmaLost(int playerLevel, long exp)
	{
	    int level = Math.max(0, Math.min(playerLevel, karmaMods.length - 1));
	    double mod = karmaMods[level];

	    return (mod > 0) ? (int) (exp / mod / 15) : 0;
	}
	
	/**
	 * Calculates karma gain upon player kill.
	 * @param pkCount : The current number of PK kills.
	 * @param isSummon : Does the victim is a summon or no (lesser karma gain if true).
	 * @return karma points that will be added to the player.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon)
	{
		int result = 14400;
		if (pkCount < 100)
			result = (int) (((((pkCount - 1) * 0.5) + 1) * 60) * 4);
		else if (pkCount < 180)
			result = (int) (((((pkCount + 1) * 0.125) + 37.5) * 60) * 4);
		
		if (isSummon)
			result = ((pkCount & 3) + result) >> 2;
		
		return result;
	}
	
	// ADDED BY VEGA
	public static final boolean calcMCritHeal(double mRate)
	{
		return mRate > Rnd.get(1500);
	}
	
	private static byte calcOverpower(Creature attacker, Creature target, L2Skill skill)
	{
		if (attacker instanceof Playable && !attacker.getActingPlayer().isInOlympiadMode())
		{
			int overpower = (int) target.calcStat(Stats.OVERPOWER, 0, attacker, skill);
			
			if (skill != null && !skill.isMagic())
				overpower /= 2;
			
			if (overpower > 0)
			{
				final int attackerOverpower = (int) attacker.calcStat(Stats.OVERPOWER, 0, target, skill);
				
				overpower -= attackerOverpower;
				
				if (overpower > 0)
				{
					try
					{
						if ((target.isAttackingNowOverpower() && target.getAI().getAttackTarget() == attacker) || (target.isCastingNow() && target.getAI().getSkill().isDamage() && target.getAI().getCastTarget() == attacker))
						{
							if (Rnd.get(100) < overpower)
							{
								if (skill != null && skill.isMagic())
									return 1;
								return 2;
							}
						}
					}
					catch (Exception e)
					{
					}
				}
			}
		}
		
		return 0;
	}
	
	public static float calcWeaponResistanceModifier(Weapon weapon, Creature target, L2Skill skill)
	{
		float multi = 1;
		
		if (weapon.getWeight() >= 5)
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_S, 0, target, skill)) / 100;
		
		else if (weapon.getWeight() == 4)
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_UNIQUE, 0, target, skill)) / 100;
		
		else if (weapon.getWeight() == 3)
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_EPIC, 0, target, skill)) / 100;
		
		else if (weapon.getWeight() == 2)
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_LEGENDARY, 0, target, skill)) / 100;
		
		else if (weapon.getWeight() == 1)
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_RELIC, 0, target, skill)) / 100;
		
		return multi;
	}
	
	public static Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];
		
		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		
		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		
		return std;
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast
	 * @param attacker
	 * @param skill
	 * @param skillTime
	 * @return
	 */
	public static final int calcSkillCastTime(Creature attacker, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			if (attacker instanceof Player)
			{
				if (!attacker.getActingPlayer().isMageClass())
				{
					if (attacker.getActingPlayer().isInOlympiadMode())
						return (int) (skillTime * 320 / attacker.getMAtkSpd(skill));
					
					return (int) (skillTime * 170 / attacker.getMAtkSpd(skill));
				}
			}
			else
				return (int) (skillTime * 160 / attacker.getMAtkSpd(skill));
			
			return (int) (skillTime * 290 / attacker.getMAtkSpd(skill));
		}
		
		if (attacker instanceof Attackable)
			return (int) (skillTime * 350 / attacker.getPAtkSpd(skill));
		
		return (int) (skillTime * 440 / attacker.getPAtkSpd(skill));
	}
}