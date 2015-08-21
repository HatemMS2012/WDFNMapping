package hms.alignment;

import hms.alignment.data.Frame;
import hms.alignment.framenet.FrameNetAPI;
import hms.alignment.framenet.FrameNetContextGenerator;
import hms.alignment.wikidata.WikidataContextGenerator;
import hms.similarity.TextSimilarityUtil;
import hms.similarity.ValueComparator;
import hms.similarity.ValueComparatorFrame;
import hms.wikidata.dbimport.JacksonDBAPI;
import hms.wikidata.graph.WikidataTraverser;
import hms.wikidata.model.PropertyOfficialCategory;
import hms.wikidata.model.StructuralPropertyMapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

import edu.cmu.lti.ws4j.util.StopWordRemover;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

public class CandidateExractor {
	
	
	public static void extractCandidates(String output , int max,PropertyOfficialCategory category, String similarityMethod, boolean useAliases, boolean useStemming,boolean useLexicalUnits) throws FileNotFoundException{

		PrintWriter out = new PrintWriter(output);
		
		Locale locale  = new Locale("en", "UK");
		String pattern = "###.##";
		DecimalFormat decimalFormat = (DecimalFormat)  NumberFormat.getNumberInstance(locale);
		decimalFormat.applyPattern(pattern);

		List<String> propIdList = JacksonDBAPI.getOfficialProperties(category);
		System.out.println("Total " + category + " Properties: " + propIdList.size()  ); 

		System.out.print("Property ID \t Property Lable \t Frame ID \t Frame Label \t Score");
		
		out.println("Similarity Method: " + similarityMethod);
		out.println("Max number of candidate: " + max);
		
		out.println("Stemming: " + useStemming);
		out.println("Aliases: " + useAliases);
		out.println("Lexical units comparison: " + useLexicalUnits);
		out.println("Category: " + category);
		out.println();
		out.println("Property ID \t Property Lable \t Frame ID \t Frame Label \t Score");


		for(String propId : propIdList) {
			
			
			String propLabel = JacksonDBAPI.getItemLabel(propId, "en");
			
			System.out.println("dealing with " + propLabel);
					
			Map<Frame, Double> matchingFrames = null;
			if(propLabel!=null) {
				
				if(useAliases){
					List<String> propAlisases = JacksonDBAPI.getItemAliases(propId, "en");
					
					propAlisases.add(propLabel);
				
					matchingFrames = FrameNetAPI.getFramesByLabel(propAlisases,useStemming,useLexicalUnits);
				}
				else {
					matchingFrames = FrameNetAPI.getFramesByLabel(propLabel,useStemming,useLexicalUnits);

				}
			
				int i = 0 ;
				for( Entry<Frame, Double> f : matchingFrames.entrySet()){
					
					if(i >= max)
						break;
					i++;
					System.out.println(propId + "\t" + propLabel + "\t" + f.getKey().getFrameId() + "\t" + f.getKey().getLabel() + "\t" + decimalFormat.format(f.getValue()));
					out.println(propId + "\t" + propLabel + "\t" + f.getKey().getFrameId() + "\t" + f.getKey().getLabel() + "\t" + decimalFormat.format(f.getValue()));

				}
				out.flush();
				
			}
		}
		out.close();
		
	}
	
	
	public static void extractCandidatesUsingContext(String output , int max,PropertyOfficialCategory category, boolean useStemming) throws FileNotFoundException{

		PrintWriter out = new PrintWriter(output);
		
		Locale locale  = new Locale("en", "UK");
		String pattern = "###.##";
		DecimalFormat decimalFormat = (DecimalFormat)  NumberFormat.getNumberInstance(locale);
		decimalFormat.applyPattern(pattern);

		List<String> propIdList = JacksonDBAPI.getOfficialProperties(category);
		System.out.println("Total " + category + " Properties: " + propIdList.size()  ); 

		System.out.println("Property ID \t Property Lable \t Frame ID \t Frame Label \t Score");
		
		
		out.println("Property ID \t Property Lable \t Frame ID \t Frame Label \t Score");

		List<String> allFrameList = FrameNetAPI.getFrames();
		
		for(String propId : propIdList) {
			
			
			String propLabel = JacksonDBAPI.getItemLabel(propId, "en");
			
			System.out.println("dealing with " + propLabel);
					
			Map<String, Double> matchingFrames = new HashMap<String, Double>();
			ValueComparator bvc =  new ValueComparator(matchingFrames);
			
			
			if(propLabel!=null) {
	
								
				for(String frameId : allFrameList){
					
//					Frame frame = FrameNetAPI.getFrameFullData(frameId);
					
				
					List<String> propertyTokens = WikidataContextGenerator.generateContext(propId,useStemming);
					
					Map<String, Integer> propertyContextVector = WikidataContextGenerator.generateVectorRepresentation(propertyTokens);
					
					
					List<String> frameTokens = FrameNetContextGenerator.generateContext(frameId, useStemming);
					
					Map<String, Integer> frameContextVector = WikidataContextGenerator.generateVectorRepresentation(frameTokens);
					
					
					double cosineSim = TextSimilarityUtil.cosine(propertyContextVector, frameContextVector);
					
					if(cosineSim >= 0.3){
						matchingFrames.put(frameId, cosineSim);
					}
				}
				
				
				 TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
				 sorted_map.putAll(matchingFrames);
				 
				 
				int i = 0 ;
				for( Entry<String, Double> f : sorted_map.entrySet()){
					
					if(i >= max)
						break;
					i++;
					System.out.println(propId + "\t" + propLabel + "\t" +  f.getKey() + "\t" + FrameNetAPI.getFrameLabel(f.getKey()) + "\t" + decimalFormat.format(f.getValue()));

					out.println(propId + "\t" + propLabel + "\t" +  f.getKey() + "\t" + FrameNetAPI.getFrameLabel(f.getKey()) + "\t" + decimalFormat.format(f.getValue()));

				}
				out.flush();
				
			}
		}
		out.close();
		
	}
	
