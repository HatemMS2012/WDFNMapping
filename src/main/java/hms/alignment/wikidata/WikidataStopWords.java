package hms.alignment.wikidata;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class WikidataStopWords {

	private static Set<String> stopWordList = new HashSet<String>();
	
	static{
		stopWordList.add("wikidata");
		stopWordList.add("where");
		stopWordList.add("about");
		stopWordList.add("property");
		stopWordList.add("properties");
		stopWordList.add("item");
		stopWordList.add("subject");
		stopWordList.add("be");
		stopWordList.add("different");
		stopWordList.add("until");
		stopWordList.add("from");
		stopWordList.add("for");
		stopWordList.add("which");
		stopWordList.add("who");
		stopWordList.add("items");
		stopWordList.add("a");
		stopWordList.add("an");
		stopWordList.add("me");
		stopWordList.add("being");
		stopWordList.add("being");
		loadStopWords("stopwords_en.txt");
	}
	
	public static boolean isStopWord(String term){
		return stopWordList.contains(term);
	}
	
	private static void loadStopWords(String file){
		
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;

			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   {
			
				stopWordList.add(strLine);
			}

			//Close the input stream
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
}
