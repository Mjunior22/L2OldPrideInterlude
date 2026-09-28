/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.base;

import static net.sf.l2j.gameserver.model.base.ClassLevel.First;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Fourth;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Second;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Third;
import static net.sf.l2j.gameserver.model.base.ClassType.ARCHER;
import static net.sf.l2j.gameserver.model.base.ClassType.DAGGER;
import static net.sf.l2j.gameserver.model.base.ClassType.TANK;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public enum PlayerClass
{
	Human_Fighter(ClassRace.HUMAN, ClassType.FIGHTER, First),
	Warrior(ClassRace.HUMAN, ClassType.FIGHTER, Second),
	Gladiator(ClassRace.HUMAN, ClassType.FIGHTER, Third),
	Warlord(ClassRace.HUMAN, ClassType.FIGHTER, Third),
	Human_Knight(ClassRace.HUMAN, TANK, Second),
	Paladin(ClassRace.HUMAN, TANK, Third),
	Dark_Avenger(ClassRace.HUMAN, TANK, Third),
	Rogue(ClassRace.HUMAN, ClassType.FIGHTER, Second),
	Treasure_Hunter(ClassRace.HUMAN, DAGGER, Third),
	Hawkeye(ClassRace.HUMAN, ARCHER, Third),
	Human_Mystic(ClassRace.HUMAN, ClassType.MYSTIC, First),
	Human_Wizard(ClassRace.HUMAN, ClassType.MYSTIC, Second),
	Sorceror(ClassRace.HUMAN, ClassType.MYSTIC, Third),
	Necromancer(ClassRace.HUMAN, ClassType.MYSTIC, Third),
	Warlock(ClassRace.HUMAN, ClassType.MYSTIC, Third),
	Cleric(ClassRace.HUMAN, ClassType.PRIEST, Second),
	Bishop(ClassRace.HUMAN, ClassType.PRIEST, Third),
	Prophet(ClassRace.HUMAN, ClassType.PRIEST, Third),
	Elven_Fighter(ClassRace.ELF, ClassType.FIGHTER, First),
	Elven_Knight(ClassRace.ELF, TANK, Second),
	Temple_Knight(ClassRace.ELF, TANK, Third),
	Swordsinger(ClassRace.ELF, TANK, Third),
	Elven_Scout(ClassRace.ELF, ClassType.FIGHTER, Second),
	Plainswalker(ClassRace.ELF, DAGGER, Third),
	Silver_Ranger(ClassRace.ELF, ARCHER, Third),
	Elven_Mystic(ClassRace.ELF, ClassType.MYSTIC, First),
	Elven_Wizard(ClassRace.ELF, ClassType.MYSTIC, Second),
	Spellsinger(ClassRace.ELF, ClassType.MYSTIC, Third),
	Elemental_Summoner(ClassRace.ELF, ClassType.MYSTIC, Third),
	Elven_Oracle(ClassRace.ELF, ClassType.PRIEST, Second),
	Elven_Elder(ClassRace.ELF, ClassType.PRIEST, Third),
	Dark_Elven_Fighter(ClassRace.DARK_ELF, ClassType.FIGHTER, First),
	Palus_Knight(ClassRace.DARK_ELF, TANK, Second),
	Shillien_Knight(ClassRace.DARK_ELF, TANK, Third),
	Bladedancer(ClassRace.DARK_ELF, ClassType.FIGHTER, Third),
	Assassin(ClassRace.DARK_ELF, ClassType.FIGHTER, Second),
	Abyss_Walker(ClassRace.DARK_ELF, DAGGER, Third),
	Phantom_Ranger(ClassRace.DARK_ELF, ARCHER, Third),
	Dark_Elven_Mystic(ClassRace.DARK_ELF, ClassType.MYSTIC, First),
	Dark_Elven_Wizard(ClassRace.DARK_ELF, ClassType.MYSTIC, Second),
	Spellhowler(ClassRace.DARK_ELF, ClassType.MYSTIC, Third),
	Phantom_Summoner(ClassRace.DARK_ELF, ClassType.MYSTIC, Third),
	Shillien_Oracle(ClassRace.DARK_ELF, ClassType.PRIEST, Second),
	Shillien_Elder(ClassRace.DARK_ELF, ClassType.PRIEST, Third),
	Orc_Fighter(ClassRace.ORC, ClassType.FIGHTER, First),
	Orc_Raider(ClassRace.ORC, ClassType.FIGHTER, Second),
	Destroyer(ClassRace.ORC, ClassType.FIGHTER, Third),
	Orc_Monk(ClassRace.ORC, ClassType.MYSTIC, Second),
	Tyrant(ClassRace.ORC, ClassType.FIGHTER, Third),
	Orc_Mystic(ClassRace.ORC, ClassType.MYSTIC, First),
	Orc_Shaman(ClassRace.ORC, ClassType.MYSTIC, Second),
	Overlord(ClassRace.ORC, ClassType.PRIEST, Third),
	Warcryer(ClassRace.ORC, ClassType.FIGHTER, Third),
	Dwarven_Fighter(ClassRace.DWARF, ClassType.FIGHTER, First),
	Dwarven_Scavenger(ClassRace.DWARF, ClassType.FIGHTER, Second),
	Bounty_Hunter(ClassRace.DWARF, ClassType.FIGHTER, Third),
	Dwarven_Artisan(ClassRace.DWARF, ClassType.FIGHTER, Second),
	Warsmith(ClassRace.DWARF, ClassType.FIGHTER, Third),
	dummyEntry1(null, null, null),
	dummyEntry2(null, null, null),
	dummyEntry3(null, null, null),
	dummyEntry4(null, null, null),
	dummyEntry5(null, null, null),
	dummyEntry6(null, null, null),
	dummyEntry7(null, null, null),
	dummyEntry8(null, null, null),
	dummyEntry9(null, null, null),
	dummyEntry10(null, null, null),
	dummyEntry11(null, null, null),
	dummyEntry12(null, null, null),
	dummyEntry13(null, null, null),
	dummyEntry14(null, null, null),
	dummyEntry15(null, null, null),
	dummyEntry16(null, null, null),
	dummyEntry17(null, null, null),
	dummyEntry18(null, null, null),
	dummyEntry19(null, null, null),
	dummyEntry20(null, null, null),
	dummyEntry21(null, null, null),
	dummyEntry22(null, null, null),
	dummyEntry23(null, null, null),
	dummyEntry24(null, null, null),
	dummyEntry25(null, null, null),
	dummyEntry26(null, null, null),
	dummyEntry27(null, null, null),
	dummyEntry28(null, null, null),
	dummyEntry29(null, null, null),
	dummyEntry30(null, null, null),
	// 3rd classes
	Duelist(ClassRace.HUMAN, ClassType.FIGHTER, Fourth),
	Dreadnought(ClassRace.HUMAN, ClassType.FIGHTER, Fourth),
	Phoenix_Knight(ClassRace.HUMAN, TANK, Fourth),
	Hell_Knight(ClassRace.HUMAN, TANK, Fourth),
	Sagittarius(ClassRace.HUMAN, ARCHER, Fourth),
	Adventurer(ClassRace.HUMAN, DAGGER, Fourth),
	Archmage(ClassRace.HUMAN, ClassType.MYSTIC, Fourth),
	Soultaker(ClassRace.HUMAN, ClassType.MYSTIC, Fourth),
	Arcana_Lord(ClassRace.HUMAN, ClassType.MYSTIC, Fourth),
	Cardinal(ClassRace.HUMAN, ClassType.PRIEST, Fourth),
	Hierophant(ClassRace.HUMAN, ClassType.FIGHTER, Fourth),
	Eva_Templar(ClassRace.ELF, TANK, Fourth),
	Sword_Muse(ClassRace.ELF, ClassType.FIGHTER, Fourth),
	Wind_Rider(ClassRace.ELF, DAGGER, Fourth),
	Moonlight_Sentinel(ClassRace.ELF, ARCHER, Fourth),
	Mystic_Muse(ClassRace.ELF, ClassType.MYSTIC, Fourth),
	Elemental_Master(ClassRace.ELF, ClassType.MYSTIC, Fourth),
	Eva_Saint(ClassRace.ELF, ClassType.PRIEST, Fourth),
	Shillien_Templar(ClassRace.DARK_ELF, TANK, Fourth),
	Spectral_Dancer(ClassRace.DARK_ELF, ClassType.FIGHTER, Fourth),
	Ghost_Hunter(ClassRace.DARK_ELF, DAGGER, Fourth),
	Ghost_Sentinel(ClassRace.DARK_ELF, ARCHER, Fourth),
	Storm_Screamer(ClassRace.DARK_ELF, ClassType.MYSTIC, Fourth),
	Spectral_Master(ClassRace.DARK_ELF, ClassType.MYSTIC, Fourth),
	Shillien_Saint(ClassRace.DARK_ELF, ClassType.MYSTIC, Fourth),
	Titan(ClassRace.ORC, ClassType.FIGHTER, Fourth),
	Grand_Khauatari(ClassRace.ORC, ClassType.FIGHTER, Fourth),
	Dominator(ClassRace.ORC, ClassType.PRIEST, Fourth),
	Doomcryer(ClassRace.ORC, ClassType.FIGHTER, Fourth),
	Fortune_Seeker(ClassRace.DWARF, ClassType.FIGHTER, Fourth),
	Maestro(ClassRace.DWARF, ClassType.FIGHTER, Fourth);
	
	private ClassRace _race;
	private ClassLevel _level;
	private ClassType _type;
	private static final Set<PlayerClass> mainSubclassSet;

	private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap = new EnumMap<>(PlayerClass.class);

	static
	{
		final Set<PlayerClass> subclasses = getSet(null, Fourth);
		mainSubclassSet = subclasses;
	}

	PlayerClass(final ClassRace pRace, final ClassType pType, final ClassLevel pLevel)
	{
		_race = pRace;
		_level = pLevel;
		_type = pType;
	}

	@Override
	public String toString()
	{
		return super.toString().replace("__", " ").replace("_", " ").replace("SUMMONER", "Summoner");
	}

	public final Set<PlayerClass> getAvailableSubclasses(final Player player)
	{
		Set<PlayerClass> subclasses = null;
		if (_level == Fourth)
		{
			subclasses = EnumSet.copyOf(mainSubclassSet);
			subclasses.remove(this);

			final Set<PlayerClass> unavailableClasses = subclassSetMap.get(this);
			if (unavailableClasses != null)
				subclasses.removeAll(unavailableClasses);
		}
		return subclasses;
	}

	public static final EnumSet<PlayerClass> getSet(final ClassRace race, final ClassLevel level)
	{
		final EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);
		for (final PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
			if (race == null || playerClass.isOfRace(race))
				if (level == null || playerClass.isOfLevel(level))
					allOf.add(playerClass);
				
		return allOf;
	}

	public final boolean isOfRace(final ClassRace pRace)
	{
		return _race == pRace;
	}

	public final boolean isOfType(final ClassType pType)
	{
		return _type.isOfType(pType);
	}

	public final boolean isOfLevel(final ClassLevel pLevel)
	{
		return _level == pLevel;
	}

	public final ClassLevel getLevel()
	{
		return _level;
	}

	public ClassType getType()
	{
		return _type;
	}
}
