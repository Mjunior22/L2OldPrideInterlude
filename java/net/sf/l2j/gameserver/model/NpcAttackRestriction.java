package net.sf.l2j.gameserver.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.model.actor.Npc;

/**
 * Classe para gerenciar restrições de ataque a NPCs
 */
public class NpcAttackRestriction
{
    // Conjuntos para armazenar as restrições
    private static final Set<Integer> RESTRICTED_NPC_IDS = new HashSet<>();
    private static final Set<String> RESTRICTED_NPC_TYPES = new HashSet<>();
    private static final Set<String> RESTRICTED_NAME_KEYWORDS = new HashSet<>();
    
    // Mensagem padrão
    private static String RESTRICTION_MESSAGE = "You cannot attack this NPC.";
    
    /**
     * Carrega as configurações do arquivo
     */
    public static void load()
    {
        try
        {
            File configFile = new File("config/CustomMods/NpcRestrictions.ini");
            
            if (!configFile.exists())
            {
                // Cria arquivo padrão se não existir
                createDefaultConfig(configFile);
            }
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile))
            {
                props.load(fis);
            }
            
            // Carrega IDs restritos
            String restrictedIds = props.getProperty("RestrictedNpcIds", "");
            if (!restrictedIds.isEmpty())
            {
                StringTokenizer st = new StringTokenizer(restrictedIds, ",");
                while (st.hasMoreTokens())
                {
                    try
                    {
                        int npcId = Integer.parseInt(st.nextToken().trim());
                        RESTRICTED_NPC_IDS.add(npcId);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
            }
            
            // Carrega tipos restritos
            String restrictedTypes = props.getProperty("RestrictedNpcTypes", "");
            if (!restrictedTypes.isEmpty())
            {
                StringTokenizer st = new StringTokenizer(restrictedTypes, ",");
                while (st.hasMoreTokens())
                {
                    RESTRICTED_NPC_TYPES.add(st.nextToken().trim());
                }
            }
            
            // Carrega keywords restritos
            String restrictedKeywords = props.getProperty("RestrictedNameKeywords", "");
            if (!restrictedKeywords.isEmpty())
            {
                StringTokenizer st = new StringTokenizer(restrictedKeywords, ",");
                while (st.hasMoreTokens())
                {
                    RESTRICTED_NAME_KEYWORDS.add(st.nextToken().trim().toLowerCase());
                }
            }
            
            // Carrega mensagem
            String message = props.getProperty("RestrictionMessage");
            if (message != null && !message.isEmpty())
            {
                RESTRICTION_MESSAGE = message;
            }
            
            System.out.println("NPC Attack Restrictions loaded: " + 
                RESTRICTED_NPC_IDS.size() + " IDs, " + 
                RESTRICTED_NPC_TYPES.size() + " types, " + 
                RESTRICTED_NAME_KEYWORDS.size() + " keywords");
        }
        catch (Exception e)
        {
            System.err.println("Error loading NPC attack restrictions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cria configuração padrão
     * @param configFile 
     * @throws Exception 
     */
    private static void createDefaultConfig(File configFile) throws Exception
    {
        configFile.getParentFile().mkdirs();
        
        String defaultConfig = "# NPC Attack Restrictions Configuration\n" +
                               "# ====================================\n" +
                               "\n" +
                               "# NPC IDs that CANNOT be attacked (comma separated)\n" +
                               "RestrictedNpcIds = 30006,30059,30080,30134,30146,30233,30256,30320,30001,30002,30003,30004,30005\n" +
                               "\n" +
                               "# NPC Types that CANNOT be attacked (comma separated)\n" +
                               "RestrictedNpcTypes = L2Teleporter,L2Merchant,L2Warehouse,L2Fisherman,L2ClanHallManager,L2CastleTeleporter,L2SymbolMaker\n" +
                               "\n" +
                               "# Name keywords that indicate protected NPCs (comma separated, case insensitive)\n" +
                               "RestrictedNameKeywords = warehouse,merchant,blacksmith,gatekeeper,teleporter,gm,admin,buffer,manager,symbol,fisher,trader\n" +
                               "\n" +
                               "# Restriction message shown to players\n" +
                               "RestrictionMessage = You cannot attack this NPC.\n";
        
        java.nio.file.Files.write(configFile.toPath(), defaultConfig.getBytes());
        System.out.println("Created default NPC restrictions config at: " + configFile.getPath());
    }
    
    /**
     * Verifica se um NPC está na lista de restritos
     * @param npc 
     * @return 
     */
    public static boolean isNpcAttackRestricted(Npc npc)
    {
        // Verifica por ID
        if (RESTRICTED_NPC_IDS.contains(npc.getNpcId()))
            return true;
        
        // Verifica por tipo
        String npcType = npc.getTemplate().getType();
        if (npcType != null)
        {
            for (String restrictedType : RESTRICTED_NPC_TYPES)
            {
                if (npcType.contains(restrictedType))
                    return true;
            }
        }
        
        // Verifica por nome/keyword
        String npcName = npc.getName().toLowerCase();
        for (String keyword : RESTRICTED_NAME_KEYWORDS)
        {
            if (npcName.contains(keyword.toLowerCase()))
                return true;
        }
        
        // Verifica se é atacável pelo template (opcional)
        if (!npc.isAttackable())
            return true;
            
        return false;
    }
    
    /**
     * Retorna a mensagem de restrição
     * @return 
     */
    public static String getRestrictionMessage()
    {
        return RESTRICTION_MESSAGE;
    }
    
    /**
     * Adiciona um NPC à lista de restrições (runtime)
     * @param npcId 
     */
    public static void addRestrictedNpcId(int npcId)
    {
        RESTRICTED_NPC_IDS.add(npcId);
    }
    
    /**
     * Remove um NPC da lista de restrições (runtime)
     * @param npcId 
     */
    public static void removeRestrictedNpcId(int npcId)
    {
        RESTRICTED_NPC_IDS.remove(npcId);
    }
    
    /**
     * Limpa todas as restrições (runtime)
     */
    public static void clearRestrictions()
    {
        RESTRICTED_NPC_IDS.clear();
        RESTRICTED_NPC_TYPES.clear();
        RESTRICTED_NAME_KEYWORDS.clear();
    }
}