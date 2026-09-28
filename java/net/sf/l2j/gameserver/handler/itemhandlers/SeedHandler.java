package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.manor.Seed;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class SeedHandler implements IItemHandler
{
	private Player _activeChar;
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!Config.ALLOW_MANOR || !(playable instanceof Player))
			return;

		_activeChar = (Player) playable;
		final WorldObject tgt = playable.getTarget();
		
		if (!(tgt instanceof Attackable) || !((Attackable) tgt).getTemplate().isSeedable())
		{
			_activeChar.sendPacket(SystemMessageId.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING);
			return;
		}

		final Attackable target = (Attackable) tgt;
		if (target.isDead() || target.isSeeded())
		{
			_activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		final Seed seed = CastleManorManager.getInstance().getSeed(item.getItemId());
		if (seed == null)
			return;

		if (seed.getCastleId() != MapRegionData.getInstance().getAreaCastle(_activeChar.getX(), _activeChar.getY()))
		{
			_activeChar.sendPacket(SystemMessageId.THIS_SEED_MAY_NOT_BE_SOWN_HERE);
			return;
		}

		target.setSeeded(seed, playable.getObjectId());

		final IntIntHolder[] skills = item.getEtcItem().getSkills();
		if (skills != null)
		{
			if (skills[0] == null)
				return;

			_activeChar.useMagic(skills[0].getSkill(), false, false);
		}
	}
}