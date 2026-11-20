package custom.mpcard;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Cliente minimo da API REST do Mercado Pago para o Checkout Pro (preferencias) e a busca de pagamentos.
 * Usa o mesmo Access Token do Pix. Nunca loga o token.
 */
public class MpCardClient
{
	public static class MpException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		private final int _status;
		
		public MpException(int status, String body)
		{
			super("HTTP " + status + ": " + (body == null ? "" : body.length() > 400 ? body.substring(0, 400) : body));
			_status = status;
		}
		
		public int getStatus()
		{
			return _status;
		}
	}
	
	private static final String BASE = "https://api.mercadopago.com";
	
	private final String _base;
	private final String _token;
	private final HttpClient _http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	
	public MpCardClient(String token)
	{
		this(BASE, token);
	}
	
	/** Construtor com URL base customizada (usado em testes com um servidor simulado). 
	 * @param baseUrl 
	 * @param token */
	MpCardClient(String baseUrl, String token)
	{
		_base = baseUrl;
		_token = token;
	}
	
	/**
	 * Cria uma preferencia do Checkout Pro. O valor e o titulo vem sempre do servidor.
	 * Boleto, caixa eletronico e transferencia (Pix) ficam de fora: o Pix tem o proprio sistema.
	 * @param purchaseId 
	 * @param reference 
	 * @param title 
	 * @param amount 
	 * @param brand 
	 * @param backUrl 
	 * @param expiresAtMillis 
	 * @return 
	 * @throws MpException 
	 */
	public JsonObject createPreference(int purchaseId, String reference, String title, BigDecimal amount, String brand, String backUrl, long expiresAtMillis) throws MpException
	{
		final JsonObject item = new JsonObject();
		item.addProperty("id", reference);
		item.addProperty("title", cut(title, 120));
		item.addProperty("quantity", 1);
		item.addProperty("currency_id", "BRL");
		item.addProperty("unit_price", amount);
		final JsonArray items = new JsonArray();
		items.add(item);
		
		final JsonArray excluded = new JsonArray();
		for (String type : new String[] { "ticket", "atm", "bank_transfer" })
		{
			final JsonObject t = new JsonObject();
			t.addProperty("id", type);
			excluded.add(t);
		}
		final JsonObject methods = new JsonObject();
		methods.add("excluded_payment_types", excluded);
		methods.addProperty("installments", 1);
		
		final JsonObject back = new JsonObject();
		back.addProperty("success", backUrl);
		back.addProperty("failure", backUrl);
		back.addProperty("pending", backUrl);
		
		final JsonObject body = new JsonObject();
		body.add("items", items);
		body.addProperty("external_reference", reference);
		body.add("payment_methods", methods);
		body.add("back_urls", back);
		body.addProperty("statement_descriptor", cut(brand, 22));
		body.addProperty("expires", true);
		body.addProperty("expiration_date_to", OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMillis), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx")));
		
		return call("POST", "/checkout/preferences", body.toString(), "L2CARD-PREF-" + purchaseId);
	}
	
	/**
	 * Busca os pagamentos feitos para uma referencia externa (mais recentes primeiro).
	 * @param reference 
	 * @return 
	 * @throws MpException 
	 */
	public JsonObject searchPayments(String reference) throws MpException
	{
		final String q = URLEncoder.encode(reference, StandardCharsets.UTF_8);
		return call("GET", "/v1/payments/search?external_reference=" + q + "&sort=date_created&criteria=desc&limit=20", null, null);
	}
	
	private JsonObject call(String method, String path, String json, String idempotencyKey) throws MpException
	{
		final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(_base + path)).timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + _token).header("Accept", "application/json");
		if (idempotencyKey != null)
			b.header("X-Idempotency-Key", idempotencyKey);
		
		if (json != null)
			b.header("Content-Type", "application/json").method(method, BodyPublishers.ofString(json));
		else
			b.method(method, BodyPublishers.noBody());
		
		final HttpResponse<String> res;
		try
		{
			res = _http.send(b.build(), BodyHandlers.ofString());
		}
		catch (IOException e)
		{
			throw new MpException(0, "network error: " + e.getMessage());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new MpException(0, "interrupted");
		}
		
		if (res.statusCode() >= 400)
			throw new MpException(res.statusCode(), res.body());
		
		try
		{
			return JsonParser.parseString(res.body()).getAsJsonObject();
		}
		catch (JsonParseException | IllegalStateException e)
		{
			throw new MpException(0, "Invalid response from Mercado Pago");
		}
	}
	
	private static String cut(String s, int max)
	{
		return s.length() <= max ? s : s.substring(0, max);
	}
}
