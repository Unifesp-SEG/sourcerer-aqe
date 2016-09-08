package br.unifesp.ppgcc.sourcereraqe.infrastructure;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import br.unifesp.ppgcc.sourcereraqe.domain.CodeVocabularyExpander;
import br.unifesp.ppgcc.sourcereraqe.domain.Expander;
import br.unifesp.ppgcc.sourcereraqe.domain.TypeExpander;
import br.unifesp.ppgcc.sourcereraqe.domain.WordNetExpander;

public class AQEApproach {

	private boolean tagCloud = false;

	private List<Expander> expanders = new ArrayList<Expander>();

	private boolean relaxReturn = false;
	private boolean relaxParams = false;
	private boolean contextRelevants = true;
	private boolean filterMethodNameTermsByParameter = true;
	private String relatedWordsServiceUrl;

	private List<QueryTerm> classNameTerms = new ArrayList<QueryTerm>();
	private List<QueryTerm> methodNameTerms = new ArrayList<QueryTerm>();
	private List<QueryTerm> returnTypeTerms = new ArrayList<QueryTerm>();
	private List<QueryTerm> paramsTerms = new ArrayList<QueryTerm>();
	
	public AQEApproach(String relatedWordsServiceUrl, String expanders, boolean relaxReturn, boolean relaxParams, boolean contextRelevants, boolean filterMethodNameTermsByParameter) throws Exception {
		this.relaxReturn = relaxReturn;
		this.relaxParams = relaxParams;
		this.contextRelevants = contextRelevants;
		this.filterMethodNameTermsByParameter = filterMethodNameTermsByParameter;
		this.relatedWordsServiceUrl = relatedWordsServiceUrl;
		
		String[] splitExpanders = StringUtils.split(expanders, ",");
		
		for(String expanderName : splitExpanders){
			Expander expander = this.getExpander(expanderName);

			if(expander == null)
				throw new Exception("Invalid expander name: " + expanderName);
			
			this.expanders.add(expander);
		}
	}
	
	public AQEApproach() throws Exception {
		this.tagCloud = true;
		this.relaxReturn = false;
		this.relaxParams = false;
		this.contextRelevants = true;
		this.filterMethodNameTermsByParameter = true;
	}
	
	public void loadMethodInterface(String methodName, String returnType, String params) throws Exception {
		this.loadMethodInterface(null, methodName, returnType, params);
	}

	public void loadMethodInterface(String className, String methodName, String returnType, String params) throws Exception {

		this.classNameTerms = this.getNameTerms(methodName);
		this.methodNameTerms = this.getNameTerms(methodName);
		this.returnTypeTerms = this.getReturnTypeTerms(returnType);
		this.paramsTerms = this.getParamsTerms(params);

		//Filter method names by parameter
		if(this.isFilterMethodNameTermsByParameter())
			methodNameTerms = this.getFilteredMethodNameTermsByParameter(methodNameTerms, paramsTerms);

		// EAQ
		for (Expander expander : this.getExpanders()) {
			if(className != null){
				if (expander.isClassNameExpander())
					for(QueryTerm queryTerm : classNameTerms)
						expander.expandTerm(queryTerm);
			}

			if (expander.isMethodNameExpander())
				for(QueryTerm queryTerm : methodNameTerms)
					expander.expandTerm(queryTerm);

			if (expander.isReturnExpander())
				for(QueryTerm queryTerm : returnTypeTerms)
					expander.expandTerm(queryTerm);

			if (expander.isParamExpander())
				for(QueryTerm queryTerm : paramsTerms)
					expander.expandTerm(queryTerm);
		}
	}

	List<QueryTerm> getNameTerms(String name){
		String names = JavaTermExtractor.getFQNTermsAsString(name);

		//Linha comentada em 28/02/2016
		//names = JavaTermExtractor.removeDuplicates(names);
		
		String[] strTerms = StringUtils.split(names, " ");
		
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		
		for(String term : strTerms){
			terms.add(new QueryTerm(term));
		}
		return terms;
	}
	
