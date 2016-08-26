package br.unifesp.ppgcc.sourcereraqe.infrastructure;


import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;


public class AQEApproachTest {

	@Test
	public void camelCase() throws Exception {
		String s = "getLongValue";
		List<QueryTerm> list = new AQEApproach().getMethodNameTerms(s);
		assertEquals("get",list.get(0).getExpandedTerms().get(0));
		assertEquals("long",list.get(1).getExpandedTerms().get(0));
		assertEquals("value",list.get(2).getExpandedTerms().get(0));
	}
	
	@Test
	public void camelCase2() throws Exception {
		String s = "acquireYearnPrize";
		List<QueryTerm> list = new AQEApproach().getMethodNameTerms(s);
		assertEquals("acquire",list.get(0).getExpandedTerms().get(0));
		assertEquals("yearn",list.get(1).getExpandedTerms().get(0));
		assertEquals("prize",list.get(2).getExpandedTerms().get(0));
	}
	
	@Test
	public void getMethodNameTerms() throws Exception {
		String s = "findByNamedQueryAndNamedParam";
		List<QueryTerm> list = new AQEApproach().getMethodNameTerms(s);
		assertEquals(7,list.size());
	}
	
}
