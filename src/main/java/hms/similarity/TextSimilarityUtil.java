package hms.similarity;

import hms.StanfordNLPTools;
import hms.alignment.wikidata.WikidataStopWords;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.netlib.util.doubleW;
import org.tartarus.snowball.ext.PorterStemmer;

import com.google.common.collect.Sets;

import dkpro.similarity.algorithms.api.SimilarityException;
import dkpro.similarity.algorithms.api.TermSimilarityMeasure;
import dkpro.similarity.algorithms.api.TextSimilarityMeasure;
import dkpro.similarity.algorithms.lexical.ngrams.WordNGramContainmentMeasure;
import dkpro.similarity.algorithms.lexical.ngrams.WordNGramJaccardMeasure;
import dkpro.similarity.algorithms.lexical.string.BoundedSubstringMatchComparator;
import dkpro.similarity.algorithms.lexical.string.CosineSimilarity;
import dkpro.similarity.algorithms.lexical.string.GreedyStringTiling;
import dkpro.similarity.algorithms.lexical.string.JaroSecondStringComparator;
import dkpro.similarity.algorithms.lexical.string.JaroWinklerSecondStringComparator;
import dkpro.similarity.algorithms.lexical.string.LevenshteinComparator;
import dkpro.similarity.algorithms.lexical.string.LevenshteinSecondStringComparator;
import dkpro.similarity.algorithms.lexical.string.LongestCommonSubsequenceComparator;
import dkpro.similarity.algorithms.lexical.string.LongestCommonSubsequenceNormComparator;
import dkpro.similarity.algorithms.lexical.string.LongestCommonSubstringComparator;
import dkpro.similarity.algorithms.lexical.string.MongeElkanSecondStringComparator;
import dkpro.similarity.algorithms.lexical.string.SubstringMatchComparator;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.StopWordRemover;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;


public class TextSimilarityUtil {


    private static final double WN_THRESHOLD = 0.25;

	private static ILexicalDatabase db = new NictWordNet();
 
    private static RelatednessCalculator[] rcs = {
        new HirstStOnge(db), new LeacockChodorow(db), new Lesk(db),  new WuPalmer(db), 
        new Resnik(db), new JiangConrath(db), new Lin(db), new Path(db)
        };

	private static void allWNSimilarities( String word1, String word2 ) {
	WS4JConfiguration.getInstance().setMFS(true);
		for ( RelatednessCalculator rc : rcs ) {
		        double s = rc.calcRelatednessOfWords(word1, word2);
		        System.out.println( rc.getClass().getName()+"\t"+s );
		}
	}
	
