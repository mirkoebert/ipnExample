package com.ebertp.ipn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.servlet.ServletInputStream;
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
 * Bestätigung an ihn zurückgesendet wird. IPN ist nicht synchron. Unser IPN endpunkt muss per HTTPS erreichbar sein.
 * @link https://developer.paypal.com/docs/classic/products/instant-payment-notification/
 * @link https://developer.paypal.com/docs/classic/ipn/integration-guide/IPNImplementation/
 *
 */
@RestController
public class IpnController {


	private static final String USER_AGENT = "ME IPN Responder";
	private static Logger LOG = LoggerFactory.getLogger(IpnController.class);


	@SuppressWarnings("unused")
	private static final String urlPaypalSandbox1 = "https://ipnpb.sandbox.paypal.com/cgi-bin/webscr";
	@SuppressWarnings("unused")
	private static final String urlPaypalSandbox2 =  "https://www.sandbox.paypal.com/cgi-bin/webscr";
	@SuppressWarnings("unused")
	private static final String urlPaypalLive1 = "https://ipnpb.paypal.com/cgi-bin/webscr";
	@SuppressWarnings("unused")
	private static final String urlPaypalLive2 = "https://www.paypal.com/cgi-bin/webscr";
	
	private String url = urlPaypalSandbox2;


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
		// write an ipn flag to bestellung or do some other clever things
		Enumeration<String> h = request.getHeaderNames();
		while (h.hasMoreElements()) {
			String s = (String) h.nextElement();
			LOG.debug("Header: "+s+" - "+request.getHeader(s));
		}

		try {
			StringBuffer buffer = new StringBuffer();
			Enumeration<String> n = request.getParameterNames();
			while (n.hasMoreElements()) {
				
				String s = (String) n.nextElement();
				buffer.append(s);
				buffer.append("=");
				buffer.append(request.getParameter(s));
				buffer.append("&");
			}
			buffer.append("cmd=_notify-validate");
			LOG.info("XXX2: "+request.getContentLength());

			// TODO Identifizieren der Bestellung an Hand von Informationen aus dem IPN
			sendIpnMessageToPaypal2(buffer.toString());
			// write empty 200 response
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(500);
			LOG.error(e.getMessage());
		}
	}

	@RequestMapping(value="/ipn2", method = RequestMethod.POST)
	public void handleIpn2(HttpServletRequest request, HttpServletResponse response) {
		String reqUri = "";
		if (request != null) {
			reqUri = request.getRequestURI();
		}

		LOG.info("[ uri : {} ] - IPN Callback wird aufgerufen", reqUri);
		// write an ipn flag to bestellung or do some other clever things
		Enumeration<String> h = request.getHeaderNames();
		while (h.hasMoreElements()) {
			String s = (String) h.nextElement();
			LOG.debug("Header: "+s+" - "+request.getHeader(s));
		}

		try {
			StringBuffer buffer = new StringBuffer();
			Enumeration<String> n = request.getParameterNames();
			request.getP
			while (n.hasMoreElements()) {
				
				String s = (String) n.nextElement();
				buffer.append(s);
				buffer.append("=");
				buffer.append(request.getParameter(s));
				buffer.append("&");
			}
			buffer.append("cmd=_notify-validate");
			LOG.info("XXX2: "+request.getContentLength());

			// TODO Identifizieren der Bestellung an Hand von Informationen aus dem IPN
			sendIpnMessageToPaypal(buffer.toString());
			// write empty 200 response
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(500);
			LOG.error(e.getMessage());
		}
	}

	
	/**
	 * Return the unchanged IPN Message to Paypal only with notify validated.
	 * 
	 * @param ipnMessage IPN Message
	 * @throws Exception in case of an Error or Paypal send INVLAID back
	 */
	private void sendIpnMessageToPaypal(String ipnReturnMessage) throws Exception {
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


	private void sendIpnMessageToPaypal2(String ipnReturnMessage) throws Exception {
		// TODO do this in a new thread
		LOG.debug("Test 2");
		LOG.info("Send IPN Message 'verified' to Paypal: "+url+" with IPN: "+ipnReturnMessage);
		
		URL u = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(ipnReturnMessage.getBytes());
		conn.connect();
		

		
		InputStream in = conn.getInputStream();
		String ins = IOUtils.toString(in);
		String m = conn.getResponseMessage();
		LOG.debug("Response Code : "  + conn.getResponseCode()+" "+m+" - "+ins);
		if (ins.equalsIgnoreCase("VERIFIED")) {
			LOG.info("IPN Message verified by Paypal successfully");
		} else {
			throw new Exception("IPN Message not verified by Paypal: "+m);
		}
	}

}
