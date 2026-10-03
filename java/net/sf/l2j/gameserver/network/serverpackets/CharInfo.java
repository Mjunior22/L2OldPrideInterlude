package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

import custom.vip.visual.VipVisual;
import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public class CharInfo extends L2GameServerPacket
{
	private final Player _activeChar;
	private final Inventory _inv;
	
	public CharInfo(Player cha)
	{
		_activeChar = cha;
		_inv = _activeChar.getInventory();
	}
	
	@Override
	protected final void writeImpl()
	{
		boolean gmSeeInvis = false;
		
		if (_activeChar.getAppearance().getInvisible())
		{
			Player tmp = getClient().getActiveChar();
			if (tmp != null && tmp.isGM())
				gmSeeInvis = true;
		}
		
		final boolean olympiad = (_activeChar.isInOlympiadMode() || _activeChar.isDisguised() || (_activeChar._inEventDM && DM._started));
		
		writeC(0x03);
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.isInOlympiadMode() ? "Olympiader" : (_activeChar._inEventDM && DM._started) ? "Constestant" : _activeChar.isDisguised() ? "Disguised" : _activeChar.getName());
		writeD(_activeChar.getRace().ordinal());
		
		if (olympiad)
			writeD(Rnd.get(1));
		else
			writeD(_activeChar.getAppearance().getSex().ordinal());
		
		if (_activeChar.getClassIndex() == 0)
			writeD(_activeChar.getClassId().getId());
		else
			writeD(_activeChar.getBaseClass());
		
		if(!_activeChar.isDressMeEnabled())
		{
			writeD(_activeChar.isInGludin() ? 0 : _inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
			// HEAD
			writeD(_activeChar.isInGludin() ? 0 : VipVisual.getPaperdollItemId(_activeChar, Inventory.PAPERDOLL_HEAD));

			
			if (_activeChar._inEventHG && HuntingGround._started)
			{
				if (_activeChar._teamNameHG.equalsIgnoreCase("(Red)"))
				{
					writeD(5611); //TIPO DO BOW
					writeD(5611);
				}
				if (_activeChar._teamNameHG.equalsIgnoreCase("(Blue)"))
				{
					writeD(6594); //TIPO DO BOW
					writeD(6594);
				}
			}
			else
			{
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			}
			
//			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
//			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
//			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
//			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			
			
			// GLOVES
			writeD(VipVisual.getPaperdollItemId(_activeChar, Inventory.PAPERDOLL_GLOVES));

			// CHEST
			writeD(VipVisual.getPaperdollItemId(_activeChar, Inventory.PAPERDOLL_CHEST));
						
			// LEGS
			writeD(VipVisual.getPaperdollItemId(_activeChar, Inventory.PAPERDOLL_LEGS));

			// FEET
			writeD(VipVisual.getPaperdollItemId(_activeChar, Inventory.PAPERDOLL_FEET));
			
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			
			if (_activeChar._inEventHG && HuntingGround._started)
			{
				if (_activeChar._teamNameHG.equalsIgnoreCase("(Red)"))
				{
					writeD(5611); //TIPO DO BOW
				}
				if (_activeChar._teamNameHG.equalsIgnoreCase("(Blue)"))
				{
					writeD(6594); //TIPO DO BOW
				}
			}
			else
			{
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			
			writeD(_activeChar.isInGludin() ? 0 : _inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(_activeChar.isInGludin() ? 0 : _inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
		}
		else
		{
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_activeChar.getDressMeData() == null ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES) : (_activeChar.getDressMeData().getGlovesId() == 0 ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES) : _activeChar.getDressMeData().getGlovesId()));
			writeD(_activeChar.getDressMeData() == null ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST) : (_activeChar.getDressMeData().getChestId() == 0 ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST) : _activeChar.getDressMeData().getChestId()));
			writeD(_activeChar.getDressMeData() == null ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS) : (_activeChar.getDressMeData().getLegsId() == 0 ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS) : _activeChar.getDressMeData().getLegsId()));
			writeD(_activeChar.getDressMeData() == null ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET) : (_activeChar.getDressMeData().getBootsId() == 0 ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET) : _activeChar.getDressMeData().getBootsId()));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_activeChar.getDressMeData() == null ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE) : (_activeChar.getDressMeData().getHelmetId() == 0 ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE) : _activeChar.getDressMeData().getHelmetId()));
			writeD(_activeChar.getDressMeData() == null ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR) : (_activeChar.getDressMeData().getHelmetId() == 0 ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR) : _activeChar.getDressMeData().getHelmetId()));
		}
		
		
		// c6 new h's
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		
		writeD(_activeChar.getPvpFlag());
		writeD(_activeChar.getKarma());
		
		writeD(_activeChar.getMAtkSpd(null));
		writeD(_activeChar.getPAtkSpd(null));
		
		writeD(_activeChar.getPvpFlag());
		writeD(_activeChar.getKarma());
		
		int _runSpd = _activeChar.getStat().getBaseRunSpeed();
		int _walkSpd = _activeChar.getStat().getBaseWalkSpeed();
		int _swimSpd = _activeChar.getStat().getBaseSwimSpeed();
		writeD(_runSpd); // base run speed
		writeD(_walkSpd); // base walk speed
		writeD(_swimSpd); // swim run speed
		writeD(_swimSpd); // swim walk speed
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_activeChar.isFlying() ? _runSpd : 0); // fly run speed
		writeD(_activeChar.isFlying() ? _walkSpd : 0); // fly walk speed
		writeF(_activeChar.getStat().getMovementSpeedMultiplier()); // run speed multiplier
		writeF(_activeChar.getStat().getAttackSpeedMultiplier()); // attack speed multiplier
		
		if (_activeChar.getMountType() != 0)
		{
			writeF(NpcTable.getInstance().getTemplate(_activeChar.getMountNpcId()).getCollisionRadius());
			writeF(NpcTable.getInstance().getTemplate(_activeChar.getMountNpcId()).getCollisionHeight());
		}
		else
		{
			writeF(_activeChar.getCollisionRadius());
			writeF(_activeChar.getCollisionHeight());
		}
		
		writeD(_activeChar.getAppearance().getHairStyle());
		writeD(_activeChar.getAppearance().getHairColor());
		writeD(_activeChar.getAppearance().getFace());
		
		writeS((_activeChar.isInOlympiadMode() || _activeChar.isDisguised()) ? "" : _activeChar.getTitle());
		
		if (((TvT.is_started() || TvT.is_teleport()) && _activeChar._inEventTvT) || ((CTF.is_started() || CTF.is_teleport()) && _activeChar._inEventCTF) 
			|| ((HuntingGround.is_started() || HuntingGround.is_teleport()) && _activeChar._inEventHG) || ((Domination.is_started() || Domination.is_teleport()) && _activeChar._inEventDomi)
			|| ((DM.is_started() || DM.is_teleport()) && _activeChar._inEventDM) || olympiad)
		{
			writeD(0);
			writeD(0);
			writeD(0);
			writeD(0);
		}
		else
		{
			writeD(_activeChar.getClanId());
			writeD(_activeChar.getClanCrestId());
			writeD(_activeChar.getAllyId());
			writeD(_activeChar.getAllyCrestId());
		}
		
		writeD(0);
		
		writeC(_activeChar.isSitting() ? 0 : 1); // standing = 1 sitting = 0
		writeC(_activeChar.isRunning() ? 1 : 0); // running = 1 walking = 0
		writeC(_activeChar.isInCombat() ? 1 : 0);
		writeC(_activeChar.isAlikeDead() ? 1 : 0);
		
		if (gmSeeInvis)
			writeC(0);
		else
			writeC(_activeChar.getAppearance().getInvisible() ? 1 : 0); // invisible = 1 visible =0
			
		writeC(_activeChar.getMountType()); // 1 on strider 2 on wyvern 0 no mount
		writeC(_activeChar.getStoreType().getId()); // 1 - sellshop
		
		writeH(_activeChar.getCubics().size());
		for (int id : _activeChar.getCubics().keySet())
			writeH(id);
		
		writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);
		
		if (gmSeeInvis)
			writeD((_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()));
		else
			writeD(_activeChar.getAbnormalEffect());
		
		writeC(_activeChar.getRecomLeft());
		writeH(_activeChar.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
		writeD(_activeChar.getClassId().getId());
		
		writeD(_activeChar.getMaxCp());
		writeD((int) _activeChar.getCurrentCp());
		writeC(olympiad && !_activeChar.isDisguised() || _activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());
		
		if (_activeChar.getTeam() == 1 || (Config.PLAYER_SPAWN_PROTECTION > 0 && _activeChar.isSpawnProtected()))
			writeC(0x01); // team circle around feet 1= Blue, 2 = red
		else if (_activeChar.getTeam() == 2)
			writeC(0x02); // team circle around feet 1= Blue, 2 = red
		else
			writeC(0x00); // team circle around feet 1= Blue, 2 = red
			
		writeD(_activeChar.getClanCrestLargeId());
		writeC(_activeChar.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
		writeC((_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA)) ? 1 : 0); // Hero Aura
		
		writeC(_activeChar.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
		
		Location loc = _activeChar.getFishingLoc();
		if (loc != null)
		{
			writeD(loc.getX());
			writeD(loc.getY());
			writeD(loc.getZ());
		}
		else
		{
			writeD(0);
			writeD(0);
			writeD(0);
		}
		
		writeD(_activeChar.getAppearance().getNameColor());
		
		writeD(0x00); // isRunning() as in UserInfo?
		
		writeD(_activeChar.getPledgeClass());
		writeD(_activeChar.getPledgeType());
		
		writeD(_activeChar.getAppearance().getTitleColor());
		
		if (_activeChar.isCursedWeaponEquipped())
			writeD(CursedWeaponManager.getInstance().getCurrentStage(_activeChar.getCursedWeaponEquippedId()) - 1);
		else
			writeD(0x00);
	}
}