package inertia.model.enums;

/**
 * @author Junior
 *
 */
public enum EResurrectionSkill
{
	NO_RESURRECTION(0, 0), // Sem skill
    RESURRECTION(1016, 1), // Resurrection
    MASS_RESURRECTION(1254, 2); // Mass Resurrection
   
    
    private final int _skillId;
    private final int _priority;
    
    private EResurrectionSkill(int skillId, int priority)
    {
        _skillId = skillId;
        _priority = priority;
    }
    
    public int getSkillId()
    {
        return _skillId;
    }
    
    public int getPriority()
    {
        return _priority;
    }
    
    public static EResurrectionSkill getResurrectionSkillForClass(int classId)
    {
        switch(classId)
        {
        	case 97:
        	case 98:
        	case 105:
        	case 112:
        		return RESURRECTION;
            default:
                return NO_RESURRECTION;
        }
    }
    
    public static boolean isResurrectionSkill(int skillId)
    {
        for (EResurrectionSkill skill : values())
        {
            if (skill.getSkillId() == skillId)
            {
                return true;
            }
        }
        return false;
    }
    
    public static EResurrectionSkill getBySkillId(int skillId)
    {
        for (EResurrectionSkill skill : values())
        {
            if (skill.getSkillId() == skillId)
            {
                return skill;
            }
        }
        return NO_RESURRECTION;
    }
}
