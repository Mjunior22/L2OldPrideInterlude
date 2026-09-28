package net.sf.l2j.gameserver.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ChanceCondition;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.base.ClassRace;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.skills.basefuncs.FuncTemplate;
import net.sf.l2j.gameserver.skills.basefuncs.Lambda;
import net.sf.l2j.gameserver.skills.basefuncs.LambdaCalc;
import net.sf.l2j.gameserver.skills.basefuncs.LambdaConst;
import net.sf.l2j.gameserver.skills.basefuncs.LambdaStats;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.conditions.ConditionCoupleDistance;
import net.sf.l2j.gameserver.skills.conditions.ConditionCoupleHp;
import net.sf.l2j.gameserver.skills.conditions.ConditionElementSeed;
import net.sf.l2j.gameserver.skills.conditions.ConditionForceBuff;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameChance;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameTime;
import net.sf.l2j.gameserver.skills.conditions.ConditionLogicAnd;
import net.sf.l2j.gameserver.skills.conditions.ConditionLogicNot;
import net.sf.l2j.gameserver.skills.conditions.ConditionLogicOr;
import net.sf.l2j.gameserver.skills.conditions.ConditionMaxDistance;
import net.sf.l2j.gameserver.skills.conditions.ConditionMinDistance;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerActiveEffectId;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerActiveSkillId;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerCharges;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerClassIdRestriction;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerCp;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerHasCastle;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerHasClanHall;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerHasSkill;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerHp;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerHpPercentage;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerInvSize;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerIsHero;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerLevel;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerMp;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerPkCount;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerPledgeClass;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerRace;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerSex;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState.PlayerState;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerWeight;
import net.sf.l2j.gameserver.skills.conditions.ConditionSkillStats;
import net.sf.l2j.gameserver.skills.conditions.ConditionSlotItemId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetActiveEffectId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetActiveSkillId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetClassIdRestriction;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetHasSkill;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetHp;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetHpMinMax;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetNpcId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetRace;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetRaceId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetState;
import net.sf.l2j.gameserver.skills.conditions.ConditionUsingItemType;
import net.sf.l2j.gameserver.skills.conditions.ConditionUsingSkill;
import net.sf.l2j.gameserver.skills.conditions.ConditionUsingSkillId;
import net.sf.l2j.gameserver.skills.conditions.ConditionWithSkill;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author mkizub
 */
abstract class DocumentBase
{
	static Logger _log = Logger.getLogger(DocumentBase.class.getName());
	
	private final File _file;
	protected Map<String, String[]> _tables;
	
	DocumentBase(File pFile)
	{
		_file = pFile;
		_tables = new HashMap<>();
	}
	
	public Document parse()
	{
		Document doc;
		try
		{
			doc = XMLDocumentFactory.getInstance().loadDocument(_file);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error loading file " + _file, e);
			return null;
		}
		
		try
		{
			parseDocument(doc);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error in file " + _file, e);
			return null;
		}
		return doc;
	}
	
	protected abstract void parseDocument(Document doc);
	
	protected abstract StatsSet getStatsSet();
	
	protected abstract String getTableValue(String name);
	
	protected abstract String getTableValue(String name, int idx);
	
	protected void resetTable()
	{
		_tables = new HashMap<>();
	}
	
	protected void setTable(String name, String[] table)
	{
		_tables.put(name, table);
	}
	
	protected void parseTemplate(Node n, Object template)
	{
		Condition condition = null;
		n = n.getFirstChild();
		if (n == null)
			return;
		
		if ("cond".equalsIgnoreCase(n.getNodeName()))
		{
			condition = parseCondition(n.getFirstChild(), template);
			Node msg = n.getAttributes().getNamedItem("msg");
			Node msgId = n.getAttributes().getNamedItem("msgId");
			if (condition != null && msg != null)
				condition.setMessage(msg.getNodeValue());
			else if (condition != null && msgId != null)
			{
				condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
				Node addName = n.getAttributes().getNamedItem("addName");
				if (addName != null && Integer.decode(getValue(msgId.getNodeValue(), null)) > 0)
					condition.addName();
			}
			n = n.getNextSibling();
		}
		
		for (; n != null; n = n.getNextSibling())
		{
			if ("add".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "Add", condition);
			else if ("addMul".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "AddMul", condition);
			else if ("sub".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "Sub", condition);
			else if ("subDiv".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "SubDiv", condition);
			else if ("mul".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "Mul", condition);
			else if ("basemul".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "BaseMul", condition);
			else if ("div".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "Div", condition);
			else if ("set".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "Set", condition);
			else if ("enchant".equalsIgnoreCase(n.getNodeName()))
				attachFunc(n, template, "Enchant", condition);
			else if ("effect".equalsIgnoreCase(n.getNodeName()))
			{
				if (template instanceof EffectTemplate)
					throw new RuntimeException("Nested effects");
				
				attachEffect(n, template, condition);
			}
		}
	}
	
