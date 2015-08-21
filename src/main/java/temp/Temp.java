package temp;

import hms.SentenceTriple;
import hms.SentenceTriplizer;
import hms.StanfordNLPTools;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.model.PropertyOfficialCategory;

import java.util.List;
import java.util.Map;

public class Temp {

	
	
	private static final String ENG = "en";

	public static void main(String[] args) {
		
		System.out.println(removeParenthesesContents("person or organization that uses a given object, for buildings use P466")); 
 
//		for (PropertyOfficialCategory type : PropertyOfficialCategory.values()) {
			
			List<String> propIdList = JacksonDBAPI.getOfficialProperties(PropertyOfficialCategory.Works);
		
			for(String id : propIdList){
				
				String label = JacksonDBAPI.getItemLabel(id, ENG);
				String desc = JacksonDBAPI.getItemDescription(id, ENG);
				
				if(desc!=null){
				
					String sentence = extractMainSentenceFromDescription(desc);
					
					Map<Integer, SentenceTriple> tripleList = SentenceTriplizer.extractTriples(sentence);
					
					if(tripleList.size() >0 ){
						SentenceTriple triple = tripleList.get(0);
						System.out.println(label + "\t" + desc + "\t" + triple.getPredicate()+"("+triple.getSubject()+","+triple.getObject()+")");
					}
					
				
				}
				
				else{
					System.out.println(id + "\t" + label + "\t" + desc + "\t" + "NO DESC" );
				}
			}
//		}
	
	}

	private static String extractMainSentenceFromDescription(String desc) {
		
		String descNoPar = removeParenthesesContents(desc);
		
		String sentence = StanfordNLPTools.getSentences(descNoPar).get(0);
		
		sentence = sentence.replace(",", " ");
		
		sentence = sentence.replace("of a given", "of");

		if(sentence.endsWith(".")){
		
			sentence = sentence.substring(0, sentence.length()-1);
					
		}
		return sentence;
	}
	
	public static String removeParenthesesContents(String text){
		text = text.replaceAll("\\(.*?\\) ?", " ");
		text = text.replaceAll("use [P|Q][0-9.]*", " ");
		text = text.replaceAll("inverse of [P|Q][0-9.]*", " ");
		text = text.replaceAll("[P|Q][0-9.]*", " ");
		return text;
	}
}
