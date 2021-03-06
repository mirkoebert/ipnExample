package com.ebertp.ipn;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PayPalIpnControllerTest {

	@Test
	public void testBuildResponseDataNullTest() {
		PayPalIpnController ipnc = new PayPalIpnController();
		try {
			ipnc.buildResponseData(null, null);
			fail("Expect Exception");
		} catch (Exception e) {
			// don't print stacktrace to polute log
		}
	}

	@Test
	public void testBuildResponseDataIpnTest() {
		PayPalIpnController ipnc = new PayPalIpnController();

		String[] names = {"payment_date","payment_status","address_status"};
		Enumeration<String> en = Collections.enumeration(new ArrayList<>(Arrays.asList(names)));
		
		Map<String, String[]> m = new HashMap<String, String[]>();
		m.put("payment_date", new String[]{"Thu%20Nov%2023%202017%2008%3A42%3A00%20GMT%2B0100%20%28CET%29"});
		m.put("payment_status", new String[]{"Completed"});
		m.put("address_status", new String[]{"confirmed"});
		
		try {
			String d = ipnc.buildResponseData(en, m);
			assertNotNull(d);
			assertEquals(d.indexOf(" "), -1);
			assertTrue(d.indexOf("=")>1);
			assertTrue(d.startsWith("cmd=_notify-validate"));
		} catch (Exception e) {
			fail("Exception "+e.getMessage());
		}
	}
}
