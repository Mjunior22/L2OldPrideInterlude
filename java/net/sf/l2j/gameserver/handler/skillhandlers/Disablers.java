package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectBuff;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class Disablers implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.STUN,
		L2SkillType.ROOT, 
		L2SkillType.SLEEP,
		L2SkillType.CONFUSION,
		L2SkillType.AGGDAMAGE,
		L2SkillType.AGGREDUCE,
		L2SkillType.AGGREDUCE_CHAR,
		L2SkillType.AGGREMOVE,
		L2SkillType.MUTE,
		L2SkillType.FAKE_DEATH,
		L2SkillType.NEGATE,
		L2SkillType.CANCEL_DEBUFF,
		L2SkillType.PARALYZE,
		L2SkillType.ERASE,
		L2SkillType.BETRAY,
		L2SkillType.DISARM,
		L2SkillType.PROC,
		L2SkillType.SWITCH,
		L2SkillType.STEAL_BUFF
	};

	@SuppressWarnings("null")
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		L2SkillType type = skill.getSkillType();

		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;

			Creature target = (Creature) obj;
			if (target.isDead() || (target.isInvul() && !target.isParalyzed())) // bypass if target is dead or invul (excluding invul from Petrification)
				continue;

			if (skill.isOffensive() && target.isPreventedFromReceivingDebuffs())
				continue;

			byte shld = Formulas.calcShldUse(activeChar, target, skill);

			switch (type)
			{
				case BETRAY:
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					break;

				case FAKE_DEATH:
					// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					break;
				
				case DISARM:
				{
					if (target instanceof Player)
					{
						if (target.getActingPlayer().isCombatFlagEquipped())
						{
							if (activeChar.getTarget() != null && target == activeChar.getTarget())
								activeChar.sendMessage("You cannot disarm combat flags");
							
							return;
						}
					}
				}

				case ROOT:
				case STUN:
					if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;

				case SLEEP:
				case PARALYZE: // use same as root for now
					if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;

				case MUTE:
					if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
					{
						// stop same type effect if available
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getSkill().getSkillType() == type)
								e.exit();
						}
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					}
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
					break;

				case CONFUSION:
					// do nothing if not on mob
					if (target instanceof Attackable)
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						{
							L2Effect[] effects = target.getAllEffects();
							for (L2Effect e : effects)
							{
								if (e.getSkill().getSkillType() == type)
									e.exit();
							}
							skill.getEffects(activeChar, target, new Env(shld, true, true, true));
						}
						else
						{
							if (activeChar instanceof Player)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
						}
					}
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					break;

				case AGGDAMAGE: 
					// if (target instanceof Attackable)
					// target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					//
					// skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					// break;

					if (target instanceof Attackable)
					{
						try
						{
							if (!activeChar.isGM() && target.getAI().getAttackTarget() != null)
							{
								Creature mobAttackTarget = target.getAI().getAttackTarget();
								
								if (mobAttackTarget.getActingPlayer() != activeChar.getActingPlayer()/* || (activeChar.getPet() != null && targetb != activeChar.getPet()) */)
								{
									if ((activeChar.getActingPlayer().getClanId() == 0 && mobAttackTarget.getActingPlayer().getClanId() == 0) || activeChar.getActingPlayer().getClanId() != mobAttackTarget.getActingPlayer().getClanId())
									{
										if (activeChar.getParty() == null)
											return;
										if (mobAttackTarget.getParty() == null)
											return;
										if (activeChar.getParty().getPartyLeaderOID() != mobAttackTarget.getParty().getPartyLeaderOID())
											return;
									}
								}
							}
						}
						catch (Exception e)
						{
							break;
						}
						
						int numba = (activeChar instanceof Player && activeChar.getActingPlayer().isTankClass()) ? 152 : 75;
						
						if (target.isRaid())
							numba = 152;
						else if (skill.getId() == 18)
							numba = 50;
						
						final int aggression = (int) ((numba * skill.getPower(activeChar) * activeChar.calcStat(Stats.AGGRESSION_PROF, 1, null, skill)) / target.getLevel());
						
						if (aggression > 0)
							target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, aggression);
						
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					}
					else
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					
					break;

				case AGGREDUCE:
					// these skills needs to be rechecked
					// if (target instanceof Attackable)
					// {
					// skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					//
					// double aggdiff = ((Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((Attackable) target).getHating(activeChar), target, skill);
					//
					// if (skill.getPower() > 0)
					// ((Attackable) target).reduceHate(null, (int) skill.getPower());
					// else if (aggdiff > 0)
					// ((Attackable) target).reduceHate(null, (int) aggdiff);
					// }
					// break;

					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					
					break;

				case AGGREDUCE_CHAR:
					// these skills needs to be rechecked
					// if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, true))
					// {
					// if (target instanceof Attackable)
					// {
					// Attackable targ = (Attackable) target;
					// targ.stopHating(activeChar);
					// if (targ.getMostHated() == null && targ.hasAI() && targ.getAI() instanceof AttackableAI)
					// {
					// ((AttackableAI) targ.getAI()).setGlobalAggro(-25);
					// targ.getAggroList().clear();
					// targ.getAI().setIntention(CtrlIntention.ACTIVE);
					// targ.setWalking();
					// }
					// }
					// skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					// }
					// else
					// {
					// if (activeChar instanceof Player)
					// activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					//
					// target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					// }

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					
					else if (activeChar instanceof Player)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					
					target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					break;

				case AGGREMOVE:
					// these skills needs to be rechecked
					// if (target instanceof Attackable && !target.isRaid())
					// {
					// if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, true))
					// {
					// if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
					// {
					// if (target.isUndead())
					// ((Attackable) target).reduceHate(null, ((Attackable) target).getHating(((Attackable) target).getMostHated()));
					// }
					// else
					// ((Attackable) target).reduceHate(null, ((Attackable) target).getHating(((Attackable) target).getMostHated()));
					// }
					// else
					// {
					// if (activeChar instanceof Player)
					// activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					//
					// target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					// }
					// }
					// else
					// target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					break;

				case ERASE:
					// doesn't affect siege summons
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld) && !(target instanceof SiegeSummon))
					{
						final Player summonOwner = ((Summon) target).getOwner();
						final Summon summonPet = summonOwner.getPet();
						if (summonPet != null)
						{
							summonPet.unSummon(summonOwner);
							summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
						}
					}
					else
					{
						if (activeChar instanceof Player)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
					break;

				case CANCEL_DEBUFF:
					L2Effect[] effects = target.getAllEffects();
					
					if (effects.length == 0 || effects == null)
						break;
					
					int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
					for (L2Effect e : effects)
					{
						if (e != null && e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
						{
							if (e.getSkill().getAbnormalLvl() < 100)
							{
								//Do not remove raid curse skills
								if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082)
								{
									e.exit();
									if (count > -1)
										count++;
								}
							}
						}
					}
					
					break;
					
				case STEAL_BUFF:
					if (!(target instanceof Player))
						return;
					
					L2Effect[] effects1 = target.getAllEffects();
					
					if (effects1 == null || effects1.length < 1)
						return;
					
					// Reversing array
					List<L2Effect> list = Arrays.asList(effects1);
					Collections.reverse(list);
					list.toArray(effects1);
					
					ArrayList<L2Effect> toSteal = new ArrayList<>();
					int count1 = 0;
					int lastSkill = 0;
					
					for (L2Effect e : effects1)
					{
						if (e == null
								|| !(e instanceof EffectBuff)
								|| e.getSkill().getSkillType() == L2SkillType.HEAL
								|| e.getSkill().isToggle()
								|| e.getSkill().isDebuff()
								|| e.getPeriod() == -1
								|| e.getSkill().isHeroSkill()
								)
							continue;
						
						if (e.getSkill().getId() == lastSkill)
						{
							if (count1 == 0) count = 1;
							toSteal.add(e);
						}
						else if (count1 < skill.getPower(activeChar))
						{
							toSteal.add(e);
							count1++;
						}
						else
							break;
					}
					if (!toSteal.isEmpty())
						stealEffects(activeChar, target, toSteal);
					break;
					
				case SWITCH: //custom edit
					if (target instanceof Player)
					{
						Player lider = (Player)target;
						
						if (!activeChar.isGM())
						{
							if (!lider.canBeDebuffed(L2EffectType.REMOVE_TARGET))
							{
								activeChar.sendMessage(lider.getName() + " is not affected by your " + skill.getName() + " because of 10 second trick/switch/aggro protection");
								return;
							}
							
							if (CastleManager.getInstance().getActiveSiege(lider) != null)
							{
								if (lider.isClanLeader() && lider.isCastingNow())
								{
									if (lider.getCurrentSkill() != null && lider.getCurrentSkill().getSkillId() == 246) //seal of ruler
									{
										SystemMessage sm = new SystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2);
										sm.addCharName(target);
										sm.addSkillName(skill.getId());
										activeChar.sendPacket(sm);
										return;
									}
								}
							}
						}
						
						lider.setDebuffProtection(L2EffectType.REMOVE_TARGET, 10000);
						
						LinkedList<Player> players = new LinkedList<>();
						LinkedList<Player> players2 = new LinkedList<>();
						
						double closestDistance = 600;
						double tempDistance = 0;
						
						for (Player player : target.getKnownTypeInRadius(Player.class, 600))
						{
							if (player != null)
							{
								if (player == target || player == activeChar || (target.getTarget() != null && player == target.getTarget()))
									continue;
								
								if (player.isAutoAttackableSwitch((Player)target) && ((Player)target).canAttack(player, false))
								{
									tempDistance = Util.calculateDistance(target, player, false);
									
									if (tempDistance <= closestDistance)
									{
										closestDistance = tempDistance;
										players.addFirst(player);
									}
								}
								else
								{
									if (players2.size() < 10)
									{
										if (player.isVisible() && !player.isAlikeDead())
											players2.add(player);
									}
								}
							}
						}
						
						if (players.size() > 0)
						{
							Player toBeTargated = players.get(0);
							
							if (toBeTargated != null)
							{
								if (((Player)target).isSitting())
									((Player)target).standUp();
								else
									target.abortCast();
								
								if (!target.isRunning())
									target.setRunning();
								
								((Player)target).setIsSelectingTarget(skill.getNextDanceMpCost());
								
								target.setKnockedbackTimer(GameTimeTaskManager.getGameTicks() + skill.getNextDanceMpCost());
								target.setTarget(toBeTargated);
								target.getAI().setIntention(CtrlIntention.ATTACK, toBeTargated);
								
								if (!(activeChar instanceof Player && !activeChar.getActingPlayer().isVisible()))
								{
									SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
									sm.addSkillName(skill.getId());
									target.sendPacket(sm);
								}
							}
						}
						else
						{
							if (players2.size() > 0) //these ones are not auto hittable
							{
								int num = Rnd.nextInt(players2.size());
								Player toBeTargated = players2.get(num);
								
								if (toBeTargated != null)
								{
									if (((Player)target).isSitting())
										((Player)target).standUp();
									else
									{
										target.abortAttack();
										target.abortCast();
									}
									
									if (!target.isRunning())
										target.setRunning();
									
									((Player)target).setIsSelectingTarget(skill.getNextDanceMpCost());
									
									target.setKnockedbackTimer(GameTimeTaskManager.getGameTicks() + skill.getNextDanceMpCost());
									target.setTarget(toBeTargated);
									target.getAI().setIntention(CtrlIntention.FOLLOW, toBeTargated);
									
									if (!(activeChar instanceof Player && activeChar.getActingPlayer().isVisible()))
									{
										SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
										sm.addSkillName(skill.getId());
										target.sendPacket(sm);
									}
								}
							}
							else
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2);
								sm.addCharName(target);
								sm.addSkillName(skill.getId());
								activeChar.sendPacket(sm);
							}
						}
						
						if (skill.hasEffects())
							skill.getEffects(activeChar, target);
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					}
					
					if (skill.hasEffects())
					{
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(skill);
						target.sendPacket(sm);
					}
					
					break;

				case NEGATE:
					if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					// Skills with negateId (skillId)
					if (skill.getNegateId().length != 0)
					{
						for (int id : skill.getNegateId())
						{
							if (id != 0)
								target.stopSkillEffects(id);
						}
					}
					// All others negate type skills
					else
					{
						final int negateLvl = skill.getNegateLvl();

						for (L2Effect e : target.getAllEffects())
						{
							final L2Skill effectSkill = e.getSkill();
							for (L2SkillType skillType : skill.getNegateStats())
							{
								// If power is -1 the effect is always removed without lvl check
								if (negateLvl == -1)
								{
									if (effectSkill.getSkillType() == skillType || (effectSkill.getEffectType() != null && effectSkill.getEffectType() == skillType))
										e.exit();
								}
								// Remove the effect according to its power.
								else
								{
									if (effectSkill.getEffectType() != null && effectSkill.getEffectAbnormalLvl() >= 0)
									{
										if (effectSkill.getEffectType() == skillType && effectSkill.getEffectAbnormalLvl() <= negateLvl)
											e.exit();
									}
									else if (effectSkill.getSkillType() == skillType && effectSkill.getAbnormalLvl() <= negateLvl)
										e.exit();
								}
							}
						}
					}
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					break;
				
				case PROC:
					int chance = 100;
					
					if (target instanceof Attackable)
					{
						if (target.getLevel() >= 90)
							chance = 25;
						else if (target.getLevel() >= 89)
							chance = 40;
						else if (target.getLevel() >= 88)
							chance = 55;
						else if (target.getLevel() >= 87)
							chance = 70;
						else if (target.getLevel() >= 86)
							chance = 85;
						
						if (target instanceof RaidBoss)
							chance -= 15;
					}
					
					if (chance >= 100 || Rnd.get(100) < chance)
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					
					break;
			}
		}

		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();

			skill.getEffectsSelf(activeChar);
		}
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
	private static void stealEffects(Creature stealer, Creature stolen, ArrayList<L2Effect> stolenEffects)
	{
		if (stolen == null || stolenEffects == null || stolenEffects.isEmpty()) return;
		
		for (L2Effect eff : stolenEffects)
		{
			// if eff time is smaller than 1 sec, will not be stolen, just to save CPU,
			// avoid synchronization(?) problems and NPEs
			if (eff.getPeriod() - eff.getTime() < 2)
				continue;
			
			Env env = new Env();
			env._character = stolen;
			env._target = stealer;
			env._skill = eff.getSkill();
			eff.getEffectTemplate();
			L2Effect e = EffectTemplate.getStolenEffect(env, eff);
			
			if (e != null)
				e.scheduleEffect();
			
			// Since there is a previous check that limits allowed effects to those which come from SkillType.BUFF,
			// it is not needed another check for SkillType
			if (stealer instanceof Player && e != null)
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				smsg.addSkillName(eff);
				stealer.sendPacket(smsg);
			}
			// Finishing stolen effect
			eff.exit();
		}
	}
}