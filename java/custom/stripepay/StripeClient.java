package custom.stripepay;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Cliente minimo da API REST da Stripe (Payment Links + Checkout Sessions + PaymentIntents).
 * As requisicoes sao form-encoded, como a Stripe exige. Nunca loga a chave secreta.
 */
public class StripeClient
{
	public static class StripeException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		private final int _status;
		
		public StripeException(int status, String body)
		{
			super("HTTP " + status + ": " + (body == null ? "" : body.length() > 400 ? body.substring(0, 400) : body));
			_status = status;
		}
		
		public int getStatus()
		{
			return _status;
		}
	}
	
	private static final String BASE = "https://api.stripe.com";
	
	private final String _base;
	private final String _key;
	private final HttpClient _http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	
	public StripeClient(String secretKey)
	{
		this(BASE, secretKey);
	}
	
	/** Construtor com URL base customizada (usado em testes com um servidor simulado). */
	StripeClient(String baseUrl, String secretKey)
	{
		_base = baseUrl;
		_key = secretKey;
	}
	
	/**
	 * Cria um Payment Link de uso unico (1 venda), com preco inline. O valor e o nome vem sempre do servidor.
	 * O metadata e copiado pela Stripe para a sessao de pagamento e para o PaymentIntent.
	 */
	public JsonObject createPaymentLink(int purchaseId, String reference, String title, long amountCents, String returnUrl) throws StripeException
	{
		final Map<String, String> f = new LinkedHashMap<>();
		f.put("line_items[0][price_data][currency]", "brl");
		f.put("line_items[0][price_data][unit_amount]", Long.toString(amountCents));
		f.put("line_items[0][price_data][product_data][name]", cut(title, 120));
		f.put("line_items[0][quantity]", "1");
		f.put("payment_method_types[0]", "card");
		f.put("restrictions[completed_sessions][limit]", "1");
		f.put("metadata[l2_order]", reference);
		f.put("payment_intent_data[metadata][l2_order]", reference);
		f.put("inactive_message", "This donation link is no longer available. Please create a new donation in the game.");
		if (returnUrl != null && !returnUrl.isEmpty())
		{
			f.put("after_completion[type]", "redirect");
			f.put("after_completion[redirect][url]", returnUrl);
		}
		
		return call("POST", "/v1/payment_links", f, "L2STRIPE-LINK-" + purchaseId);
	}
	
	/** Desativa o link: ninguem mais consegue abrir e pagar. */
	public JsonObject deactivatePaymentLink(String linkId) throws StripeException
	{
		final Map<String, String> f = new LinkedHashMap<>();
		f.put("active", "false");
		return call("POST", "/v1/payment_links/" + safeId(linkId), f, null);
	}
	
	/** Sessoes de pagamento criadas por um Payment Link (cada vez que alguem abre o link e paga). */
	public JsonObject listSessions(String linkId) throws StripeException
	{
		return call("GET", "/v1/checkout/sessions?payment_link=" + safeId(linkId) + "&limit=20", null, null);
	}
	
	/** PaymentIntent com a cobranca (charge) expandida: serve para detectar estorno e contestacao. */
	public JsonObject getPaymentIntent(String paymentIntentId) throws StripeException
	{
		return call("GET", "/v1/payment_intents/" + safeId(paymentIntentId) + "?expand[]=latest_charge", null, null);
	}
	
	private JsonObject call(String method, String path, Map<String, String> form, String idempotencyKey) throws StripeException
	{
		final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(_base + path)).timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + _key).header("Accept", "application/json");
		if (idempotencyKey != null)
			b.header("Idempotency-Key", idempotencyKey);
		
		if (form != null)
			b.header("Content-Type", "application/x-www-form-urlencoded").method(method, BodyPublishers.ofString(encode(form)));
		else
			b.method(method, BodyPublishers.noBody());
		
		final HttpResponse<String> res;
		try
		{
			res = _http.send(b.build(), BodyHandlers.ofString());
		}
		catch (IOException e)
		{
			throw new StripeException(0, "erro de rede: " + e.getMessage());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new StripeException(0, "interrompido");
		}
		
		if (res.statusCode() >= 400)
			throw new StripeException(res.statusCode(), res.body());
		
		try
		{
			return JsonParser.parseString(res.body()).getAsJsonObject();
		}
		catch (JsonParseException | IllegalStateException e)
		{
			throw new StripeException(0, "resposta invalida da Stripe");
		}
	}
	
	private static String encode(Map<String, String> form)
	{
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : form.entrySet())
		{
			if (sb.length() > 0)
				sb.append('&');
			sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}
	
	private static String safeId(String id) throws StripeException
	{
		if (id == null || !id.matches("[A-Za-z0-9_]{5,80}"))
			throw new StripeException(0, "id invalido");
		return id;
	}
	
	private static String cut(String s, int max)
	{
		return s.length() <= max ? s : s.substring(0, max);
	}
}
