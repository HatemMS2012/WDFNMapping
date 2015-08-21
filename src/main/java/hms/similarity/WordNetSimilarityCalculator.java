package hms.similarity;

import hms.StanfordNLPTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

public class WordNetSimilarityCalculator {

	private static ILexicalDatabase db = new NictWordNet();

	
	
	public static double calculateSimilarityLemmatize(String text1, String text2, WordNetSimilarityMethod method) {
		
		if(text1 == null || text2 == null)
			return 0;
		List<String> wordArr1 = new ArrayList<String>();
		List<String> wordArr2 = new ArrayList<String>();
		
		
		wordArr1 = StanfordNLPTools.lemmatize(text1);
		wordArr1 = TextSimilarityUtil.tokenize(wordArr1.toString(),false);
		
		wordArr2= StanfordNLPTools.lemmatize(text2);
		wordArr2 = TextSimilarityUtil.tokenize(wordArr2.toString(),false);
			
		double similarity = calculateSimilarity(wordArr1, wordArr2, method);
		
		
		return similarity;
		
		
	}
	
	
	public static double calculateSimilarityNoLemmatization(Collection<String> text1, String word, WordNetSimilarityMethod method) {
		
		double similarity = 0 ;
		if(text1 == null || word == null)
			return 0;
	
		for(String w: text1){
			
			similarity+= calculateSimilarity(w, word, method);
		}
		
		return similarity;
		
		
	}
	
	public static double calculateSimilarityNoLemmatization(Collection<String> text1, Collection<String> text2, WordNetSimilarityMethod method) {
		
		double similarity = 0 ;
		if(text1 == null || text2 == null)
			return 0;
	
		for(String w: text1){
			
			similarity+= calculateSimilarityNoLemmatization(text2,w,method);
		}
		
		return similarity;
		
		
	}
	
	
	
	public static double calculateSimilarity(String word1, String word2, WordNetSimilarityMethod method) {

		word1 = word1.toLowerCase().trim();
		word2 = word2.toLowerCase().trim();
		
		
		RelatednessCalculator rc = getSimilarityCalculator(method);
		
		
		double maxSimilarity =  rc.calcRelatednessOfWords(word1, word1);

		double actualSimilarity = rc.calcRelatednessOfWords(word1, word2);
		
		double normalizedSimilarity = actualSimilarity/maxSimilarity;
		
		return normalizedSimilarity;
		
		
		
	}

	
	public static double calculateSimilarity(List<String> wordList1, List<String> wordList2, WordNetSimilarityMethod method) {
		
		double sim = 0;

		for(String word1:wordList1) {
		
			for (String word2 : wordList2) {
	
				double temp = calculateSimilarity(word1, word2, method);
				sim += temp;
			}
		}
		
		sim = sim /(wordList1.size() + wordList2.size());
		
		return sim;
	}
	
	
	
	private static RelatednessCalculator getSimilarityCalculator(WordNetSimilarityMethod method) {
	
		RelatednessCalculator rc = null;
		
		if (method.equals(WordNetSimilarityMethod.Resnik)) {

			rc = new Resnik(db);
		}
		if (method.equals(WordNetSimilarityMethod.HirstStOnge)) {

			rc = new HirstStOnge(db);
		}
		if (method.equals(WordNetSimilarityMethod.JiangConrath)) {

			rc = new JiangConrath(db);
		}
		if (method.equals(WordNetSimilarityMethod.Lesk)) {

			rc = new Lesk(db);
		}
		if (method.equals(WordNetSimilarityMethod.Lin)) {

			rc = new Lin(db);
		}
		if (method.equals(WordNetSimilarityMethod.Path)) {

			rc = new Path(db);
		}
		if (method.equals(WordNetSimilarityMethod.WuPalmer)) {

			rc = new WuPalmer(db);
		}

		if (method.equals(WordNetSimilarityMethod.LeacockChodorow)) {

			rc = new LeacockChodorow(db);
		}
		return rc;
	}
	
	public static void main(String[] args) {
		
		
//		System.out.println(calculateSimilarity("sky", "sea", WordNetSimilarityMethod.Resnik));
//		System.out.println(calculateSimilarity("sky", "love", WordNetSimilarityMethod.Resnik));
//
//		
//		
		List<String> t1 = new ArrayList<String>();
		List<String> t2 = new ArrayList<String>();
		
		t1.add("sky"); t1.add("star");
		t2.add("sky"); t2.add("earth");
//		
//		System.out.println(calculateSimilarity(t1,t2, WordNetSimilarityMethod.Resnik));
		
		double s = calculateSimilarityLemmatize("Frederick William I of Prussia", "Egg", WordNetSimilarityMethod.Path);
		System.out.println(s);

		s = calculateSimilarityNoLemmatization(t1, "Egg", WordNetSimilarityMethod.Path);

		System.out.println(s);
		
		s = calculateSimilarityNoLemmatization(t2, t2, WordNetSimilarityMethod.Path);

		System.out.println(s);

	
	}
}
