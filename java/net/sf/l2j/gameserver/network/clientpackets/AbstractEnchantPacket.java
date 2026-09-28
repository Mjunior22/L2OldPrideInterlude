package net.sf.l2j.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.CrystalType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;

public abstract class AbstractEnchantPacket extends L2GameClientPacket
{
	public static final Map<Integer, EnchantScroll> _scrolls = new HashMap<>();
	
	public static final int ITEM_DESTROYED = 0;
	public static final int ENCHANT_TO_4_OR_0 = 1;
	public static final int ENCHANT_MINUS_ONE_OR_NEXT_LEVEL = 2;
	public static final int REMAIN_SAME_ENCHANT = 3;
	public static final int ENCHANT_TO_10_OR_6_OR_3_OR_0 = 4;
	public static final int RETURNS_TO_0 = 5;
	public static final int ENCHANT_TO_7_OR_3_OR_0 = 6;
	public static final int ENCHANT_TO_20_14_OR_12_OR_10_OR_6_OR_3_OR_0 = 7;

	public static class EnchantItem
	{
		protected final boolean _isWeapon;
		protected final CrystalType _grade;
		protected int _maxEnchantLevel = 35;
		protected final int _chanceAdd;
		protected final int[] _itemIds;
		
		public EnchantItem(boolean wep, CrystalType type, int level, int chanceAdd, int[] items)
		{
			_isWeapon = wep;
			_grade = type;
			_maxEnchantLevel = level;
			_chanceAdd = chanceAdd;
			_itemIds = items;
		}

		/**
		 * @param enchantItem : The item to enchant.
		 * @return true if support item can be used for this item
		 */
		public final boolean isValid(ItemInstance enchantItem)
		{
			if (enchantItem == null)
				return false;

			// checking scroll type and configured maximum enchant level
			switch (enchantItem.getItem().getType2())
			{
				case Item.TYPE2_WEAPON:
					if (!_isWeapon)
						return false;
					break;

				case Item.TYPE2_SHIELD_ARMOR:
				case Item.TYPE2_ACCESSORY:
					if (_isWeapon)
						return false;
					break;

				default:
					return false;
			}

			// check for crystal type
			if (_grade != enchantItem.getItem().getCrystalType())
				return false;

			if (enchantItem.getItem().getWeight() == 4)
				_maxEnchantLevel = 25;
			else if (enchantItem.getItem().getWeight() == 3)
				_maxEnchantLevel = 25;
			else if (enchantItem.getItem().getWeight() == 2)
				_maxEnchantLevel = 18;
			else if (enchantItem.getItem().getWeight() == 1)
				_maxEnchantLevel = 14;
			else
				_maxEnchantLevel = 35;
			
			if (_maxEnchantLevel != 0 && enchantItem.getEnchantLevel() >= _maxEnchantLevel)
			{
				return false;
			}

			return true;
		}

		public final int getChanceAdd()
		{
			return _chanceAdd;
		}
	}
	
	public static final class EnchantScroll extends EnchantItem
	{
		private final boolean _isForbidden;
		private final boolean _isLegendary;
		private final boolean _isBlessed;
		private final boolean _isCrystal;
		private final boolean _isSafe;
		private final boolean _isDivine;
		private final int _scrollLvl;
		
		public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, CrystalType type, int level, int chance, int[] items)
		{
			super(wep, type, level, chance, items);
			
			_isBlessed = bless;
			_isCrystal = crystal;
			_isSafe = safe;
			_isForbidden = false;
			_isLegendary = false;
			_isDivine = false;
			
			if (_isDivine)
				_scrollLvl = 5;
			else if (_isForbidden)
				_scrollLvl = 4;
			else if (_isLegendary)
				_scrollLvl = 3;
			else if (_isBlessed)
				_scrollLvl = 2;
			else if (_isCrystal)
				_scrollLvl = 1;
			else
				_scrollLvl = 0;
		}
		
