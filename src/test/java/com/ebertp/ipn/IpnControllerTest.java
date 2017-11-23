package com.ebertp.ipn;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;

public class IpnControllerTest {

	@Test
	public void testBuildResponseDataNullTest() {
		IpnController ipnc = new IpnController();
		try {
			ipnc.buildResponseData(null, null);
			fail("Expect Exception");
		} catch (Exception e) {
			// don't print stacktrace to polute log
		}
	}

	@Test
	public void testBuildResponseDataIpnTest() {
		IpnController ipnc = new IpnController();

		String[] names = {"payment_date","payment_status","address_status"};
		Enumeration<String> en = Collections.enumeration(new ArrayList<>(Arrays.asList(names)));
		
		Map m = new HashMap<String, String[]>();
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
