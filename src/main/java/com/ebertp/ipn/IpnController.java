package com.ebertp.ipn;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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


	private static final String urlPaypalSandbox = "https://ipnpb.sandbox.paypal.com/cgi-bin/webscr";
	@SuppressWarnings("unused")
	private static final String urlPaypalLive = "https://ipnpb.paypal.com/cgi-bin/webscr";


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

		BufferedReader r;
		try {
			r = request.getReader();
			StringBuffer buffer = new StringBuffer();
			Enumeration<String> n = request.getParameterNames();
			int i = 0;
			while (n.hasMoreElements()) {
				if (i >0) {
				buffer.append("&");
				}
				i++;
				String s = (String) n.nextElement();
				buffer.append(s);
				buffer.append("=");
				buffer.append(request.getParameter(s));
			}
			LOG.info("XXX1: "+buffer);
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
	private void sendIpnMessageToPaypal(String ipnMessage) throws Exception {
		String url = urlPaypalSandbox;
		// TODO do this in a new thread
		LOG.debug("Test");
		String ipnReturnMessage = "cmd=_notify-validate"+ipnMessage;
		LOG.info("Send IPN Message 'verified' to Paypal: "+url+" with IPN: "+ipnReturnMessage);

		HttpClient client = HttpClientBuilder.create().build();
		// TODO use right paypal url based in env
		HttpPost post = new HttpPost(url);

		// TODO ask for user agent
		post.setHeader("User-Agent", USER_AGENT);

		post.setEntity(new StringEntity(ipnReturnMessage) );
		HttpResponse response = client.execute(post);
		LOG.debug("Response Code : "  + response.getStatusLine().getStatusCode());
		// validate response
		InputStream r = response.getEntity().getContent();
		String reponseMessage = IOUtils.toString( r, Charset.defaultCharset());
		if (reponseMessage.equalsIgnoreCase("VERIFIED")) {
			LOG.info("IPN Message verified by Paypal successfully");
		} else {
			throw new Exception("IPN Message not verified by Paypal: "+reponseMessage);
		}
	}


}