	private List<QueryTerm> getReturnTypeTerms(String returnType){
		
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		if(!StringUtils.isBlank(returnType))
			terms.add(new QueryTerm(StringUtils.trim(returnType)));
		
		return terms;
	}
	
	private List<QueryTerm> getParamsTerms(String params){
		String[] strTerms = StringUtils.split(StringUtils.trim(params),  ",");
		
		List<QueryTerm> terms = new ArrayList<QueryTerm>();
		
		for(String term : strTerms){
			terms.add(new QueryTerm(StringUtils.trim(term)));
		}
		
		return terms;
	}
	
	private List<QueryTerm> getFilteredMethodNameTermsByParameter(List<QueryTerm> methodNameTerms, List<QueryTerm> paramsTerms){
		if (methodNameTerms.size() <= 1 )
			return methodNameTerms;
		
		List<QueryTerm> filteredMethodNameTermsByParameter = new ArrayList<QueryTerm>();
		for (QueryTerm methodQueryTerm : methodNameTerms) {
			boolean useMethodNameTerm = true;
			for (QueryTerm paramTerm : paramsTerms) {
				if (methodQueryTerm.getExpandedTerms().get(0).equalsIgnoreCase(paramTerm.getExpandedTerms().get(0))){
					useMethodNameTerm = false;
					break;
				}
			}
			if(useMethodNameTerm)
				filteredMethodNameTermsByParameter.add(methodQueryTerm);
		}
		return filteredMethodNameTermsByParameter;
	}
	
	private Expander getExpander(String expander){
		if(expander != null & StringUtils.trim(expander).equalsIgnoreCase(Expander.WORDNET_EXPANDER))
			return new WordNetExpander(this.relatedWordsServiceUrl);
		if(expander != null & StringUtils.trim(expander).equalsIgnoreCase(Expander.CODE_VOCABULARY_EXPANDER))
			return new CodeVocabularyExpander(this.relatedWordsServiceUrl);
		if(expander != null & StringUtils.trim(expander).equalsIgnoreCase(Expander.TYPE_EXPANDER))
			return new TypeExpander();
		
		return null;
	}
	
	public String getAutoDescription() throws Exception {
		String desc = "";
		
		if(this.tagCloud)
			return "Tag Cloud";
		
		if(this.relaxReturn)
			desc += "relaxReturn | ";
		if(this.relaxParams)
			desc += "relaxParams | ";
		if(this.contextRelevants)
			desc += "contextRelevants | ";
		if(this.filterMethodNameTermsByParameter)
			desc += "filterMethodNameTermsByParameter | ";
		
		boolean first = true;
		for(Expander expander : expanders){
			if(first){
				desc += expander.getName();
				first = false;
			}else
				desc += ", " + expander.getName();
		}
		
		if("".equals(desc) || desc.endsWith(" | "))
			desc += "Without expansion";

		return desc;
	}

	public boolean hasMethodNameExpander(){
		for(Expander expander : expanders){
			if (expander.isMethodNameExpander())
				return true;
		}
		return false;
	}

	public boolean hasParamExpander(){
		for(Expander expander : expanders){
			if (expander.isParamExpander())
				return true;
		}
		return false;
	}

	public boolean hasReturnExpander(){
		for(Expander expander : expanders){
			if (expander.isReturnExpander())
				return true;
		}
		return false;
	}

	public boolean isTagCloud() {
		return tagCloud;
	}

	public List<Expander> getExpanders() {
		return expanders;
	}

	public boolean isRelaxParams() {
		return relaxParams;
	}

	public boolean isRelaxReturn() {
		return relaxReturn;
	}

	public boolean isContextRelevants() {
		return contextRelevants;
	}

	public boolean isFilterMethodNameTermsByParameter() {
		return filterMethodNameTermsByParameter;
	}

	public List<QueryTerm> getMethodNameTerms() {
		return methodNameTerms;
	}

	public List<QueryTerm> getReturnTypeTerms() {
		return returnTypeTerms;
	}

	public List<QueryTerm> getParamsTerms() {
		return paramsTerms;
	}

	public List<QueryTerm> getClassNameTerms() {
		return classNameTerms;
	}
}