	public static double calculateTextSimilairty(String text1, String text2,TextSimilarityMethod method){
		
		
		TextSimilarityMeasure measure = null;
		
		if(method.equals(TextSimilarityMethod.WordNGramContainment)){
			measure = new WordNGramContainmentMeasure(1);   

		}

		else if(method.equals(TextSimilarityMethod.WordNGramJaccard)){
			measure = new WordNGramJaccardMeasure(1);   

		}
	
		
		List<String> tokens1 = tokenize(text1,true);   
		 List<String> tokens2 = tokenize(text2,true);

		double score = 0;
		try {
			score = measure.getSimilarity(tokens1,tokens2);
		} catch (SimilarityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return score;
	}
	
public static double calculateTextSimilairty(List<String> tokens1, List<String> tokens2,TextSimilarityMethod method){
		
		
		TextSimilarityMeasure measure = null;
		
		if(method.equals(TextSimilarityMethod.WordNGramContainment)){
			measure = new WordNGramContainmentMeasure(1);   

		}

		else if(method.equals(TextSimilarityMethod.WordNGramJaccard)){
			measure = new WordNGramJaccardMeasure(1);   

		}
	

		double score = -1;
		try {
			score = measure.getSimilarity(tokens1,tokens2);
		} catch (SimilarityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return score;
	}
	
	
	public static double calculateTermSimilarity(String term1, String term2, TermSimilarityMethod method){
		
		TermSimilarityMeasure simMeasure = null;
		try {
			if(method.equals(TermSimilarityMethod.BoundedSubstringMatch)){
				simMeasure = new BoundedSubstringMatchComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.CosineSimilarity)){
				simMeasure = new CosineSimilarity() ;
			}
			else if(method.equals(TermSimilarityMethod.GreedyStringTiling)){
				simMeasure = new GreedyStringTiling(3) ;
			}
			else if(method.equals(TermSimilarityMethod.JaroSecondString)){
				simMeasure = new JaroSecondStringComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.JaroWinklerSecondString)){
				simMeasure = new JaroWinklerSecondStringComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.Levenshtein)){
				simMeasure = new LevenshteinComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.LevenshteinSecondString)){
				simMeasure = new LevenshteinSecondStringComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.LongestCommonSubsequence)){
				simMeasure = new LongestCommonSubsequenceComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.LongestCommonSubsequenceNorm)){
				simMeasure = new LongestCommonSubsequenceNormComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.LongestCommonSubstring)){
				simMeasure = new LongestCommonSubstringComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.MongeElkanSecondString)){
				simMeasure = new MongeElkanSecondStringComparator();
			}
			else if(method.equals(TermSimilarityMethod.SubstringMatch)){
				simMeasure = new SubstringMatchComparator() ;
			}
			else if(method.equals(TermSimilarityMethod.WordNet)){
				return calculateWNSimilarity(term1, term2,WordNetSimilarityMethod.Path,true);
			}
		
			return simMeasure.getSimilarity(term1, term2);
		} catch (SimilarityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public static double calculateTermSimilarityAll(String term1, String term2){
		
		double similarity = 0;
		
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.GreedyStringTiling);
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.JaroSecondString);
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.JaroWinklerSecondString);
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.LongestCommonSubsequence);
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.LongestCommonSubsequenceNorm);
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.LongestCommonSubstring);
		similarity += TextSimilarityUtil.calculateTermSimilarity(term1.toLowerCase(), term2.toLowerCase(),TermSimilarityMethod.MongeElkanSecondString);

		return similarity;
	}
	
	public static double calculateTermSimilaritySimple(String term1, String term2){
		
		double similarity = 0;
		term1 = term1.toLowerCase();
		term2 = term2.toLowerCase();
		
		if(term1.contains(term2) || term2.contains(term1)){
			return calculateTermSimilarity(term1,term2, TermSimilarityMethod.LongestCommonSubsequence);
		}
		
		return similarity;
	}
	
	public static double calculateTermSimilarityExactMatch(String term1, String term2, boolean stem){
		
		double similarity = 0;
		
		List<String> term1Arr = tokenize(term1,stem);
		List<String> term2Arr = tokenize(term2,stem);
		
			
			for (String term1Comp : term1Arr) {
				
				for (String term2Comp : term2Arr) {
				
//					System.out.println(term1Comp + " " + term2Comp);
					if(term1Comp.equals(term2Comp)){
						similarity +=1;
					}
				}

		}
		
		similarity = similarity/Math.min(term1Arr.size(), term2Arr.size());
		
		return similarity;
	}
	
	public static double calculateWNSimilarity(String word1, String word2, WordNetSimilarityMethod method, boolean max){
		
		
		if(word1 == null || word2 == null)
			return 0;
		
		List<String> wordArr1 = new ArrayList<String>();
		List<String> wordArr2 = new ArrayList<String>();
		
		
		wordArr1 = StanfordNLPTools.lemmatize(word1);
		wordArr1 = tokenize(wordArr1.toString(),false);
		
		wordArr2= StanfordNLPTools.lemmatize(word2);
		wordArr2 = tokenize(wordArr2.toString(),false);
			

		
		
		if(wordArr1.size() > 1 || wordArr2.size() > 1 && max) {
			return calculateWNSimilarityMax(wordArr1, wordArr2, method);
		}
		
		else if(wordArr1.size() > 1 || wordArr2.size() > 1 && !max) {
			
			return calculateWNSimilarityAvg(wordArr1, wordArr2, method);

		}
		WS4JConfiguration.getInstance().setMFS(true);
		
		RelatednessCalculator rc = null ;
		
		if(method.equals(WordNetSimilarityMethod.Resnik)){
			
			rc = new  Resnik(db); //Resnik
		}
//		if (method.equals(WordNetSimilarityMethod.HirstStOnge)) {
//
//			rc = new HirstStOnge(db); // Resnik
//		}
		if (method.equals(WordNetSimilarityMethod.JiangConrath)) {

			rc = new JiangConrath(db); // Resnik
		}
//		if (method.equals(WordNetSimilarityMethod.Lesk)) {
//
//			rc = new Lesk(db); // Resnik
//		}
		if (method.equals(WordNetSimilarityMethod.Lin)) {

			rc = new Lin(db); // Resnik
		}
		if (method.equals(WordNetSimilarityMethod.Path)) {

			rc = new Path(db); // Resnik
		}
		if (method.equals(WordNetSimilarityMethod.WuPalmer)) {

			rc = new WuPalmer(db); // Resnik
		}
		
		if (method.equals(WordNetSimilarityMethod.LeacockChodorow)) {

			rc = new LeacockChodorow(db); // Resnik
		}
		
		double norm = rc.calcRelatednessOfWords(word1, word1);

		double sim = rc.calcRelatednessOfWords(word1, word2);
		
		double orgSim = sim/norm;
		
		return orgSim;
		
	}
	
	public static double calculateWNSimilarityMax(List<String> text1, String word2, WordNetSimilarityMethod method){
		
		//Get max similarity
		double sim = 0;
		
		for(String word: text1){
			double temp = calculateWNSimilarity(word, word2,method,true);	
			if(temp > sim){
				sim = temp;
			}
		}
	
		return sim;
		
	}
	
	public static double calculateWNSimilarityMax(List<String> text1,List<String> text2, WordNetSimilarityMethod method) {

		// Get max similarity
		double sim = 0;

		for (String word : text2) {

			double temp = calculateWNSimilarityMax(text1, word,method);
			
			
			if(temp > sim){
				sim = temp;
			}
			

		}
		
		return sim;

	}
	
	public static double calculateWNSimilarityAvg(List<String> text1,List<String> text2, WordNetSimilarityMethod method) {

		// Get max similarity
		double sim = 0;

		for(String word1:text1) {
		
			for (String word2 : text2) {
	
				double temp = calculateWNSimilarity(word1, word2, method,false);
				sim += temp;
			}
		}
		sim = sim/(double)Math.min(text1.size(), text2.size());
		
		return sim;

	}
	
	
	public static double calculateStemOverlap(String text1, String text2){
		
		if(text1==null || text2 == null)
			return 0;
		
		List<String> tokenList1 = tokenize(text1, true);
		
		List<String> tokenList2 = tokenize(text2, true);
		
		int totalTokens = tokenList1.size() + tokenList2.size();
		
		int countOverlap = 0 ;
		for(String tk1 : tokenList1){
			
			for(String tk2: tokenList2){
				
				if(tk1.equals(tk2)){
					
					countOverlap ++ ;
					break;
				}
				
			}
		}
		return (double) countOverlap / (double) totalTokens;
		
		
	}
	
	public static double calculateStemOverlap(Collection<String> text1, String word){
		
		double countOverlap = 0;
		
		for(String word2 : text1){
			
			if(word2.trim().equalsIgnoreCase(word.trim())){
				countOverlap ++ ;
			}
		}
		
		return countOverlap;
	}
	
	public static double calculateStemOverlap(Collection<String> text1, Collection<String> text2 ){
		
		double countOverlap = 0;
		
		for(String word2 : text1){
			
			countOverlap += calculateStemOverlap(text2,word2);
		}
		
		return countOverlap;
	}
