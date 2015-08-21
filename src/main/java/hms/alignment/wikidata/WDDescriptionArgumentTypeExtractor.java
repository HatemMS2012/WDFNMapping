package hms.alignment.wikidata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hms.SentenceTriple;
import hms.SentenceTriplizer;
import hms.StanfordNLPTools;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wn.WordNetManager;

public class WDDescriptionArgumentTypeExtractor {

	
	
	
	public static Set<String> filterPos ;
	
	static {
		filterPos = new HashSet<String>();
		filterPos.add("DT");
//		filterPos.add("JJ");
//		filterPos.add("JJR");
//		filterPos.add("JJS");
	}
	
	
	
	
	
	private static final String ENG = "en";

	public static WDDescription extractArgumentTypesFromDescription(String entityId){
		
		WDDescription wdDesc = new WDDescription();
		
		
		String label = JacksonDBAPI.getItemLabel(entityId, ENG);
		String desc = JacksonDBAPI.getItemDescription(entityId, ENG);
		String sentence = extractMainSentenceFromDescription(desc);
		
		wdDesc.setId(entityId);
		wdDesc.setLabel(label);
		wdDesc.setDescription(desc);
		wdDesc.setMainSentence(sentence);
		
		Map<Integer, SentenceTriple> tripleList = SentenceTriplizer.extractTriples(sentence);
		
		if(tripleList.size() > 0){
			
			String subject = tripleList.get(0).getSubject();
			String object = tripleList.get(0).getObject();
			String predicate = tripleList.get(0).getPredicate();
			
			
			//Clean subject and object from PoS
			List<String> subjectTypes = null;
			List<String> objectTypes = null;
			
			if(subject!=null){
				subjectTypes = getWordPosPairs(subject, WDDescriptionArgumentTypeExtractor.filterPos);
			}
			
			if(object!=null){
				objectTypes = getWordPosPairs(object, WDDescriptionArgumentTypeExtractor.filterPos);
			}
			
			if((label.contains(" of") || desc.contains("in which") || desc.contains("for which") || desc.contains("at which")) || (predicate!=null && predicate.split("/")[0].equals("has"))){
				
				wdDesc.setArg1Types(subjectTypes);
			
				wdDesc.setArg2Types(objectTypes);
				
				
			}
			

			else{
				
				wdDesc.setArg1Types(objectTypes);
			
				wdDesc.setArg2Types(subjectTypes);
				
			}

			wdDesc.setPredicate(predicate);
			
		}
		return wdDesc;
	}
	
	
	/**
	 * Extract and clean the main sentence of an entity description
	 * @param desc
	 * @return
	 */
	public static String extractMainSentenceFromDescription(String desc) {
		String sentence  = null;
		
		
		desc = desc.replace(", but", ". but");
		desc = desc.replace(";", ".");
	
		
		String descNoPar = removeParenthesesContents(desc);
		
		if(StanfordNLPTools.getSentences(descNoPar).size() > 0){
		
			sentence = StanfordNLPTools.getSentences(descNoPar).get(0);
			
			sentence = sentence.replace(",", " ");
	//		sentence = sentence.replace(";", ".");
			sentence = sentence.replace("of a given", "of");
			sentence = sentence.replace("this is a ", " ");
			
		
			sentence = sentence.replace("the object as their", "");
			sentence = sentence.replace("as an option", "");
			
			sentence = sentence.replace("the subject as their", "");
			sentence = sentence.replace("the subject as its", "");
			sentence = sentence.replace("the object as its", "");
			
			sentence = sentence.replace("the subject", "the Subject");
			sentence = sentence.replace("the object", "the Object");
			sentence = sentence.replace("who takes a main role in", "");
			
			
	//		sentence = sentence.replace("the subject", "the entity");
			
	//		sentence = sentence.replace("the object", "the entity");
	
			if(sentence.endsWith(".")){
			
				sentence = sentence.substring(0, sentence.length()-1);
						
			}
		}
		return sentence;
	}
	
	/**
	 * Remove text found in "()" from a text as well as strings that
	 * contain Pxxx or Qxxx
	 * @param text
	 * @return
	 */
	public static String removeParenthesesContents(String text){
		text = text.replaceAll("\\(.*?\\) ?", " ");
		text = text.replaceAll("use [P|Q][0-9.]*", " ");
		text = text.replaceAll("inverse of [P|Q][0-9.]*", " ");
		text = text.replaceAll("[P|Q][0-9.]*", " ");
		return text;
	}
	
	/**
	 * E.g. the/DT subject/NN  -> the subject
	 * @param wordPoSed
	 */
	public static List<String> getWordPosPairs(String textPoSed, Set<String> filterPos){

		List<String> wordList = new ArrayList<String>();
		
		String[] text = textPoSed.split(" ");
	
		int index = 0 ;
		
		for(String t:text){
		
			String[] tArray = t.split("/");
			String word = tArray[0];
			String pos =  tArray[1];
			
			//if(!filterPos.contains(pos) && !word.equals("entity")){
			if(!filterPos.contains(pos)){
				wordList.add(index,word);
				index ++ ;
			}
			
		}
		return wordList;
	}
	
	
	public static void main(String[] args) {
		
		
		String[] testCases = {"P7","P108","P69","P157","P112","P802","P115","P118","P634"};

		for(String p : testCases){
			WDDescription res = extractArgumentTypesFromDescription(p);
			System.out.println(res.getLabel() + ": " + res.getDescription());
			System.out.println("Arg1: " + res.getArg1Types() + " , Arg2: " + res.getArg2Types() + " , Pred: " +res.getPredicate());
		}
	
	}
}