		public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, boolean forbidden, boolean legendary, boolean divine, CrystalType type, int level, int chance, int[] items)
		{
			super(wep, type, level, chance, items);
			
			_isBlessed = bless;
			_isCrystal = crystal;
			_isSafe = safe;
			_isForbidden = forbidden;
			_isLegendary = legendary;
			_isDivine = divine;
			
			if (_isDivine)
				_scrollLvl = 5;
			else if (_isForbidden)
				_scrollLvl = 4;
			else if (_isLegendary)
				_scrollLvl = 3;
			else if (_isBlessed)
				_scrollLvl = 2;
			else if (_isCrystal)
				_scrollLvl = 1;
			else
				_scrollLvl = 0;
		}
		
		public final boolean isDivine()
		{
			return _isDivine;
		}
		
		public final boolean isForbidden()
		{
			return _isForbidden;
		}
		
		public final boolean isLegendary()
		{
			return _isLegendary;
		}
		
		public final boolean isBlessed()
		{
			return _isBlessed;
		}
		
		public final boolean isCrystal()
		{
			return _isCrystal;
		}
		
		/*
		 * Return true for safe-enchant scrolls (enchant level will remain on failure)
		 */
		public final boolean isSafe()
		{
			return _isSafe;
		}
		
		public final int getMaxEnchantLevel(final ItemInstance enchantItem)
		{
			return _maxEnchantLevel;
		}
		
		public final boolean isValid(ItemInstance enchantItem, EnchantItem supportItem)
		{
			// blessed scrolls can't use support items
			if (supportItem != null && (!supportItem.isValid(enchantItem) || isBlessed()))
				return false;
			
			return isValid(enchantItem);
		}
		
		public final int getChance(final ItemInstance enchantItem)
		{
			
			if (!isValid(enchantItem))
				return -200;
			
			int chance = 0;
			
			switch (enchantItem.getEnchantLevel())
			{
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
					chance = 100;
					break;
				case 7:
					chance = 90;
					break;
				case 8:
					chance = 90;
					break;
				case 9:
					chance = 85;
					break;
				case 10:
					chance = 85;
					break;
				case 11:
					chance = 80;
					break;
				case 12:
					chance = 80;
					break;
				case 13:
					chance = 70;
					break;
				case 14:
					chance = 70;
					break;
				case 15:
					chance = 65;
					break;
				case 16:
					chance = 65;
					break;
				case 17:
					chance = 60;
					break;
				case 18:
					chance = 60;
					break;
				case 19:
					chance = 50;
					break;
				case 20:
					chance = 50;
					break;
				case 21:
					chance = 45;
					break;
				case 22:
					chance = 45;
					break;
				case 23:
					chance = 40;
					break;
				case 24:
					chance = 40;
					break;
				case 25:
					chance = 40;
					break;
				case 26:
					chance = 40;
					break;
				case 27:
					chance = 40;
					break;
				case 28:
					chance = 40;
					break;
				case 29:
					chance = 40;
					break;
				case 30:
					chance = 40;
					break;
				case 31:
					chance = 40;
					break;
				case 32:
					chance = 40;
					break;
				case 33:
					chance = 40;
					break;
				case 34:
					chance = 10;
					break;
				default:
					chance = 10;
			}
			
			if (enchantItem.getItem().getWeight() == 4 && enchantItem.getEnchantLevel() > 6)
				chance -= 15;
			else if (enchantItem.getItem().getWeight() == 4 && enchantItem.getEnchantLevel() > 19)
				chance -= 15;
			else if (enchantItem.getItem().getWeight() == 4 && enchantItem.getEnchantLevel() > 24)
				chance = 0;
			
			else if (enchantItem.getItem().getWeight() == 3 && enchantItem.getEnchantLevel() > 6)
				chance -= 25;
			else if (enchantItem.getItem().getWeight() == 3 && enchantItem.getEnchantLevel() > 17)
				chance -= 15;
			else if (enchantItem.getItem().getWeight() == 3 && enchantItem.getEnchantLevel() > 24)
				chance = 0;
			
			else if (enchantItem.getItem().getWeight() == 2 && enchantItem.getEnchantLevel() > 6)
				chance -= 50;
			else if (enchantItem.getItem().getWeight() == 2 && enchantItem.getEnchantLevel() > 11)
				chance -= 20;
			else if (enchantItem.getItem().getWeight() == 2 && enchantItem.getEnchantLevel() > 18)
				chance = 0;
			
			else if (enchantItem.getItem().getWeight() == 1 && enchantItem.getEnchantLevel() > 6)
				chance -= 60;
			else if (enchantItem.getItem().getWeight() == 1 && enchantItem.getEnchantLevel() > 9)
				chance -= 30;
			else if (enchantItem.getItem().getWeight() == 1 && enchantItem.getEnchantLevel() > 16)
				chance = 0;
			
			switch (_scrollLvl)
			{
				case 0: // NORMAL
				{
					chance += 5;
					if (enchantItem.getItem().getWeight() < 5 && enchantItem.getEnchantLevel() >= 25)
						chance = 0;
					break;
				}
				case 1: // CRYSTAL
				{
					chance += 10;
					if (enchantItem.getItem().getWeight() < 5 && enchantItem.getEnchantLevel() >= 25)
						chance = 0;
					
					else if (((enchantItem.getItem().getWeight() == 2 && enchantItem.getEnchantLevel() >= 10) || enchantItem.getItem().getWeight() == 1))
					{
						chance -= 25;
						break;
					}
					break;
				}
				case 2: // BLESSED
				{
					chance += 30;
					break;
				}
				case 3: // LEGENDARY
				{
					// chance += 40;
					chance = 25;
					break;
				}
				case 4: // FORBBIDEN
				{
					chance += 50;
					break;
				}
				
				case 5: // DIVINE
				{
					chance = 25;
					break;
				}
				default:
				{
					System.out.println("LOL WTF get enchant chance has a 'default' scroll wtf lol");
					return -1;
				}
			}
			
			return chance;
		}
		
		/**
		 * @param enchantItem - item to be enchanted
		 * @return 3 = item and enchant stay the same 2 = enchant is subtracted by 1 1 = enchant is set to 0 0 = item is destroyed
		 */
		public final byte determineFateOfItemIfFail(final ItemInstance enchantItem)
		{
			
			final int uniqueness = enchantItem.getItem().getWeight();
			
			if (_isForbidden)
				return REMAIN_SAME_ENCHANT;
			
			if (_isDivine)
			{
				switch (uniqueness)
				{
					case 0:
						if (enchantItem.getEnchantLevel() >= 14)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 1:
						if (enchantItem.getEnchantLevel() >= 14)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 2:
						if (enchantItem.getEnchantLevel() >= 18)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 3:
						if (enchantItem.getEnchantLevel() >= 23)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 4:
						if (enchantItem.getEnchantLevel() >= 25)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
				}
				
				if (uniqueness >= 5)
					return REMAIN_SAME_ENCHANT;
			}
			
			if (_isLegendary)
			{
				switch (uniqueness)
				{
					case 0:
						if (enchantItem.getEnchantLevel() >= 14)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 1:
						if (enchantItem.getEnchantLevel() >= 14)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 2:
						if (enchantItem.getEnchantLevel() >= 18)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 3:
						if (enchantItem.getEnchantLevel() >= 25)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
					case 4:
						if (enchantItem.getEnchantLevel() >= 25)
							return ITEM_DESTROYED;
						return REMAIN_SAME_ENCHANT;
				}
				
				if (uniqueness >= 5)
					return REMAIN_SAME_ENCHANT;
			}
			
			else if (_isBlessed)
			{
				switch (uniqueness)
				{
					case 0:
						return ITEM_DESTROYED;
					case 1:
						if (enchantItem.getEnchantLevel() >= 10)
							return ITEM_DESTROYED;
						return ENCHANT_TO_4_OR_0;
					case 2:
						if (enchantItem.getEnchantLevel() >= 14)
							return ITEM_DESTROYED;
						return ENCHANT_TO_10_OR_6_OR_3_OR_0;
					case 3:
						if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel())
							return ENCHANT_TO_10_OR_6_OR_3_OR_0;
						return ENCHANT_TO_7_OR_3_OR_0;
					case 4:
						if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel())
							return ENCHANT_TO_20_14_OR_12_OR_10_OR_6_OR_3_OR_0;
						return ENCHANT_MINUS_ONE_OR_NEXT_LEVEL;
				}
				
				if (uniqueness >= 5)
					return REMAIN_SAME_ENCHANT;
			}
			
			else if (_isCrystal)
			{
				switch (uniqueness)
				{
					case 0:
						return ITEM_DESTROYED;
					case 1:
						if (enchantItem.getEnchantLevel() >= 7)
							return ITEM_DESTROYED;
						return RETURNS_TO_0;
					case 2:
						if (enchantItem.getEnchantLevel() >= 10)
							return ITEM_DESTROYED;
						return ENCHANT_TO_4_OR_0;
					case 3:
						if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel() + 4)
							return ITEM_DESTROYED;
						return ENCHANT_TO_7_OR_3_OR_0;
					case 4:
						if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel() + 4)
							return ITEM_DESTROYED;
						return ENCHANT_TO_7_OR_3_OR_0;
				}
				
				if (uniqueness >= 5)
					return ENCHANT_TO_20_14_OR_12_OR_10_OR_6_OR_3_OR_0;
			}
			
			else // normal scrolls
			{
				switch (uniqueness)
				{
					case 0:
						return ITEM_DESTROYED;
					case 1:
						return ITEM_DESTROYED;
					case 2:
						return ITEM_DESTROYED;
					case 3:
						if (enchantItem.getEnchantLevel() >= 4)
							return ITEM_DESTROYED;
						return ENCHANT_TO_4_OR_0;
					case 4:
						if (enchantItem.getEnchantLevel() >= 7)
							return ITEM_DESTROYED;
						return ENCHANT_TO_7_OR_3_OR_0;
				}
				
				if (uniqueness >= 5)
					return ENCHANT_TO_20_14_OR_12_OR_10_OR_6_OR_3_OR_0;
			}
			
			return ITEM_DESTROYED;
		}
	}

	/**
	 * Format : itemId, (isWeapon, isBlessed, isCrystal, grade)<br>
	 * Allowed items IDs must be sorted by ascending order.
	 */
	static
	{
		// itemId, (isWeapon, isBlessed, isCrystal, isSafe, grade, max enchant level, chance increase, allowed item IDs)
		// allowed items list must be sorted by ascending order
		
		_scrolls.put(729, new EnchantScroll(true, false, false, false, CrystalType.A, 20, 0, null));
		_scrolls.put(730, new EnchantScroll(false, false, false, false, CrystalType.A, 20, 0, null));
		_scrolls.put(731, new EnchantScroll(true, false, true, false, CrystalType.A, 20, 0, null));
		_scrolls.put(732, new EnchantScroll(false, false, true, false, CrystalType.A, 20, 0, null));
		_scrolls.put(947, new EnchantScroll(true, false, false, false, CrystalType.B, 20, 0, null));
		_scrolls.put(948, new EnchantScroll(false, false, false, false, CrystalType.B, 20, 0, null));
		_scrolls.put(949, new EnchantScroll(true, false, true, false, CrystalType.B, 20, 0, null));
		_scrolls.put(950, new EnchantScroll(false, false, true, false, CrystalType.B, 20, 0, null));
		_scrolls.put(951, new EnchantScroll(true, false, false, false, CrystalType.C, 20, 0, null));
		_scrolls.put(952, new EnchantScroll(false, false, false, false, CrystalType.C, 20, 0, null));
		_scrolls.put(953, new EnchantScroll(true, false, true, false, CrystalType.C, 20, 0, null));
		_scrolls.put(954, new EnchantScroll(false, false, true, false, CrystalType.C, 20, 0, null));
		_scrolls.put(955, new EnchantScroll(true, false, false, false, CrystalType.D, 20, 0, null));
		_scrolls.put(956, new EnchantScroll(false, false, false, false, CrystalType.D, 20, 0, null));
		_scrolls.put(957, new EnchantScroll(true, false, true, false, CrystalType.D, 20, 0, null));
		_scrolls.put(958, new EnchantScroll(false, false, true, false, CrystalType.D, 20, 0, null));
		
		_scrolls.put(959, new EnchantScroll(true, false, false, false, CrystalType.S, 35, 0, null));
		_scrolls.put(960, new EnchantScroll(false, false, false, false, CrystalType.S, 35, 0, null));
		_scrolls.put(961, new EnchantScroll(true, false, true, false, CrystalType.S, 35, 0, null));
		_scrolls.put(962, new EnchantScroll(false, false, true, false, CrystalType.S, 35, 0, null));
		
		_scrolls.put(6569, new EnchantScroll(true, true, false, false, CrystalType.A, 20, 0, null));
		_scrolls.put(6570, new EnchantScroll(false, true, false, false, CrystalType.A, 20, 0, null));
		_scrolls.put(6571, new EnchantScroll(true, true, false, false, CrystalType.B, 20, 0, null));
		_scrolls.put(6572, new EnchantScroll(false, true, false, false, CrystalType.B, 20, 0, null));
		_scrolls.put(6573, new EnchantScroll(true, true, false, false, CrystalType.C, 20, 0, null));
		_scrolls.put(6574, new EnchantScroll(false, true, false, false, CrystalType.C, 20, 0, null));
		_scrolls.put(6575, new EnchantScroll(true, true, false, false, CrystalType.D, 20, 0, null));
		_scrolls.put(6576, new EnchantScroll(false, true, false, false, CrystalType.D, 20, 0, null));
		// Blessed scroll grade S
		_scrolls.put(6577, new EnchantScroll(true, true, false, false, CrystalType.S, 35, 0, null));
		_scrolls.put(6578, new EnchantScroll(false, true, false, false, CrystalType.S, 35, 0, null));
		// Legendary scrolls enchant
		_scrolls.put(9740, new EnchantScroll(true, false, false, false, false, true, false, CrystalType.S, 35, 0, null));
		_scrolls.put(9741, new EnchantScroll(false, false, false, false, false, true, false, CrystalType.S, 35, 0, null));
		// Forbiden scrolls enchant
		_scrolls.put(22018, new EnchantScroll(true, false, false, false, true, false, false, CrystalType.S, 20, 0, null));
		_scrolls.put(22020, new EnchantScroll(false, false, false, false, true, false, false, CrystalType.S, 20, 0, null));
		_scrolls.put(22019, new EnchantScroll(true, false, false, false, CrystalType.A, 20, 0, null));
		_scrolls.put(22021, new EnchantScroll(false, false, false, false, CrystalType.A, 20, 0, null));
		// Divine scrolls enchant
		_scrolls.put(98000, new EnchantScroll(true, false, false, false, false, false, true, CrystalType.S, 20, 0, null));
		_scrolls.put(98001, new EnchantScroll(false, false, false, false, false, false, true, CrystalType.S, 20, 0, null));
	}

	/**
	 * @param scroll The instance of item to make checks on.
	 * @return enchant template for scroll.
	 */
	public static final EnchantScroll getEnchantScroll(ItemInstance scroll)
	{
		return _scrolls.get(scroll.getItemId());
	}

	/**
	 * @param item The instance of item to make checks on.
	 * @return true if item can be enchanted.
	 */
	public static final boolean isEnchantable(ItemInstance item)
	{
		if (item.isHeroItem() || item.isShadowItem() || item.isEtcItem() || item.getItem().getItemType() == WeaponType.FISHINGROD)
			return false;

		// only equipped items or in inventory can be enchanted
		if (item.getLocation() != ItemInstance.ItemLocation.INVENTORY && item.getLocation() != ItemInstance.ItemLocation.PAPERDOLL)
			return false;

		return true;
	}
}