//	public static void openNLP() throws InvalidFormatException, IOException{
//		InputStream is = new FileInputStream("models/en-token.bin");
//		TokenizerModel model = new TokenizerModel(is);
//		Tokenizer tokenizer = new TokenizerME(model);
//		StopWordRemover a = StopWordRemover.getInstance();
//		
//		String tokens[] = tokenizer.tokenize("Do you like the good weahter. It is an amazing stuff");
//		String[] bb = a.removeStopWords(tokens);
//		System.out.println(Arrays.asList(tokens));
//		System.out.println(Arrays.asList(bb));
//
//	}
	

//	public static void lucene() throws Exception {
//		 
//		String text = "I can't beleive: that the Carolina Hurricanes, won the 2005-2006 Stanley Cup.";
//		
////		StandardTokenizer tokenizer=new StandardTokenizer(Version.LUCENE_CURRENT,new StringReader());
//			
//			StringReader reader = new StringReader(text);
////			StandardTokenizer tokenizer = new StandardTokenizer(Version.LUCENE_35,reader);
//			 
//			LowerCaseTokenizer tokenizer = new LowerCaseTokenizer(Version.LUCENE_35,reader);
//
//			final StandardFilter standardFilter = new StandardFilter(Version.LUCENE_35, tokenizer);
//			final StopFilter stopFilter = new StopFilter(Version.LUCENE_35, standardFilter, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
//			
//			final CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
//		    final PorterStemmer stemmer = new PorterStemmer();
//
//			 stopFilter.reset();
//			    while(stopFilter.incrementToken()) {
//			        final String token = charTermAttribute.toString().toString();
//			        System.out.println("token: " + token);
//			       
//			        stemmer.setCurrent(token);
//
//			        stemmer.stem();
//			        
//			        final String current = stemmer.getCurrent();
//
////			        System.out.println("stemmed token: " + current);
//			    }
//		}
	
	public static List<String> tokenize(String text, boolean stem) {

		List<String> tokens  = new ArrayList<String>();
		
		// StandardTokenizer tokenizer = new  StandardTokenizer(Version.LUCENE_35,reader);
		
		StringReader reader = new StringReader(text);


		LowerCaseTokenizer tokenizer = new LowerCaseTokenizer(Version.LUCENE_35, reader);

		final StandardFilter standardFilter = new StandardFilter(Version.LUCENE_35, tokenizer);
		
		final StopFilter stopFilter = new StopFilter(Version.LUCENE_35,	standardFilter, StopAnalyzer.ENGLISH_STOP_WORDS_SET);

		final CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
		
		try {
			stopFilter.reset();
		
		
			while (stopFilter.incrementToken()) {
				String token = charTermAttribute.toString().toString();
				
				if(WikidataStopWords.isStopWord(token)){
					continue;
				}
				
				if(token.length() > 1){
				
					
					if(stem){
					    final PorterStemmer stemmer = new PorterStemmer();

						stemmer.setCurrent(token);
						stemmer.stem();
						token = stemmer.getCurrent();
					}
					tokens.add(token);
				}
					
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tokens;

	}

	 public static double cosine(Map<String, Integer> v1, Map<String, Integer> v2) {
          Set<String> both = Sets.newHashSet(v1.keySet());
          both.retainAll(v2.keySet());
          double sclar = 0, norm1 = 0, norm2 = 0;
          for (String k : both) sclar += v1.get(k) * v2.get(k);
          for (String k : v1.keySet()) norm1 += v1.get(k) * v1.get(k);
          for (String k : v2.keySet()) norm2 += v2.get(k) * v2.get(k);
          return sclar / Math.sqrt(norm1 * norm2);
	 }
	public static void main(String[] args) {
		
		List<String> t1 = new ArrayList<String>();
		List<String> t2 = new ArrayList<String>();
		
		t1.add("sky"); t1.add("star");
		t2.add("sky"); t2.add("earth"); t2.add("star");
		
		double s = calculateStemOverlap(t1, "star");
		System.out.println(s);
		
		s = calculateStemOverlap(t1, t2);
		System.out.println(s);
		
////		calculateWNSimilarityAvg(t1, t2, WordNetSimilarityMethod.JiangConrath);
//		
//		calculateWNSimilarity("sky", "sea", WordNetSimilarityMethod.Resnik,false);
//		calculateWNSimilarity("sky", "sun", WordNetSimilarityMethod.Path,false);
//
//
////		System.out.println(calculateWNSimilarity("sequence of images that give the impression of movement", "A socially and/or monetarily significant entity which is given to a Competitor according to Score or Rank.", WordNetSimilarityMethod.Resnik ));
////		System.out.println(1.7976931348623157E308 + 1.7976931348623157E308 +1.7976931348623157E308);
////		calculateTermSimilarityExactMatch("educated at", "studied at university");
//		
////		System.out.println(calculateTermSimilarityExactMatch("Agree or refus to act","sex or gender",true));
////		System.out.println(tokenize("Agree or refus to act", true));
//		
////		System.out.println(calculateWNSimilarity("care", "auto"));
//		
//		System.out.println(calculateStemOverlap("Jack studied at the university", "Mary studies at the university of Passau and the university of vienna"));
//		
//		System.out.println(calculateStemOverlap("Jack studied at the university", "Mary studies at the university of Passau and the university of vienna"));
	}
	
	
	public static String applyStemmer(String query) {
		final PorterStemmer stemmer = new PorterStemmer();
		String result = "" ;
		String[] queryArr = query.split(" ");
		
		for(String queryItem : queryArr){
			stemmer.setCurrent(queryItem);
			stemmer.stem();
			result = result + stemmer.getCurrent() + " ";
		}
		result.trim();
		return result;
	}
}
