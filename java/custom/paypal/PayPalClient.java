package custom.paypal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Cliente minimo da API REST do PayPal (Orders v2 + Payments v2).
 * Usa apenas java.net.http (Java 11+) e Gson. Nunca loga o client secret nem o token.
 */
public class PayPalClient
{
	/** Erro da API do PayPal (status HTTP 0 = falha de rede/JSON). */
	public static class PayPalException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		private final int _status;
		private final String _body;
		
		public PayPalException(int status, String body)
		{
			super("HTTP " + status + ": " + (body == null ? "" : body.length() > 400 ? body.substring(0, 400) : body));
			_status = status;
			_body = body == null ? "" : body;
		}
		
		public int getStatus()
		{
			return _status;
		}
		
		public boolean isAlreadyCaptured()
		{
			return _body.contains("ORDER_ALREADY_CAPTURED");
		}
	}
	
	private static final String LIVE = "https://api-m.paypal.com";
	private static final String SANDBOX = "https://api-m.sandbox.paypal.com";
	
	private final String _base;
	private final String _clientId;
	private final String _secret;
	private final HttpClient _http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	
	private String _token;
	private long _tokenExpires;
	
	public PayPalClient(boolean sandbox, String clientId, String secret)
	{
		this(sandbox ? SANDBOX : LIVE, clientId, secret);
	}
	
	/** Construtor com URL base customizada (usado em testes com um servidor simulado). 
	 * @param baseUrl 
	 * @param clientId 
	 * @param secret */
	PayPalClient(String baseUrl, String clientId, String secret)
	{
		_base = baseUrl;
		_clientId = clientId;
		_secret = secret;
	}
	
	/**
	 * Cria uma order (intent CAPTURE). O valor e a moeda vem sempre do servidor.
	 * @param purchaseId 
	 * @param currency 
	 * @param value 
	 * @param description 
	 * @param brand 
	 * @param returnUrl 
	 * @param cancelUrl 
	 * @return 
	 * @throws PayPalException 
	 */
	public JsonObject createOrder(int purchaseId, String currency, String value, String description, String brand, String returnUrl, String cancelUrl) throws PayPalException
	{
		final JsonObject amount = new JsonObject();
		amount.addProperty("currency_code", currency);
		amount.addProperty("value", value);
		
		final JsonObject unit = new JsonObject();
		unit.addProperty("reference_id", "P" + purchaseId);
		unit.addProperty("custom_id", String.valueOf(purchaseId));
		unit.addProperty("description", cut(description, 120));
		unit.add("amount", amount);
		
		final JsonArray units = new JsonArray();
		units.add(unit);
		
		final JsonObject ctx = new JsonObject();
		ctx.addProperty("brand_name", cut(brand, 100));
		ctx.addProperty("user_action", "PAY_NOW");
		ctx.addProperty("shipping_preference", "NO_SHIPPING");
		ctx.addProperty("return_url", returnUrl);
		ctx.addProperty("cancel_url", cancelUrl);
		
		final JsonObject body = new JsonObject();
		body.addProperty("intent", "CAPTURE");
		body.add("purchase_units", units);
		body.add("application_context", ctx);
		
		return call("POST", "/v2/checkout/orders", body.toString(), "L2ORD-" + purchaseId);
	}
	
	public JsonObject getOrder(String orderId) throws PayPalException
	{
		return call("GET", "/v2/checkout/orders/" + safeId(orderId), null, null);
	}
	
	/**
	 * Captura uma order aprovada. O requestId torna a chamada idempotente.
	 * @param orderId 
	 * @param requestId 
	 * @return 
	 * @throws PayPalException 
	 */
	public JsonObject captureOrder(String orderId, String requestId) throws PayPalException
	{
		return call("POST", "/v2/checkout/orders/" + safeId(orderId) + "/capture", "{}", requestId);
	}
	
	public JsonObject getCapture(String captureId) throws PayPalException
	{
		return call("GET", "/v2/payments/captures/" + safeId(captureId), null, null);
	}
	
	// ------------------------------------------------------------------------------------
	
	private synchronized String token(boolean force) throws PayPalException
	{
		if (!force && _token != null && System.currentTimeMillis() < _tokenExpires)
			return _token;
		
		final String basic = Base64.getEncoder().encodeToString((_clientId + ":" + _secret).getBytes(StandardCharsets.UTF_8));
		final HttpRequest req = HttpRequest.newBuilder(URI.create(_base + "/v1/oauth2/token")).timeout(Duration.ofSeconds(20)).header("Authorization", "Basic " + basic).header("Content-Type", "application/x-www-form-urlencoded").header("Accept", "application/json").POST(BodyPublishers.ofString("grant_type=client_credentials")).build();
		
		final HttpResponse<String> res = send(req);
		if (res.statusCode() >= 400)
			throw new PayPalException(res.statusCode(), "falha ao obter token");
		
		final JsonObject json = parse(res.body());
		final JsonElement token = json.get("access_token");
		if (token == null || token.isJsonNull())
			throw new PayPalException(0, "resposta de token sem access_token");
		
		final JsonElement ttl = json.get("expires_in");
		final long seconds = (ttl == null || ttl.isJsonNull()) ? 300 : ttl.getAsLong();
		
		_token = token.getAsString();
		_tokenExpires = System.currentTimeMillis() + Math.max(30, seconds - 60) * 1000L;
		return _token;
	}
	
	private JsonObject call(String method, String path, String json, String requestId) throws PayPalException
	{
		for (int attempt = 0; attempt < 2; attempt++)
		{
			final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(_base + path)).timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + token(attempt > 0)).header("Accept", "application/json");
			if (requestId != null)
				b.header("PayPal-Request-Id", requestId);
			
			if (json != null)
				b.header("Content-Type", "application/json").method(method, BodyPublishers.ofString(json));
			else
				b.method(method, BodyPublishers.noBody());
			
			final HttpResponse<String> res = send(b.build());
			
			// token expirado/revogado: renova uma vez e tenta de novo
			if (res.statusCode() == 401 && attempt == 0)
				continue;
			
			if (res.statusCode() >= 400)
				throw new PayPalException(res.statusCode(), res.body());
			
			return parse(res.body());
		}
		throw new PayPalException(401, "nao autorizado");
	}
	
	private HttpResponse<String> send(HttpRequest req) throws PayPalException
	{
		try
		{
			return _http.send(req, BodyHandlers.ofString());
		}
		catch (IOException e)
		{
			throw new PayPalException(0, "erro de rede: " + e.getMessage());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new PayPalException(0, "interrompido");
		}
	}
	
	private static JsonObject parse(String body) throws PayPalException
	{
		if (body == null || body.isBlank())
			return new JsonObject();
		
		try
		{
			return JsonParser.parseString(body).getAsJsonObject();
		}
		catch (JsonParseException | IllegalStateException e)
		{
			throw new PayPalException(0, "resposta invalida do PayPal");
		}
	}
	
	private static String safeId(String id) throws PayPalException
	{
		if (id == null || !id.matches("[A-Za-z0-9_-]{5,64}"))
			throw new PayPalException(0, "id invalido");
		return id;
	}
	
	private static String cut(String s, int max)
	{
		return s.length() <= max ? s : s.substring(0, max);
	}
}