	public static void extractCandidatesByDesc(String output , int max,PropertyOfficialCategory category) throws FileNotFoundException{

		PrintWriter out = new PrintWriter(output);
		
		Locale locale  = new Locale("en", "UK");
		String pattern = "###.##";
		DecimalFormat decimalFormat = (DecimalFormat)  NumberFormat.getNumberInstance(locale);
		decimalFormat.applyPattern(pattern);

		List<String> propIdList = JacksonDBAPI.getOfficialProperties(category);
		
		System.out.println("Total " + category + " Properties: " + propIdList.size()  ); 

		System.out.print("Property ID \t Property Lable \t Frame ID \t Frame Label \t Score");
		
		out.println("Property ID \t Property Lable \t Frame ID \t Frame Label \t Score");


		for(String propId : propIdList) {
			
			String query = "" ;
			
			String propLabel = JacksonDBAPI.getItemLabel(propId, "en");
			
			String propDesc = JacksonDBAPI.getItemDescription(propId, "en");
			
			Map<Frame, Double> matchingFrames = null;
		
			if(propLabel!=null) {
				
				if(propDesc!=null){
					
					query = propLabel + " " + propDesc;
				}
				matchingFrames = FrameNetAPI.getFramesByDefinition(query);

			
				int i = 0 ;
				for(Frame f : matchingFrames.keySet()){
					
					if(i >= max)
						break;
					i++;
					System.out.println(propId + "\t" + propLabel + "\t" + f.getFrameId() + "\t" + f.getLabel() + "\t" + decimalFormat.format(matchingFrames.get(f)));
					out.println(propId + "\t" + propLabel + "\t" + f.getFrameId() + "\t" + f.getLabel() + "\t" + decimalFormat.format(matchingFrames.get(f)));

				}
				out.flush();
				
			}
		}
		out.close();
		
	}
	
	public static void main(String[] args) throws Exception {
		
		
		boolean useStemming = true;
//		boolean useAliases = false;
//		boolean useLexicalUnits = true;
//		String similarityMethod = "EXACT_MATCHING";
		int maxMatchings = 5;
//	
//		
//		extractCandidates("output/test.txt",maxMatchings, PropertyOfficialCategory.Person,similarityMethod,useAliases,useStemming,useLexicalUnits);
		
		
		for (PropertyOfficialCategory type : PropertyOfficialCategory.values()) {
			String fileName = "output/FN-WD_COSINE"+"_"+type+"_max"+maxMatchings   ;
			if(useStemming){
				fileName +="_with_stemming";
			}
			else{
				fileName += "_no_stemming";
			}
			fileName += "_" + type + ".txt";
			extractCandidatesUsingContext(fileName,maxMatchings, type,useStemming);
		}
//		
//		for (PropertyOfficialCategory type : PropertyOfficialCategory.values()) {
//			 
//				String fileName = "output/FN-WD_Matching"+"_"+similarityMethod+"_max"+maxMatchings ;
//				
//				if(useAliases){
//					fileName +="_with_aliases";
//				}
//				else{
//					fileName +="_no_aliases";
//				}
//				if(useLexicalUnits){
//					fileName +="_with_LUs";
//				}
//				else{
//					fileName += "_no_LUs";
//				}
//				if(useStemming){
//					fileName +="_with_stemming";
//				}
//				else{
//					fileName += "_no_stemming";
//				}
//				fileName += "_" + type + ".txt";
//				
//				System.out.println(fileName);
//				
//				extractCandidates(fileName,maxMatchings, type,similarityMethod,useAliases,useStemming,useLexicalUnits);
//
//			
//			}
		
		
	}
	
	
	
}
