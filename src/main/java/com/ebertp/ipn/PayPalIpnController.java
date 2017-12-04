package com.ebertp.ipn;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



/**
 * Empfängt IPN Events von Zahlungsdienstleistern wie z.B. PayPal. Der Zahlungsanbieter sendet 4 Tage lang IPN Events bis eine 
 * Bestätigung an ihn zurückgesendet wird. IPN ist nicht synchron. Dieser IPN Endpunkt muss per HTTPS erreichbar sein.
 * @link https://developer.paypal.com/docs/classic/products/instant-payment-notification/
 * @link https://developer.paypal.com/docs/classic/ipn/integration-guide/IPNImplementation/
 *
 */
@RestController
public class PayPalIpnController {


	private static final String USER_AGENT = "ME IPN Responder";
	private static Logger LOG = LoggerFactory.getLogger(PayPalIpnController.class);


	@SuppressWarnings("unused")
	private static final String urlPaypalSandbox1 = "https://ipnpb.sandbox.paypal.com/cgi-bin/webscr";
	private static final String urlPaypalSandbox2 =  "https://www.sandbox.paypal.com/cgi-bin/webscr";
	@SuppressWarnings("unused")
	private static final String urlPaypalLive1 = "https://ipnpb.paypal.com/cgi-bin/webscr";
	private static final String urlPaypalLive2 = "https://www.paypal.com/cgi-bin/webscr";
	private static final boolean sandboxmode = true;
	


	// TODO register IPN callback URL at Paypal Request

	/**
	 * IPN Listener
	 * @param request
	 * @param response
	 */
	@RequestMapping(value="/ipn", method = RequestMethod.POST)
	public void handleIpn(HttpServletRequest request, HttpServletResponse response) {
		String reqUri = "";
		if (request != null) {
			reqUri = request.getRequestURI();
		}

		LOG.info("[ uri : {} ] - IPN Callback wird aufgerufen", reqUri);
		if(LOG.isDebugEnabled()) {
			logRequestHeaders(request);
		}
		// write an ipn flag to bestellung or do some other clever things
		LOG.debug("Invoice: "+request.getParameter("invoice"));

		try {
			String responseData = buildResponseData(request.getParameterNames(), request.getParameterMap());

			// TODO Identifizieren der Bestellung an Hand von Informationen aus dem IPN
			String paypalurl = urlPaypalSandbox2;
			if(!sandboxmode) {
				paypalurl = urlPaypalLive2;
			}
			sendIpnMessageToPaypal2(paypalurl, responseData);
			//sendIpnMessageToPaypal2("http://localhost:1902/xxx", buffer.toString());
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(500);
			LOG.error(e.getMessage());
		}
	}



	private void logRequestHeaders(HttpServletRequest request) {
		Enumeration<String> h = request.getHeaderNames();
		while (h.hasMoreElements()) {
			String s = (String) h.nextElement();
			LOG.debug("Header: "+s+" - "+request.getHeader(s));
		}
	}



	String buildResponseData(Enumeration<String> n, Map<String, String[]> map) throws UnsupportedEncodingException {
		StringBuffer buffer = new StringBuffer("cmd=_notify-validate");
		while (n.hasMoreElements()) {
			buffer.append("&");
			String s = (String) n.nextElement();
			buffer.append(s);
			buffer.append("=");
			buffer.append(URLEncoder.encode(map.get(s)[0], "UTF-8"));
		}
		return buffer.toString();
	}


	
	/**
	 * Return the unchanged IPN Message to Paypal only with notify validated.
	 * 
	 * @param ipnMessage IPN Message
	 * @throws Exception in case of an Error or Paypal send INVLAID back
	 */
	private void sendIpnMessageToPaypal(String url, String ipnReturnMessage) throws Exception {
		// TODO do this in a new thread
		LOG.debug("Test");
		LOG.info("Send IPN Message 'verified' to Paypal: "+url+" with IPN: "+ipnReturnMessage);

		HttpClient client = HttpClientBuilder.create().build();
		// TODO use right paypal url based in env
		HttpPost post = new HttpPost(url);

		// TODO ask for user agent
		post.setHeader("User-Agent", USER_AGENT);
		post.setHeader("content-type", "application/x-www-form-urlencoded");
		post.setHeader("host", "www.paypal.com");

		
		post.setEntity(new StringEntity(ipnReturnMessage) );
		
		HttpResponse response = client.execute(post);
		LOG.debug("Response Code : "  + response.getStatusLine().getStatusCode()+" "+response.getStatusLine().getReasonPhrase());
		Header[] h = response.getAllHeaders();
		for (int i = 0; i < h.length; i++) {
			LOG.debug("Header: "+h[i].getName()+ " "+h[i].getValue());
		}
		// validate response
		InputStream r = response.getEntity().getContent();
		String reponseMessage = IOUtils.toString( r, Charset.defaultCharset());
		if (reponseMessage.equalsIgnoreCase("VERIFIED")) {
			LOG.info("IPN Message verified by Paypal successfully");
		} else {
			throw new Exception("IPN Message not verified by Paypal: "+reponseMessage);
		}
	}


	private void sendIpnMessageToPaypal2(String url, String ipnReturnMessage) throws Exception {
		// TODO do this in a new thread
		// Don't write live customer data to logs
		if(sandboxmode) {
			LOG.debug("IPN: "+ipnReturnMessage);
		}
		LOG.info("Send IPN Message 'verified' to Paypal: "+url);
		
		URL u = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(ipnReturnMessage.getBytes());
		conn.connect();
		
		InputStream in = conn.getInputStream();
		String ins = IOUtils.toString(in, "UTF-8");
		String m = conn.getResponseMessage();
		LOG.debug("Response Code : "  + conn.getResponseCode()+" "+m+" - "+ins);
		if (ins.equalsIgnoreCase("VERIFIED")) {
			LOG.info("IPN Message verified by Paypal successfully");
		} else {
			throw new Exception("IPN Message not verified by Paypal: "+ins);
		}
	}


}
