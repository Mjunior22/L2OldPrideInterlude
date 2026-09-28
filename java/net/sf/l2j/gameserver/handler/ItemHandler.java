package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.itemhandlers.BeastSpice;
import net.sf.l2j.gameserver.handler.itemhandlers.Book;
import net.sf.l2j.gameserver.handler.itemhandlers.Calculator;
import net.sf.l2j.gameserver.handler.itemhandlers.ClanSkillDonate;
import net.sf.l2j.gameserver.handler.itemhandlers.DonatePotion;
import net.sf.l2j.gameserver.handler.itemhandlers.Elixir;
import net.sf.l2j.gameserver.handler.itemhandlers.EnchantScrolls;
import net.sf.l2j.gameserver.handler.itemhandlers.ExtractIt;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.handler.itemhandlers.Harvester;
import net.sf.l2j.gameserver.handler.itemhandlers.HeroItem;
import net.sf.l2j.gameserver.handler.itemhandlers.ItemSkills;
import net.sf.l2j.gameserver.handler.itemhandlers.Keys;
import net.sf.l2j.gameserver.handler.itemhandlers.LuckyBox;
import net.sf.l2j.gameserver.handler.itemhandlers.Maps;
import net.sf.l2j.gameserver.handler.itemhandlers.MercTicket;
import net.sf.l2j.gameserver.handler.itemhandlers.NoblesseItem;
import net.sf.l2j.gameserver.handler.itemhandlers.OfflineClick;
import net.sf.l2j.gameserver.handler.itemhandlers.PaganKeys;
import net.sf.l2j.gameserver.handler.itemhandlers.PetFood;
import net.sf.l2j.gameserver.handler.itemhandlers.Recipes;
import net.sf.l2j.gameserver.handler.itemhandlers.RollingDice;
import net.sf.l2j.gameserver.handler.itemhandlers.ScrollOfResurrection;
import net.sf.l2j.gameserver.handler.itemhandlers.SeedHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.SevenSignsRecord;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin1;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin10;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin11;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin12;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin13;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin14;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin15;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin16;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin17;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin18;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin19;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin2;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin20;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin21;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin22;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin23;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin24;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin25;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin26;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin27;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin28;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin29;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin3;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin30;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin31;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin32;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin33;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin34;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin35;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin36;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin37;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin38;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin39;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin4;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin40;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin41;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin42;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin43;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin44;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin45;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin46;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin47;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin48;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin49;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin5;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin50;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin6;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin7;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin8;
import net.sf.l2j.gameserver.handler.itemhandlers.Skin9;
import net.sf.l2j.gameserver.handler.itemhandlers.SoulCrystals;
import net.sf.l2j.gameserver.handler.itemhandlers.SpecialXMas;
import net.sf.l2j.gameserver.handler.itemhandlers.SummonItems;
import net.sf.l2j.gameserver.handler.itemhandlers.VipCoin;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;

public class ItemHandler
{
	private final Map<Integer, IItemHandler> _datatable = new HashMap<>();

	public static ItemHandler getInstance()
	{
		return SingletonHolder._instance;
	}

	protected ItemHandler()
	{
		registerItemHandler(new OfflineClick());
		registerItemHandler(new BeastSpice());
		registerItemHandler(new Book());
		registerItemHandler(new Calculator());
		registerItemHandler(new Elixir());
		registerItemHandler(new EnchantScrolls());
		registerItemHandler(new Harvester());
		registerItemHandler(new ItemSkills());
		registerItemHandler(new Keys());
		registerItemHandler(new Maps());
		registerItemHandler(new MercTicket());
		registerItemHandler(new PaganKeys());
		registerItemHandler(new PetFood());
		registerItemHandler(new Recipes());
		registerItemHandler(new RollingDice());
		registerItemHandler(new ScrollOfResurrection());
		registerItemHandler(new SeedHandler());
		registerItemHandler(new SevenSignsRecord());
		registerItemHandler(new SpecialXMas());
		registerItemHandler(new SoulCrystals());
		registerItemHandler(new SummonItems());
		// ADDED BY VEGA
		registerItemHandler(new Gem());
		registerItemHandler(new ExtractIt());
		registerItemHandler(new LuckyBox());
		registerItemHandler(new DonatePotion());
		registerItemHandler(new NoblesseItem());
		registerItemHandler(new ClanSkillDonate());
		registerItemHandler(new HeroItem());
		registerItemHandler(new Skin1());
		registerItemHandler(new Skin2());
		registerItemHandler(new Skin3());
		registerItemHandler(new Skin4());
		registerItemHandler(new Skin5());
		registerItemHandler(new Skin6());
		registerItemHandler(new Skin7());
		registerItemHandler(new Skin8());
		registerItemHandler(new Skin9());
		registerItemHandler(new Skin10());
		registerItemHandler(new Skin11());
		registerItemHandler(new Skin12());
		registerItemHandler(new Skin13());
		registerItemHandler(new Skin14());
		registerItemHandler(new Skin15());
		registerItemHandler(new Skin16());
		registerItemHandler(new Skin17());
		registerItemHandler(new Skin18());
		registerItemHandler(new Skin19());
		registerItemHandler(new Skin20());
		registerItemHandler(new Skin21());
		registerItemHandler(new Skin22());
		registerItemHandler(new Skin23());
		registerItemHandler(new Skin24());
		registerItemHandler(new Skin25());
		registerItemHandler(new Skin26());
		registerItemHandler(new Skin27());
		registerItemHandler(new Skin28());
		registerItemHandler(new Skin29());
		registerItemHandler(new Skin30());
		registerItemHandler(new Skin31());
		registerItemHandler(new Skin32());
		registerItemHandler(new Skin33());
		registerItemHandler(new Skin34());
		registerItemHandler(new Skin35());
		registerItemHandler(new Skin36());
		registerItemHandler(new Skin37());
		registerItemHandler(new Skin38());
		registerItemHandler(new Skin39());
		registerItemHandler(new Skin40());
		registerItemHandler(new Skin41());
		registerItemHandler(new Skin42());
		registerItemHandler(new Skin43());
		registerItemHandler(new Skin44());
		registerItemHandler(new Skin45());
		registerItemHandler(new Skin46());
		registerItemHandler(new Skin47());
		registerItemHandler(new Skin48());
		registerItemHandler(new Skin49());
		registerItemHandler(new Skin50());
		registerItemHandler(new VipCoin());
	}

	public void registerItemHandler(IItemHandler handler)
	{
		_datatable.put(handler.getClass().getSimpleName().intern().hashCode(), handler);
	}

	public IItemHandler getItemHandler(EtcItem item)
	{
		if (item == null || item.getHandlerName() == null)
			return null;

		return _datatable.get(item.getHandlerName().hashCode());
	}

	public int size()
	{
		return _datatable.size();
	}

	private static class SingletonHolder
	{
		protected static final ItemHandler _instance = new ItemHandler();
	}
}