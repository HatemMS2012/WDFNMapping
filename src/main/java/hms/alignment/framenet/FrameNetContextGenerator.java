package hms.alignment.framenet;

import hms.alignment.data.Frame;
import hms.alignment.data.SemanticRole;
import hms.alignment.wikidata.WikidataContextGenerator;
import hms.similarity.TextSimilarityUtil;

import java.util.ArrayList;
import java.util.List;

public class FrameNetContextGenerator {
	
	
	public static List<String> generateContext(String frameId, boolean applyStemming){
		
		
		List<String> tokenList = new ArrayList<String>();
		
		
		Frame frame = FrameNetAPI.getFrameFullData(frameId);
		
		
		String label = frame.getLabel();
		
		tokenList.addAll(TextSimilarityUtil.tokenize(label.replace("_", " "), applyStemming));
		
		String definition = frame.getDefinition();		
		if(definition!=null){
			tokenList.addAll(TextSimilarityUtil.tokenize(definition, applyStemming));
		}
		
		List<String> lexicalUnits = frame.getLexicalEntries();
		
		for(String lexicalUnit : lexicalUnits){
			tokenList.addAll(TextSimilarityUtil.tokenize(lexicalUnit, applyStemming));
		}
		
		List<SemanticRole> semanticArguments = frame.getRoles();
		for(SemanticRole sr : semanticArguments){
			tokenList.addAll(TextSimilarityUtil.tokenize(sr.getDefnition(), applyStemming));
			tokenList.addAll(TextSimilarityUtil.tokenize(sr.getRole(), applyStemming));
			
		}
		return tokenList;
				
		
	}

}
