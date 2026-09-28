package custom.effectprotection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Junior
 *
 */
public class PvpProtectionManager
{
    private static PvpProtectionManager _instance;
    private final Map<Integer, Map<String, Long>> _playerEffectCooldown;
    
    public enum EffectType
    {
        STUN(15000),      // 15 segundos
        PARALYZE(15000),
        SLEEP(15000),
        ROOT(15000),
        SILENCE(15000),
        FEAR(15000),
        CONFUSION(15000),
        DISARM(15000),
        KNOCKBACK(15000),
    	TRICK(15000);
        
        private final long delay;
        
        EffectType(long delay)
        {
            this.delay = delay;
        }
        
        public long getDelay()
        {
            return delay;
        }
    }
    
    private PvpProtectionManager()
    {
        _playerEffectCooldown = new ConcurrentHashMap<>();
    }
    
    public static PvpProtectionManager getInstance()
    {
        if (_instance == null)
        {
            _instance = new PvpProtectionManager();
        }
        return _instance;
    }
    
    /**
     * Verifica se um player pode receber um efeito específico de outro player
     * @param target Player que vai receber o efeito
     * @param attacker Player que está aplicando o efeito
     * @param effectType Tipo do efeito
     * @return true se pode aplicar, false se está em cooldown
     */
    public boolean canApplyEffect(Player target, Player attacker, EffectType effectType)
    {
        if (target == null || attacker == null)
            return true;
        
        if (!isPvPSituation(target, attacker))
            return true;
        
        String key = effectType.name();
        
        Map<String, Long> targetCooldowns = _playerEffectCooldown.get(target.getObjectId());
        if (targetCooldowns == null)
            return true;
        
        Long lastApplyTime = targetCooldowns.get(key);
        if (lastApplyTime != null)
        {
            long elapsed = System.currentTimeMillis() - lastApplyTime;
            if (elapsed < effectType.getDelay())
            {
                long remainingSeconds = (effectType.getDelay() - elapsed) / 1000;
                attacker.sendMessage(target.getName() + " is protected against " + effectType.name() + " for " + remainingSeconds + " more seconds!");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Registra que um efeito foi aplicado em um target
     * @param target Player que recebeu o efeito
     * @param effectType Tipo do efeito aplicado
     */
    public void registerEffect(Player target, EffectType effectType)
    {
        if (target == null)
            return;
        
        Map<String, Long> targetCooldowns = _playerEffectCooldown.computeIfAbsent(
            target.getObjectId(), 
            k -> new ConcurrentHashMap<>()
        );
        
        targetCooldowns.put(effectType.name(), System.currentTimeMillis());
        
        target.sendMessage("You are now protected against " + effectType.name() + " for " + (effectType.getDelay() / 1000) + " seconds!");
    }
    
    /**
     * Verifica se é uma situação de PvP entre dois players
     * @param target 
     * @param attacker 
     * @return 
     */
    private static boolean isPvPSituation(Player target, Player attacker)
    {
        if (target == null || attacker == null)
            return false;
        
        if (target.isPeaceZone() || attacker.isPeaceZone())
            return false;
        
        if (target == attacker)
            return false;
        
        return true;
    }
    
    /**
     * Limpa os cooldowns de um player (útil quando o player loga)
     * @param player 
     */
    public void clearPlayerCooldowns(Player player)
    {
        if (player != null)
        {
            _playerEffectCooldown.remove(player.getObjectId());
        }
    }
    
    /**
     * Retorna o tempo restante de cooldown para um efeito específico
     * @param target 
     * @param effectType 
     * @return tempo em segundos, ou 0 se não estiver em cooldown
     */
    public long getRemainingCooldown(Player target, EffectType effectType)
    {
        if (target == null)
            return 0;
        
        Map<String, Long> targetCooldowns = _playerEffectCooldown.get(target.getObjectId());
        if (targetCooldowns == null)
            return 0;
        
        Long lastApplyTime = targetCooldowns.get(effectType.name());
        if (lastApplyTime == null)
            return 0;
        
        long elapsed = System.currentTimeMillis() - lastApplyTime;
        long remaining = effectType.getDelay() - elapsed;
        
        return remaining > 0 ? remaining / 1000 : 0;
    }
}
