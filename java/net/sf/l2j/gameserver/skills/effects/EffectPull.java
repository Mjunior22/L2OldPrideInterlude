package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.instance.Tester;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Util;

import custom.effectprotection.PvpProtectionManager;

public class EffectPull extends L2Effect
{
	private Creature _actor;
	private Creature _target;
	
	public EffectPull(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.THROW_UP;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() == null || getEffector() == null)
			return false;
		
		if (getEffected() == getEffector())
			return false;
		
		if (getEffected() instanceof RaidBoss || getEffected().isRooted() || getEffected().isInvul() || getSkill().getBuffDuration() == 1 || getEffected() instanceof Tester)
		{
			_actor = getEffector();
			_target = getEffected();
		}
		else
		{
			_actor = getEffected();
			_target = getEffector();
			
			if (_actor instanceof Player && _actor.getActingPlayer().isSitting())
				_actor.getActingPlayer().standUp();
		}
		
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
		if (getEffected() instanceof Player && getEffector() instanceof Player)
		{
			Player target = (Player) getEffected();
			Player attacker = (Player) getEffector();
			
			// Verifica se pode aplicar o efeito
			if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.KNOCKBACK))
				return false; // Não aplica o efeito
				
			// Registra que o efeito foi aplicado
			PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.KNOCKBACK);
		}
		// ===== FIM DO NOVO SISTEMA =====
		
		_actor.abortAttack();
		
		if (_actor.isMoving())
			_actor.broadcastPacket(new StopMove(_actor));
		else if (_actor.isCastingNow())
			_actor.abortCast();
		
		if (_actor instanceof Playable)
			_actor.getAI().setIntention(CtrlIntention.ACTIVE);
		
		int id, x, y, z, tx, ty, tz;
		
		id = _actor.getObjectId();
		x = _actor.getX();
		y = _actor.getY();
		z = _actor.getZ();
		tx = _target.getX();
		ty = _target.getY();
		tz = _target.getZ();
		
		if (x > tx)
			tx++;
		else if (x < tx)
			tx--;
		
		if (y > ty)
			ty++;
		else if (y < ty)
			ty--;
		
		_actor.broadcastPacket(new FlyToLocation(id, x, y, z, tx, ty, tz, FlyType.valueOf(getSkill().getFlyType())));
		_actor.getPosition().setXYZ(tx, ty, tz);
		
		final int hamstring = (int) _actor.calcStat(Stats.HAMSTRING, 0, _target, getSkill());
		
		if (hamstring > 0 && getSkill().getFlyType() != "DUMMY")
		{
			final int distance = (int) Util.calculateDistance(x, y, z, tx, ty);
			final int damage = (int) (hamstring * distance / 2.5);
			if (damage > 0)
			{
				_actor.reduceCurrentHp(damage, _target, true, true, null, true);
				_target.sendMessage("You caused an additional " + damage + " damage to " + _actor.getName() + " due to Hemorrhage");
			}
		}
		
		return true;
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}