	protected void attachFunc(Node n, Object template, String name, Condition attachCond)
	{
		Stats stat = Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		String order = n.getAttributes().getNamedItem("order").getNodeValue();
		Lambda lambda = getLambda(n, template);
		int ord = Integer.decode(getValue(order, template));
		Condition applayCond = parseCondition(n.getFirstChild(), template);
		FuncTemplate ft = new FuncTemplate(attachCond, applayCond, name, stat, ord, lambda);
		
		if (template instanceof Item)
			((Item) template).attach(ft);
		else if (template instanceof L2Skill)
			((L2Skill) template).attach(ft);
		else if (template instanceof EffectTemplate)
			((EffectTemplate) template).attach(ft);
	}
	
	protected void attachLambdaFunc(Node n, Object template, LambdaCalc calc)
	{
		String name = n.getNodeName();
		final StringBuilder sb = new StringBuilder(name);
		sb.setCharAt(0, Character.toUpperCase(name.charAt(0)));
		name = sb.toString();
		Lambda lambda = getLambda(n, template);
		FuncTemplate ft = new FuncTemplate(null, null, name, null, calc.getFuncs().size(), lambda);
		calc.addFunc(ft.getFunc(new Env(), calc));
	}
	
	protected void attachEffect(Node n, Object template, Condition attachCond)
	{
		NamedNodeMap attrs = n.getAttributes();
		String name = this.getValue(attrs.getNamedItem("name").getNodeValue().intern(), template);
		int time = 1;
		int count = 1;
		if (attrs.getNamedItem("count") != null)
		{
			count = Integer.decode(this.getValue(attrs.getNamedItem("count").getNodeValue(), template));
		}
		
		if (attrs.getNamedItem("time") != null)
		{
			time = Integer.decode(this.getValue(attrs.getNamedItem("time").getNodeValue(), template));
		}
		
		if (Config.ENABLE_ALTERNATIVE_SKILL_DURATION)
		{
			if (Config.SKILL_DURATION_LIST.containsKey(((L2Skill) template).getId()))
			{
				if (((L2Skill) template).getLevel() < 100)
					time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
				else if (((L2Skill) template).getLevel() >= 100 && ((L2Skill) template).getLevel() < 140)
					time += Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
				else if (((L2Skill) template).getLevel() > 140)
					time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
			}
		}
		else if (((L2Skill) template).getBuffDuration() > 0)
		{
			time = ((L2Skill) template).getBuffDuration() / 1000 / count;
		}
		
		boolean self = false;
		if (attrs.getNamedItem("self") != null && Integer.decode(this.getValue(attrs.getNamedItem("self").getNodeValue(), template)) == 1)
		{
			self = true;
		}
		
		boolean icon = true;
		if (attrs.getNamedItem("noicon") != null && Integer.decode(this.getValue(attrs.getNamedItem("noicon").getNodeValue(), template)) == 1)
		{
			icon = false;
		}
		
		boolean msg = true;
		if (attrs.getNamedItem("nomsg") != null && Integer.decode(this.getValue(attrs.getNamedItem("nomsg").getNodeValue(), template)) == 1)
		{
			msg = false;
		}
		
		Lambda lambda = this.getLambda(n, template);
		Condition applayCond = this.parseCondition(n.getFirstChild(), template);
		AbnormalEffect abnormal = AbnormalEffect.NULL;
		String stackType;
		if (attrs.getNamedItem("abnormal") != null)
		{
			stackType = attrs.getNamedItem("abnormal").getNodeValue();
			abnormal = AbnormalEffect.getByName(stackType);
		}
		
		stackType = "none";
		if (attrs.getNamedItem("stackType") != null)
		{
			stackType = attrs.getNamedItem("stackType").getNodeValue();
		}
		
		float stackOrder = 0.0F;
		if (attrs.getNamedItem("stackOrder") != null)
		{
			stackOrder = Float.parseFloat(this.getValue(attrs.getNamedItem("stackOrder").getNodeValue(), template));
		}
		
		double effectPower = -1.0D;
		if (attrs.getNamedItem("effectPower") != null)
		{
			effectPower = Double.parseDouble(this.getValue(attrs.getNamedItem("effectPower").getNodeValue(), template));
		}
		
		L2SkillType type = null;
		if (attrs.getNamedItem("effectType") != null)
		{
			String typeName = this.getValue(attrs.getNamedItem("effectType").getNodeValue(), template);
			
			try
			{
				type = Enum.valueOf(L2SkillType.class, typeName);
			}
			catch (Exception var27)
			{
				throw new IllegalArgumentException("Not skilltype found for: " + typeName);
			}
		}
		
		if (effectPower > -1 && type == null && name != "StoneSkin" && _log.isLoggable(Level.WARNING))
			_log.log(Level.WARNING, "Missing effectType for effect: " + name + " " + ((L2Skill) template).getId());
		
		boolean isChanceSkillTrigger = name == EffectChanceSkillTrigger.class.getName();
		int trigId = 0;
		if (attrs.getNamedItem("triggeredId") != null)
		{
			trigId = Integer.parseInt(this.getValue(attrs.getNamedItem("triggeredId").getNodeValue(), template));
		}
		else if (isChanceSkillTrigger)
		{
			throw new NoSuchElementException(name + " requires triggerId");
		}
		
		int trigLvl = 1;
		if (attrs.getNamedItem("triggeredLevel") != null)
		{
			trigLvl = Integer.parseInt(this.getValue(attrs.getNamedItem("triggeredLevel").getNodeValue(), template));
		}
		
		String chanceCond = null;
		if (attrs.getNamedItem("chanceType") != null)
		{
			chanceCond = this.getValue(attrs.getNamedItem("chanceType").getNodeValue(), template);
		}
		else if (isChanceSkillTrigger)
		{
			throw new NoSuchElementException(name + " requires chanceType");
		}
		
		int activationChance = -1;
		if (attrs.getNamedItem("activationChance") != null)
		{
			activationChance = Integer.parseInt(this.getValue(attrs.getNamedItem("activationChance").getNodeValue(), template));
		}
		
		ChanceCondition chance = ChanceCondition.parse(chanceCond, activationChance);
		if (chance == null && isChanceSkillTrigger)
		{
			throw new NoSuchElementException("Invalid chance condition: " + chanceCond + " " + activationChance);
		}
		boolean ignoreProtection = false;
		if (attrs.getNamedItem("ignorep") != null && Integer.decode(this.getValue(attrs.getNamedItem("ignorep").getNodeValue(), template)) == 1)
		{
			ignoreProtection = true;
		}
		
		EffectTemplate lt = new EffectTemplate(attachCond, applayCond, name, lambda, count, time, abnormal, stackType, stackOrder, icon, effectPower, type, trigId, trigLvl, chance, msg, ignoreProtection);
		this.parseTemplate(n, lt);
		if (template instanceof L2Skill)
		{
			if (self)
			{
				((L2Skill) template).attachSelf(lt);
			}
			else
			{
				((L2Skill) template).attach(lt);
			}
		}
	}
	
