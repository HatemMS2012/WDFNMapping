package hms.alignment.evaluation;

import hms.similarity.WordNetSimilarityMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluator {

	
	public static void evaluateProperty(String resultFile) throws IOException{
		
		
		Map<String, Map<String, String>> groundTruthMap = loadGroundTruth("test/input/test_cases_new.txt");
		
		System.out.println("Case \t subject(pred/GT) \t correct_subject \t object(pred/GT) \t correct_object");

		
		FileInputStream fstream = new FileInputStream(resultFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		int totalCases = 0;
		int totalCorrectSubjects = 0;
		int totalCorrectObjects = 0;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
			int correctSubject = 0 ;
			int correctObject = 0;
			totalCases ++;
			
			String[] lineArr = strLine.split("\t");
			
			String frameId = lineArr[0];
			String propId = lineArr[1];
			String subject = lineArr[2];
			String object = lineArr[3];
			String subjectRole ="";
			String objectRole = "";
			if(subject.contains("=")){
				subjectRole = subject.substring(subject.indexOf("{")+1, subject.indexOf("="));
			}
			if(object.contains("=")){
				objectRole = object.substring(object.indexOf("{")+1, object.indexOf("="));
			}
			
			
			String groundSubject= groundTruthMap.get(propId+"_"+frameId).get("subject");
			String groundObject= groundTruthMap.get(propId+"_"+frameId).get("object");
			String groundCase =  groundTruthMap.get(propId+"_"+frameId).get("case");
			
			if(groundSubject.equals(subjectRole)){
				correctSubject = 1;
			}
			if(groundObject.contains(objectRole)){
				correctObject = 1 ;
			}
			totalCorrectSubjects +=correctSubject;
			totalCorrectObjects += correctObject;
			
			System.out.println(groundCase + "\t" + subjectRole+"/"+groundSubject + "\t" + correctSubject  + "\t" + objectRole+"/"+groundObject + "\t" + correctObject);
		}
		
		System.out.println( "Total \t" + totalCases + "\t" + totalCases);
		System.out.println( "AVG \t" + (totalCorrectSubjects/(double)totalCases) + "\t" + (totalCorrectObjects/(double)totalCases));

		br.close();
	}
	
	public static void evaluate(String resultFile, String subjectRole, String objectRole) throws IOException{
	

		//Obj: Stanford University	{Qualification=
		
		int truePositivesSubject = 0;
		int falsePositivesSubject = 0;
		
		int truePositivesObject = 0;
		int falsePositivesObject = 0;
		
		int totalSubject=0;
		int totalObject=0;
		// Open the file
		FileInputStream fstream = new FileInputStream(resultFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
		 
			if(strLine.equals("..................."))
				continue;
			
			String[] lineArr = strLine.split("\t");
			String arg = lineArr[0];
			if(arg.contains("null"))
				continue;
			if(!strLine.contains("="))
				continue;
			String argLabel =  lineArr[1].substring(1, lineArr[1].indexOf("="));
			
			
			
			if(arg.contains("Sub:")){
				totalSubject ++;
				if(argLabel.equalsIgnoreCase(subjectRole)){
					
					truePositivesSubject++;
				}
				else{
					falsePositivesSubject ++ ;
				}
			}
			if(arg.contains("Obj:")){
				totalObject ++ ;
				if(argLabel.equalsIgnoreCase(objectRole)){
					
					truePositivesObject++;
				}
				else{
					falsePositivesObject ++ ;
				}
			}
			
		}

		//Close the input stream
		br.close();
		System.out.println(truePositivesSubject + "\t" + falsePositivesSubject + "\t" + totalSubject + "\t" + ((double)truePositivesSubject/(double)totalSubject)
							+ "\t" + truePositivesObject + "\t" + falsePositivesObject + "\t" + totalObject + "\t" + ((double)truePositivesObject/(double)totalObject));
		
	}
	
	public static void main(String[] args) throws IOException {
	
		String wnDir = "test/output/WN/";
		String[] wnFileNames = new File(wnDir).list();
		
		for(String wnFile : wnFileNames){
			System.out.println(wnFile);
			
			evaluateProperty(wnDir+wnFile);
			
			System.out.println("......................");
		}
		
		String soDir = "test/output/stemOverlap/";
		String[] soFileNames = new File(soDir).list();
		
		for(String soFile : soFileNames){
			System.out.println(soFile);
			
			evaluateProperty(soDir+soFile);
			System.out.println("......................");
		}
		

		
//		System.out.println("...........\n");
//
//		evaluateProperty("test/output/WN/arg_test_case_1_only_property_wn.txt");
//		
//		System.out.println("...........\n");
//		evaluateProperty("test/output/stemOverlap/arg_test_case_1_only_property_stemOverlap.txt");
		
		
//		Map<String, Map<String, String>> groundTruthMap = loadGroundTruth("test/input/test_cases.txt");
//
//		String dir = "test/output/wn/";
//		
//		
//		String[] files = new File(dir).list();
//		
//		System.out.println("Config \t truePositivesSubject \t falsePositivesSubject \t totalSubject  \t accuracySubject "
//				+ "\t truePositivesObject \t falsePositivesObject \t totalObject   \t accuracyObject");
//
//		for(String fileName:files){
//			
//			//annotation_P106_FN_SemanticPredicate_27_Resnik_d=1.txt
//			String propId = fileName.substring(fileName.indexOf("_")+1, fileName.indexOf("_FN"));
//			String frameId = fileName.substring(fileName.indexOf("_FN")+1, fileName.indexOf("-"));
//			
//			Map<String, String> groundRoles = groundTruthMap.get(propId+"_"+frameId);
//			String groundSubject = groundRoles.get("subject");
//			String groundObject = groundRoles.get("object");
//			
//			
//			System.out.print(groundRoles.get("case") + "\t");
//			
//			evaluate(dir+fileName, groundSubject, groundObject);
//
//		}	
	}
	
	public static Map<String, Map<String, String>> loadGroundTruth(String testCaseFile ) throws IOException{
		
		Map<String, Map<String, String>> groundTruthMap  = new HashMap<String, Map<String,String>>();
		
		FileInputStream fstream = new FileInputStream(testCaseFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		//Read File Line By Line
		
		int i = 0;
		while ((strLine = br.readLine()) != null)   {
			
			i++;
			//ignore the first line
			if(i==1)
				continue;
				 
			String[] lineArr = strLine.split(",");
			String propertyID = lineArr[0];
			String frameID =  lineArr[2];
			String propLabel =  lineArr[1];
			String frameLabel =  lineArr[3];
			String subject = lineArr[4].trim();
			String object =  lineArr[5].trim();
			
			String key = propertyID+"_"+frameID ;
			Map<String,String> groundLabels = new HashMap<String, String>();
			groundLabels.put("subject", subject);
			groundLabels.put("object", object);
			groundLabels.put("case", propLabel+"<->" + frameLabel);
			groundTruthMap.put(key, groundLabels);
			
		}
		
		br.close();
		return groundTruthMap;
	}
}
