package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class ClanSkillDonate implements IItemHandler
{
	public static final int CLAN_SKILLS[] =
	{
		370,
		371,
		372,
		373,
		374,
		375,
		376,
		377,
		378,
		379,
		380,
		381,
		382,
		383,
		384,
		385,
		386,
		387,
		388,
		389,
		390,
		391,
		392,
		393,
		394
	};
	int _skillLvl;

	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
			
		final Player activeChar = (Player) playable;
		
		// Verificação do clã
		if (activeChar.getClan() == null)
		{
			activeChar.sendMessage("You must be in a clan to use it");
			return;
		}
		
		// Verificação do líder do clã
		if (!activeChar.isClanLeader())
		{
			activeChar.sendMessage("Only clan leader can use this item.");
			return;
		}
		
		int itemId = item.getItemId();
		switch (itemId)
		{
			case 9784:
				_skillLvl = 1;
				break;
			case 9785:
				_skillLvl = 2;
				break;
			case 9786:
				_skillLvl = 3;
				break;
			default:
				activeChar.sendMessage("Invalid item.");
				return;
		}
		
		boolean skillAdded = false;
		final L2Skill[] list = activeChar.getClan().getAllSkills();
		
		for (int skillId : CLAN_SKILLS)
		{
			boolean hasSkill = false;
			
			// Verifica se o clã já tem a skill no mesmo nível ou superior
			for (L2Skill sk : list)
			{
				if (sk.getId() == skillId && sk.getLevel() >= _skillLvl)
				{
					hasSkill = true;
					break;
				}
			}
			
			if (hasSkill)
				continue;
			
			// Obtém a skill da tabela
			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, _skillLvl);
			
			// VERIFICAÇÃO CRÍTICA: Skill existe?
			if (skill == null)
			{
				System.out.println("[ERROR] ClanSkillDonate: Skill ID " + skillId + " Level " + _skillLvl + " not found in SkillTable!");
				continue;
			}

			activeChar.getClan().addNewSkill(skill);
			skillAdded = true;
			
			// Envia mensagem do sistema
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(skill);
			activeChar.sendPacket(sm);
			activeChar.getClan().broadcastToOnlineMembers(sm);
			
			// Envia mensagem no chat do clã
			String skillname = skill.getName();
			String text = activeChar.getName() + " > " + skillname + " Lvl." + String.valueOf(_skillLvl) + " to the clan.";
			CreatureSay cs = new CreatureSay(0, Say2.CLAN, activeChar.getClan().getName(), text);
			activeChar.getClan().broadcastToOnlineMembers(cs);
		}
		
		// Atualiza lista de skills para todos os membros
		if (skillAdded)
		{
			for (Player member : activeChar.getClan().getOnlineMembers())
			{
				member.sendSkillList();
			}
			
			// Remove o item apenas se alguma skill foi adicionada
			activeChar.destroyItem("Donate Clan", item.getObjectId(), 1, activeChar, true);
		}
		else
		{
			activeChar.sendMessage("Your clan already has all available skills at this level or higher.");
			// Não remove o item se nenhuma skill foi adicionada
		}
	}
}