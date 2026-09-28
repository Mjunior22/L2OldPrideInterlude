package custom.soulcrystal;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junior
 *
 */
public class ItemEvolutionHolder
{
	 // Mapa: Arma Base ID -> [Arma Evoluída Opção 1, Opção 2, Opção 3]
    private static final Map<Integer, int[]> ITEM_EVOLUTIONS = new HashMap<>();
    
    static
    {
    	//UNIQUE WEAPONS
        ITEM_EVOLUTIONS.put(9600, new int[]{11000, 12000, 13000});
        ITEM_EVOLUTIONS.put(9601, new int[]{11001, 12001, 13001});
        ITEM_EVOLUTIONS.put(9602, new int[]{11002, 12002, 13002});
        ITEM_EVOLUTIONS.put(9603, new int[]{11003, 12003, 13003});
        ITEM_EVOLUTIONS.put(9604, new int[]{11004, 12004, 13004});
        ITEM_EVOLUTIONS.put(9605, new int[]{11005, 12005, 13005});
        ITEM_EVOLUTIONS.put(9606, new int[]{11006, 12006, 13006});
        ITEM_EVOLUTIONS.put(9607, new int[]{11007, 12007, 13007});
        ITEM_EVOLUTIONS.put(9608, new int[]{11008, 12008, 13008});
        ITEM_EVOLUTIONS.put(9609, new int[]{11009, 12009, 13009});
        ITEM_EVOLUTIONS.put(9610, new int[]{11010, 12010, 13010});
        ITEM_EVOLUTIONS.put(9611, new int[]{11011, 12011, 13011});
        ITEM_EVOLUTIONS.put(9612, new int[]{11012, 12012, 13012});
        ITEM_EVOLUTIONS.put(9613, new int[]{11013, 12013, 13013});
        ITEM_EVOLUTIONS.put(9614, new int[]{11014, 12014, 13014});
        ITEM_EVOLUTIONS.put(9615, new int[]{11015, 12015, 13015});
        ITEM_EVOLUTIONS.put(9616, new int[]{11016, 12016, 13016});
        ITEM_EVOLUTIONS.put(9617, new int[]{11017, 12017, 13017});
        ITEM_EVOLUTIONS.put(9618, new int[]{11018, 12018, 13018});
        ITEM_EVOLUTIONS.put(9619, new int[]{11019, 12019, 13019});
        ITEM_EVOLUTIONS.put(9621, new int[]{11021, 12021, 13021});
        ITEM_EVOLUTIONS.put(9622, new int[]{11022, 12022, 13022});
        
        //UNIQUE ARMORS
        ITEM_EVOLUTIONS.put(9501, new int[]{11101, 12101, 13101}); // CHEST DREAD HEAVY
        ITEM_EVOLUTIONS.put(9504, new int[]{11104, 12104, 13104}); // CHEST DREAD LIGHT
        ITEM_EVOLUTIONS.put(9507, new int[]{11107, 12107, 13107}); // CHEST DREAD ROBE
        ITEM_EVOLUTIONS.put(9512, new int[]{11112, 12112, 13112}); // CHEST TITANIUM HEAVY
        ITEM_EVOLUTIONS.put(9515, new int[]{11115, 12115, 13115}); // CHEST TITANIUM LIGHT
        ITEM_EVOLUTIONS.put(9518, new int[]{11118, 12118, 13118}); // CHEST TITANIUM ROBE
    }
    
    /**
     * Retorna as 3 opções de evolução para uma arma base
     * @param baseWeaponId ID da arma base
     * @return Array com 3 IDs das armas evoluídas, ou null se não encontrar
     */
    public static int[] getEvolutions(int baseWeaponId)
    {
        return ITEM_EVOLUTIONS.get(baseWeaponId);
    }
    
    /**
     * Verifica se uma arma pode evoluir
     * @param baseWeaponId ID da arma base
     * @return true se a arma tem evoluções configuradas
     */
    public static boolean canEvolve(int baseWeaponId)
    {
        return ITEM_EVOLUTIONS.containsKey(baseWeaponId);
    }
    
    /**
     * Retorna todas as armas que podem evoluir
     * @return Set com todos os IDs de armas base
     */
    public static java.util.Set<Integer> getAllEvolvableWeapons()
    {
        return ITEM_EVOLUTIONS.keySet();
    }
}
