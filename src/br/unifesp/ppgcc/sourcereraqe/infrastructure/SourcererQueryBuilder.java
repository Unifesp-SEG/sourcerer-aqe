package br.unifesp.ppgcc.sourcereraqe.infrastructure;

import java.util.List;

public class SourcererQueryBuilder {

	private AQEApproach aqeApproach;

	public SourcererQueryBuilder(String relatedWordsServiceUrl, String expanders, boolean relaxReturn, boolean relaxParams, boolean contextRelevants, boolean filterMethodNameTermsByParameter) throws Exception {
		aqeApproach = new AQEApproach(relatedWordsServiceUrl, expanders, relaxReturn, relaxParams, contextRelevants, filterMethodNameTermsByParameter);
	}

	public SourcererQueryBuilder(AQEApproach aqeApproach) throws Exception {
		this.aqeApproach = aqeApproach;
	}

	public SourcererQueryBuilder() throws Exception {
		aqeApproach = new AQEApproach();
	}

	public String getSourcererExpandedQuery(String methodName, String returnType, String params) throws Exception {

		aqeApproach.loadMethodInterface(methodName, returnType, params);

		this.prioritizeOrininalTerms(aqeApproach.getMethodNameTerms());
		String methodPart = this.getMethodNamePart(aqeApproach.getMethodNameTerms());
		String returnTypePart = (aqeApproach.isRelaxReturn() ? "" : this.getReturnTypePart(aqeApproach.getReturnTypeTerms()));
		String paramsPart = (aqeApproach.isRelaxParams() ? "" : this.getParamsPart(aqeApproach.getParamsTerms()));

		return methodPart + returnTypePart + paramsPart;
	}

	private void prioritizeOrininalTerms(List<QueryTerm> queryTerms){
		for(QueryTerm queryTerm : queryTerms){
			String originalTerm = queryTerm.getExpandedTerms().get(0);
			queryTerm.getExpandedTerms().set(0, originalTerm + "^100");
		}
	}
	
	public String getMethodNamePart(List<QueryTerm> methodTerms) throws Exception {

		if(methodTerms.size() == 0)
			return "";

		String query = "";
		query += "sname_contents:(" + this.getSourcererQueryPart(methodTerms) + ")";
		
		return query;
	}

	public String getReturnTypePart(List<QueryTerm> returnTypeTerms) {

		if(returnTypeTerms.size() == 0)
			return "";

		String query = "";
		query += "\nreturn_sname_contents:(" + this.getSourcererQueryPart(returnTypeTerms) + ")";
		
		return query;
	}

	public String getParamsPart(List<QueryTerm> paramsTerms) {

		boolean sourcererLibBug = paramsTerms.size() == 1 && ")".equals(paramsTerms.get(0)); // entityId in ( 5842071 , 5877324 )
		if (paramsTerms.size() == 0 || sourcererLibBug)
			return "";

		String query = "";
		query += "\nparam_count:" + paramsTerms.size();
		query += "\nparams_snames_contents:(" + this.getSourcererQueryPart(paramsTerms) + ")";

		return query;
	}
	
	private String getSourcererQueryPart(List<QueryTerm> queryTerms) {
		String query = "";
		boolean firstTerm = true;
		for(QueryTerm queryTerm : queryTerms){

			if(!firstTerm)
				query += " AND ";
			else
				firstTerm = false;
			
			boolean firstExpanded = true;
			for(String expandedTerm : queryTerm.getExpandedTerms()){
				if(firstExpanded){
					query += "( " + expandedTerm;
					firstExpanded = false;
				} else
					query += " OR " + expandedTerm;
			}
			if(queryTerm.getExpandedTerms().size() > 0)
				query += " )";
			

			firstExpanded = true;
			for(String expandedTerm : queryTerm.getExpandedTermsNot()){
				if(firstExpanded){
					query += " AND !( " + expandedTerm;
					firstExpanded = false;
				} else
					query += " OR " + expandedTerm;
			}
			if(queryTerm.getExpandedTermsNot().size() > 0)
				query += " )";
		}

		return query;
	}

}
