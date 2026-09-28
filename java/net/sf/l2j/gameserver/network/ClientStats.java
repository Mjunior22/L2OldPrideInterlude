package net.sf.l2j.gameserver.network;

import java.util.logging.Logger;

import net.sf.l2j.Config;

public class ClientStats
{
	public int processedPackets = 0;
	public int droppedPackets = 0;
	public int unknownPackets = 0;
	public int totalQueueSize = 0;
	public int maxQueueSize = 0;
	public int totalBursts = 0;
	public int maxBurstSize = 0;
	public int shortFloods = 0;
	public int longFloods = 0;
	public int totalQueueOverflows = 0;
	public int totalUnderflowExceptions = 0;

	private final int[] _packetsInSecond;
	private long _packetCountStartTick = 0;
	private int _head;
	private int _totalCount = 0;

	private int _floodsInMin = 0;
	private long _floodStartTick = 0;
	private int _unknownPacketsInMin = 0;
	private long _unknownPacketStartTick = 0;
	private int _overflowsInMin = 0;
	private long _overflowStartTick = 0;
	private int _underflowReadsInMin = 0;
	private long _underflowReadStartTick = 0;

	volatile boolean _floodDetected = false;
	volatile boolean _queueOverflowDetected = false;

	private final int BUFFER_SIZE;

	public ClientStats()
	{
		BUFFER_SIZE = Config.CLIENT_PACKET_QUEUE_MEASURE_INTERVAL;
		_packetsInSecond = new int[BUFFER_SIZE];
		_head = BUFFER_SIZE - 1;
	}

	/**
	 * @return true if incoming packet need to be dropped.
	 */
	protected final boolean dropPacket()
	{
		// Mantém proteção mas só ativa em casos extremos
		final boolean result = _floodDetected && (_packetsInSecond[_head] > 150); // Só dropa se > 150 pacotes/segundo
		if (result)
		{
			droppedPackets++;
			// Log para monitoramento
			Logger.getLogger("Packet drop for " + _packetsInSecond[_head] + " packets/sec");
		}
		return result;
	}

	/**
	 * @param queueSize
	 * @return true if flood detected first and ActionFailed packet need to be sent. Later during flood returns true (and send ActionFailed) once per second.
	 */
	protected final boolean countPacket(int queueSize)
	{
		processedPackets++;
		totalQueueSize += queueSize;
		if (maxQueueSize < queueSize)
			maxQueueSize = queueSize;
		
		// Reset flood detection se a queue estiver normal
		if (_queueOverflowDetected && queueSize < 5)
			_queueOverflowDetected = false;

		return countPacket();
	}

	/**
	 * Counts unknown packets.
	 * @return true if threshold is reached.
	 */
	protected final boolean countUnknownPacket()
	{
		unknownPackets++;

		final long tick = System.currentTimeMillis();
		if (tick - _unknownPacketStartTick > 60000)
		{
			_unknownPacketStartTick = tick;
			_unknownPacketsInMin = 1;
			return false;
		}

		_unknownPacketsInMin++;
		// Limite bem generoso: 50 pacotes desconhecidos por minuto
		return _unknownPacketsInMin > 50;
	}

	/**
	 * Counts burst length.
	 * @param count - current number of processed packets in burst
	 * @return true if execution of the queue need to be aborted.
	 */
	protected final boolean countBurst(int count)
	{
		if (count > maxBurstSize)
			maxBurstSize = count;

		// Limite generoso: 300 pacotes em burst
		if (count < 300)
			return false;

		totalBursts++;
		
		// Log para debug
		Logger.getLogger("Large burst detected: " + count + " packets");
		return true;
	}

	/**
	 * Counts queue overflows.
	 * @return true if threshold is reached.
	 */
	protected final boolean countQueueOverflow()
	{
		_queueOverflowDetected = true;
		totalQueueOverflows++;

		final long tick = System.currentTimeMillis();
		if (tick - _overflowStartTick > 60000)
		{
			_overflowStartTick = tick;
			_overflowsInMin = 1;
			return false;
		}

		_overflowsInMin++;
		// Limite generoso: 20 overflows por minuto
		return _overflowsInMin > 20;
	}

	/**
	 * Counts underflow exceptions.
	 * @return true if threshold is reached.
	 */
	protected final boolean countUnderflowException()
	{
		totalUnderflowExceptions++;

		final long tick = System.currentTimeMillis();
		if (tick - _underflowReadStartTick > 60000)
		{
			_underflowReadStartTick = tick;
			_underflowReadsInMin = 1;
			return false;
		}

		_underflowReadsInMin++;
		// Limite generoso: 20 underflows por minuto
		return _underflowReadsInMin > 20;
	}

	/**
	 * @return true if maximum number of floods per minute is reached.
	 */
	protected final boolean countFloods()
	{
		// Limite bem generoso: 30 floods por minuto
		return _floodsInMin > 30;
	}

	private final boolean longFloodDetected()
	{
		// Média de 60 pacotes/segundo é considerada flood longo
		return (_totalCount / BUFFER_SIZE) > 60;
	}

	/**
	 * @return true if flood detected first and ActionFailed packet need to be sent. Later during flood returns true (and send ActionFailed) once per second.
	 */
	private final synchronized boolean countPacket()
	{
		_totalCount++;
		final long tick = System.currentTimeMillis();
		if (tick - _packetCountStartTick > 1000)
		{
			_packetCountStartTick = tick;

			// Clear flood detection se estiver dentro de limites normais
			if (_floodDetected && !longFloodDetected() && _packetsInSecond[_head] < 40) // 40 pacotes/segundo
				_floodDetected = false;

			// wrap head of the buffer around the tail
			if (_head <= 0)
				_head = BUFFER_SIZE;
			_head--;

			_totalCount -= _packetsInSecond[_head];
			_packetsInSecond[_head] = 1;
			
			// Só retorna true (flood) se estiver MUITO alto
			return _floodDetected && _packetsInSecond[_head] > 100;
		}

		final int count = ++_packetsInSecond[_head];
		if (!_floodDetected)
		{
			// Só detecta flood acima de 80 pacotes/segundo (bem generoso)
			if (count > 80)
				shortFloods++;
			else if (longFloodDetected())
				longFloods++;
			else
				return false;

			_floodDetected = true;
			if (tick - _floodStartTick > 60000)
			{
				_floodStartTick = tick;
				_floodsInMin = 1;
			}
			else
				_floodsInMin++;

			return true; // Return true only in the beginning of the flood
		}

		return false;
	}
}