	protected Condition parseCondition(Node n, Object template)
	{
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE)
			n = n.getNextSibling();
		
		if (n == null)
			return null;
		
		if ("and".equalsIgnoreCase(n.getNodeName()))
			return parseLogicAnd(n, template);
		
		if ("or".equalsIgnoreCase(n.getNodeName()))
			return parseLogicOr(n, template);
		
		if ("not".equalsIgnoreCase(n.getNodeName()))
			return parseLogicNot(n, template);
		
		if ("player".equalsIgnoreCase(n.getNodeName()))
			return parsePlayerCondition(n, template);
		
		if ("target".equalsIgnoreCase(n.getNodeName()))
			return parseTargetCondition(n, template);
		
		if ("skill".equalsIgnoreCase(n.getNodeName()))
			return parseSkillCondition(n);
		
		if ("using".equalsIgnoreCase(n.getNodeName()))
			return parseUsingCondition(n);
		
		if ("game".equalsIgnoreCase(n.getNodeName()))
			return parseGameCondition(n);
		
		return null;
	}
	
	protected Condition parseLogicAnd(Node n, Object template)
	{
		ConditionLogicAnd cond = new ConditionLogicAnd();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if (n.getNodeType() == Node.ELEMENT_NODE)
				cond.add(parseCondition(n, template));
			
		if (cond.conditions == null || cond.conditions.length == 0)
			_log.severe("Empty <and> condition in " + _file);
		
		return cond;
	}
	
	protected Condition parseLogicOr(Node n, Object template)
	{
		ConditionLogicOr cond = new ConditionLogicOr();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if (n.getNodeType() == Node.ELEMENT_NODE)
				cond.add(parseCondition(n, template));
			
		if (cond.conditions == null || cond.conditions.length == 0)
			_log.severe("Empty <or> condition in " + _file);
		
		return cond;
	}
	
	protected Condition parseLogicNot(Node n, Object template)
	{
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if (n.getNodeType() == Node.ELEMENT_NODE)
				return new ConditionLogicNot(parseCondition(n, template));
			
		_log.severe("Empty <not> condition in " + _file);
		return null;
	}
	
	protected Condition parsePlayerCondition(Node n, Object template)
	{
		Condition cond = null;
		int[] ElementSeeds = new int[5];
		byte[] forces = new byte[2];
		NamedNodeMap attrs = n.getAttributes();
		
		for (int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			if ("race".equalsIgnoreCase(a.getNodeName()))
			{
				ClassRace race = ClassRace.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerRace(race));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				int lvl = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerLevel(lvl));
			}
			else if ("resting".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RESTING, val));
			}
			else if ("riding".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RIDING, val));
			}
			else if ("flying".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FLYING, val));
			}
			else if ("moving".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.MOVING, val));
			}
			else if ("running".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RUNNING, val));
			}
			else if ("behind".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.BEHIND, val));
			}
			else if ("front".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FRONT, val));
			}
			// ADDED BY VEGA
			
			else if ("hasskill".equalsIgnoreCase(a.getNodeName()))
			{
				int skill = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHasSkill(skill));
			}
			else if ("cp".equalsIgnoreCase(a.getNodeName()))
			{
				int cp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerCp(cp));
			}
			else if ("flagged".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FLAGGED, val));
			}
			else if ("combat".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.COMBAT, val));
			}
			else if ("attacking".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.ATTACKING, val));
			}
			else if ("casting".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.CASTING, val));
			}
			else if ("bleeding".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.BLEEDING, val));
			}
			else if ("invisible".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.INVISIBLE, val));
			}
			else if ("chaotic".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.CHAOTIC, val));
			}
			
			else if ("olympiad".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(PlayerState.OLYMPIAD, val));
			}
			else if ("ishero".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerIsHero(val));
			}
			else if ("hp".equalsIgnoreCase(a.getNodeName()))
			{
				int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHp(hp));
			}
			else if ("coupleHp".equalsIgnoreCase(a.getNodeName()))
			{
				int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionCoupleHp(hp));
			}
			else if ("coupleDistance".equalsIgnoreCase(a.getNodeName()))
			{
				int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionCoupleDistance(hp));
			}
			else if ("hprate".equalsIgnoreCase(a.getNodeName()))
			{
				double rate = Double.parseDouble(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHpPercentage(rate));
			}
			else if ("mp".equalsIgnoreCase(a.getNodeName()))
			{
				int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerMp(hp));
			}
			else if ("pkCount".equalsIgnoreCase(a.getNodeName()))
			{
				int expIndex = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerPkCount(expIndex));
			}
			else if ("battle_force".equalsIgnoreCase(a.getNodeName()))
			{
				forces[0] = Byte.decode(getValue(a.getNodeValue(), null));
			}
			else if ("spell_force".equalsIgnoreCase(a.getNodeName()))
			{
				forces[1] = Byte.decode(getValue(a.getNodeValue(), null));
			}
			else if ("charges".equalsIgnoreCase(a.getNodeName()))
			{
				int value = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerCharges(value));
			}
			else if ("weight".equalsIgnoreCase(a.getNodeName()))
			{
				int weight = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerWeight(weight));
			}
			else if ("invSize".equalsIgnoreCase(a.getNodeName()))
			{
				int size = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerInvSize(size));
			}
			else if ("pledgeClass".equalsIgnoreCase(a.getNodeName()))
			{
				int pledgeClass = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerPledgeClass(pledgeClass));
			}
			else if ("clanHall".equalsIgnoreCase(a.getNodeName()))
			{
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerHasClanHall(array));
			}
			else if ("castle".equalsIgnoreCase(a.getNodeName()))
			{
				int castle = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHasCastle(castle));
			}
			else if ("sex".equalsIgnoreCase(a.getNodeName()))
			{
				int sex = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerSex(sex));
			}
			else if ("active_effect_id".equalsIgnoreCase(a.getNodeName()))
			{
				int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id));
			}
			else if ("active_effect_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				String val = getValue(a.getNodeValue(), template);
				int effect_id = Integer.decode(getValue(val.split(",")[0], template));
				int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id, effect_lvl));
			}
			else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
			{
				int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id));
			}
			else if ("active_skill_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				String val = getValue(a.getNodeValue(), template);
				int skill_id = Integer.decode(getValue(val.split(",")[0], template));
				int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id, skill_lvl));
			}
			else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
			{
				ArrayList<Integer> array = new ArrayList<>();
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerClassIdRestriction(array));
			}
			else if ("seed_fire".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[0] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_water".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[1] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_wind".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[2] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_various".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[3] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_any".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[4] = Integer.decode(getValue(a.getNodeValue(), null));
			}
		}
		
		// Elemental seed condition processing
		for (int elementSeed : ElementSeeds)
		{
			if (elementSeed > 0)
			{
				cond = joinAnd(cond, new ConditionElementSeed(ElementSeeds));
				break;
			}
		}
		
		if (forces[0] + forces[1] > 0)
			cond = joinAnd(cond, new ConditionForceBuff(forces));
		
		if (cond == null)
			_log.severe("Unrecognized <player> condition in " + _file);
		
		return cond;
	}
	
	protected Condition parseTargetCondition(Node n, Object template)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			if ("hp_min_max".equalsIgnoreCase(a.getNodeName()))
			{
				String val = getValue(a.getNodeValue(), template);
				int hpMin = Integer.decode(getValue(val.split(",")[0], template));
				int hpMax = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionTargetHpMinMax(hpMin, hpMax));
			}
			else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
			{
				int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id));
			}
			else if ("race_id".equalsIgnoreCase(a.getNodeName()))
			{
				List<Integer> array = new ArrayList<>();
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetRaceId(array));
			}
			else if ("npcId".equalsIgnoreCase(a.getNodeName()))
			{
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetNpcId(array));
			}
			
			// ADDED BY VEGA
			
			else if ("active_effect_id".equalsIgnoreCase(a.getNodeName()))
			{
				int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id));
			}
			else if ("active_effect_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				String val = getValue(a.getNodeValue(), template);
				int effect_id = Integer.decode(getValue(val.split(",")[0], template));
				int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id, effect_lvl));
			}
			else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
			{
				int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id));
			}
			else if ("active_skill_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				String val = getValue(a.getNodeValue(), template);
				int skill_id = Integer.decode(getValue(val.split(",")[0], template));
				int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id, skill_lvl));
			}
			else if ("race".equalsIgnoreCase(a.getNodeName()))
			{
				ClassRace race = ClassRace.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetRace(race));
			}
			else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
			{
				ArrayList<Integer> array = new ArrayList<>();
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetClassIdRestriction(array));
			}
			else if ("hasSkill".equalsIgnoreCase(a.getNodeName()))
			{
				int skill = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionTargetHasSkill(skill));
			}
			else if ("mindistance".equalsIgnoreCase(a.getNodeName()))
			{
				int distance = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionMinDistance(distance * distance));
			}
			else if ("maxdistance".equalsIgnoreCase(a.getNodeName()))
			{
				int distance = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionMaxDistance(distance * distance));
			}
			else if ("behind".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.BEHIND, val));
			}
			else if ("front".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.FRONT, val));
			}
			else if ("flagged".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.FLAGGED, val));
			}
			else if ("combat".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.COMBAT, val));
			}
			else if ("attacking".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.ATTACKING, val));
			}
			else if ("casting".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.CASTING, val));
			}
			else if ("bleeding".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.BLEEDING, val));
			}
			else if ("invisible".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.INVISIBLE, val));
			}
			else if ("chaotic".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.CHAOTIC, val));
			}
			else if ("olympiad".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetState(PlayerState.OLYMPIAD, val));
			}
			else if ("hp".equalsIgnoreCase(a.getNodeName()))
			{
				int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionTargetHp(hp));
			}
		}
		
		if (cond == null)
			_log.severe("Unrecognized <target> condition in " + _file);
		
		return cond;
	}
	
	protected Condition parseSkillCondition(Node n)
	{
		NamedNodeMap attrs = n.getAttributes();
		Stats stat = Stats.valueOfXml(attrs.getNamedItem("stat").getNodeValue());
		return new ConditionSkillStats(stat);
	}
	
	protected Condition parseUsingCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			if ("kind".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					for (WeaponType wt : WeaponType.values())
					{
						if (wt.toString().equalsIgnoreCase(item))
						{
							mask |= wt.mask();
							break;
						}
					}
					for (ArmorType at : ArmorType.values())
					{
						if (at.toString().equalsIgnoreCase(item))
						{
							mask |= at.mask();
							break;
						}
					}
				}
				cond = joinAnd(cond, new ConditionUsingItemType(mask));
			}
			
			else if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				int id = Integer.parseInt(a.getNodeValue());
				cond = joinAnd(cond, new ConditionUsingSkill(id));
			}
			else if ("skillId".equalsIgnoreCase(a.getNodeName()))
			{
				int id = Integer.parseInt(a.getNodeValue());
				cond = joinAnd(cond, new ConditionUsingSkillId(id));
			}
			else if ("slotitem".equalsIgnoreCase(a.getNodeName()))
			{
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
				int id = Integer.parseInt(st.nextToken().trim());
				int slot = Integer.parseInt(st.nextToken().trim());
				int enchant = 0;
				if (st.hasMoreTokens())
					enchant = Integer.parseInt(st.nextToken().trim());
				cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
			}
		}
		
		if (cond == null)
			_log.severe("Unrecognized <using> condition in " + _file);
		
		return cond;
	}
	
	protected Condition parseGameCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			if ("night".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(val));
			}
			
			// ADDED BY VEGA
			if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionWithSkill(val));
			}
			if ("chance".equalsIgnoreCase(a.getNodeName()))
			{
				int val = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionGameChance(val));
			}
		}
		
		if (cond == null)
			_log.severe("Unrecognized <game> condition in " + _file);
		
		return cond;
	}
	
	protected void parseTable(Node n)
	{
		NamedNodeMap attrs = n.getAttributes();
		String name = attrs.getNamedItem("name").getNodeValue();
		
		if (name.charAt(0) != '#')
			throw new IllegalArgumentException("Table name must start with #");
		
		StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
		List<String> array = new ArrayList<>(data.countTokens());
		
		while (data.hasMoreTokens())
			array.add(data.nextToken());
		
		setTable(name, array.toArray(new String[array.size()]));
	}
	
	protected void parseBeanSet(Node n, StatsSet set, Integer level)
	{
		String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		char ch = value.length() == 0 ? ' ' : value.charAt(0);
		
		if (ch == '#' || ch == '-' || Character.isDigit(ch))
			set.set(name, String.valueOf(getValue(value, level)));
		else
			set.set(name, value);
	}
	
	protected Lambda getLambda(Node n, Object template)
	{
		Node nval = n.getAttributes().getNamedItem("val");
		if (nval != null)
		{
			String val = nval.getNodeValue();
			if (val.charAt(0) == '#')
			{ // table by level
				return new LambdaConst(Double.parseDouble(getTableValue(val)));
			}
			else if (val.charAt(0) == '$')
			{
				if (val.equalsIgnoreCase("$player_level"))
					return new LambdaStats(LambdaStats.StatsType.PLAYER_LEVEL);
				if (val.equalsIgnoreCase("$target_level"))
					return new LambdaStats(LambdaStats.StatsType.TARGET_LEVEL);
				if (val.equalsIgnoreCase("$player_max_hp"))
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_HP);
				if (val.equalsIgnoreCase("$player_max_mp"))
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_MP);
				// try to find value out of item fields
				StatsSet set = getStatsSet();
				String field = set.getString(val.substring(1));
				
				if (field != null)
					return new LambdaConst(Double.parseDouble(getValue(field, template)));
				
				// failed
				throw new IllegalArgumentException("Unknown value " + val);
			}
			else
				return new LambdaConst(Double.parseDouble(val));
		}
		LambdaCalc calc = new LambdaCalc();
		n = n.getFirstChild();
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE)
			n = n.getNextSibling();
		
		if (n == null || !"val".equals(n.getNodeName()))
			throw new IllegalArgumentException("Value not specified");
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			attachLambdaFunc(n, template, calc);
		}
		return calc;
	}
	
	protected String getValue(String value, Object template)
	{
		// is it a table?
		if (value.charAt(0) == '#')
		{
			if (template instanceof L2Skill)
				return getTableValue(value);
			else if (template instanceof Integer)
				return getTableValue(value, ((Integer) template).intValue());
			else
				throw new IllegalStateException();
		}
		return value;
	}
	
	protected Condition joinAnd(Condition cond, Condition c)
	{
		if (cond == null)
			return c;
		
		if (cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}