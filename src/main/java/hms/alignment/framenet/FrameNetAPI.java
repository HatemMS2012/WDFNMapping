package hms.alignment.framenet;

import hms.StanfordNLPTools;
import hms.alignment.data.Frame;
import hms.alignment.data.SemanticRole;
import hms.db.DBConnector;
import hms.similarity.TextSimilarityMethod;
import hms.similarity.TextSimilarityUtil;
import hms.similarity.ValueComparator;
import hms.similarity.ValueComparatorFrame;
import hms.similarity.WordNetSimilarityMethod;
import hms.wikidata.dbimport.JacksonDBAPI;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;



public class FrameNetAPI {

	
	public static String SELECT_FRAME_GIVEN_LABEL = "select * from frames where label like ?";
	public static String SELECT_FRAME_LABEL = "select label from frames where frame_id = ? " ;
	public static String SELECT_ALL_FRAMES = "select frame_id from frames";
	
	public static String SELECT_FRAME_DATA = "select frame_id as fID, label fLabel, definition fDef " +
										 	  "from framenet.frames where frame_id = ?";
	
	public static String SELECT_SEMANTIC_ROLE_DATA = "select semantic_role sr, core_type srType, definition srDef "+
													 "from  semantic_arguments " + 
													 "where frame_id = ? and core_type = 'core'" ;
	
	public static String SELECT_LEXICAL_ENTRIES_FOR_FRAME = "select lemma from lexical_entry  where frame_id = ? ";
	
	public static String SELECT_RELATED_FRAMES = "select target_frame_id,relation_name from frame_relation where src_frame_id = ?";

	public static String SELECT_FRAMES_BY_LU = "select * from lexical_entry where lemma like  ?" ;
	
	public static String SELECT_FE_FILLERS = "SELECT fe_filler FROM framenet.lexical_units_frame " +
											  "where frame_label like ? and frame_element like ?" ;
	
	public static String SELECT_FE_FILLER_ANNOTATION = "SELECT frame_element FROM framenet.lexical_units_frame " +
			  "where frame_label like ? and fe_filler like ?" ;

	
	public static List<String> getFEFillers(String frameLabel, String fe){
		

		List<String> fillerSet = new ArrayList<String>();
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_FE_FILLERS);
		
		try {
			st.setString(1, frameLabel);
			st.setString(2, fe);
				
			ResultSet res = st.executeQuery();
			
			while(res.next()){
				
				String filler =  res.getString("fe_filler");
				fillerSet.add(filler);
			}
			
			
			res.close();
			st.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return fillerSet;
	}
	
	public static Set<String> getFEFillerAnnotation(String frameLabel, String filler){
		

		Set<String> fillerAnnotation = new HashSet<String>();
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_FE_FILLER_ANNOTATION);
		
		try {
			st.setString(1, frameLabel);
			st.setString(2, filler);
				
			ResultSet res = st.executeQuery();
			
			while(res.next()){
				
				String annotation =  res.getString("frame_element");
				fillerAnnotation.add(annotation);
			}
			
			
			res.close();
			st.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return fillerAnnotation;
	}
	
	public static List<String> getFramesByLU(String lu){
		
		List<String> frameIdList = new ArrayList<String>();
		
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_FRAMES_BY_LU);
		
		try {
			st.setString(1, lu);
				
			ResultSet res = st.executeQuery();
			
			while(res.next()){
				
				String frameId =  res.getString("frame_id");
				frameIdList.add(frameId);
			}
			
			
			res.close();
			st.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return frameIdList;
		
	}
	/**
	 * Get the label of a given frame (identified by its id)
	 * @param frameId
	 * @return
	 */
	public static String getFrameLabel(String frameId){
		
		String label = null;
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_FRAME_LABEL);
		
		try {
			st.setString(1, frameId);
				
			ResultSet res = st.executeQuery();
			
			if(res.next()){
				
				label = res.getString("label");
				res.close();
				st.close();
				return label;
			}
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	public static List<String> getLexicalEntries(String frameId){

		List<String> result = new ArrayList<String>();
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_LEXICAL_ENTRIES_FOR_FRAME);
		
		try {
			st.setString(1, frameId);
				
			ResultSet res = st.executeQuery();
			
			while(res.next()){
				
				result.add(res.getString("lemma"));
			}
			
			res.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
		
	}
	
	/**
	 * Get the list of frames related to a given frame (identified by its id)
	 * @param frameId
	 * @return
	 */
	public static Map<String, String> getRelatedFrames(String frameId){

		Map<String, String> relatedFramesMap = new HashMap<String, String>();
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_RELATED_FRAMES);
		
		try {
			st.setString(1, frameId);
				
			ResultSet res = st.executeQuery();
			
			while(res.next()){
				String targetFrame = res.getString("target_frame_id");
				String relation = res.getString("relation_name");
				relatedFramesMap.put(targetFrame,relation);
			}
			
			res.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return relatedFramesMap;
		
	}
	/**
	 * Get frames from FrameNet with labels that match the input query
	 * @param label
	 * @param exactMatch If true apply exact match (ignore case)
	 * @return
	 */
	public static List<String> getFrameByLabel(String label,boolean exactMatch){
		
		List<String> result = new ArrayList<String>();
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_FRAME_GIVEN_LABEL);
		
		try {
			if(exactMatch){
				st.setString(1,label);
			}
			else{
				st.setString(1, "%"+label+"%");
			}
			
			ResultSet res = st.executeQuery();
			
			while(res.next()){
				
				result.add(res.getString("frame_id"));
			}
			
			res.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * Generate for a give frame (identified with its ID) the set of related data. This include
	 * the frame lable and definition in addition to the its semantic roles (label, type and definitions) 
	 * @param frameId
	 * @return
	 */
	public static Frame getFrameFullData(String frameId){
		
		Frame frame = new Frame();
		
		
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_FRAME_DATA);
		
		ResultSet res;
		try {
			st.setString(1, frameId);
			res = st.executeQuery();
			
			if(res.next()){
				
				
				String frameLabel = res.getString("fLabel");
				String frameDef = res.getString("fDef");
			
				
			
				frame.setDefinition(frameDef);
				frame.setFrameId(frameId);
				frame.setLabel(frameLabel);
				
				
				List<SemanticRole> semRoleList = new ArrayList<SemanticRole>();
				
				semRoleList = getSemanticRoles(frameId);
				
				frame.setRoles(semRoleList);
				
				List<String> lexicalEntries = getLexicalEntries(frameId);
				
				frame.setLexicalEntries(lexicalEntries);
				
				
				Map<String, String> relatedFrames = getRelatedFrames(frameId);
				
				frame.setRelatedFrames(relatedFrames);
			}
			res.close();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return frame;
		
	}
	private static List<SemanticRole> getSemanticRoles(String frameId) throws SQLException {
		
		List<SemanticRole> semRoleList = new ArrayList<SemanticRole>();
		PreparedStatement st2 = DBConnector.createPreparedStatemen(SELECT_SEMANTIC_ROLE_DATA);
		st2.setString(1, frameId);
		ResultSet res2 = st2.executeQuery();
		
		while(res2.next()){
			
			String semanticRole = res2.getString("sr");
			String semanticRoleType = res2.getString("srType");
			String semanticRoleDef = res2.getString("srDef");
						
			SemanticRole sr = new SemanticRole();
			sr.setDefnition(semanticRoleDef);
			sr.setRole(semanticRole);
			sr.setType(semanticRoleType);
			
			semRoleList.add(sr);
		}
		res2.close();
		st2.close();
		return semRoleList;
	}
	
	public static List<String> getFrames(){
		
		List<String> frameIdList = new ArrayList<String>();
		PreparedStatement st = DBConnector.createPreparedStatemen(SELECT_ALL_FRAMES);
		
		ResultSet res;
		try {
			res = st.executeQuery();
			
			while(res.next()){
				
				String frameId = res.getString("frame_id");
				frameIdList.add(frameId);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return frameIdList;
	}
	
	/**
	 * Get frames matching a given query.
	 * The decision on the similarity between a frame and the query is based on the lexical similarity
	 * between the frame label and the query
	 * @param query
	 * @return
	 */
	public static Map<Frame,Double> getFramesByLabel(String query,boolean stem, boolean lexicalUnits){
		
		Map<Frame,Double> candidateFrameList = new HashMap<Frame,Double>();
		ValueComparatorFrame bvc =  new ValueComparatorFrame(candidateFrameList);

		List<String> allFrameList = getFrames();
		
		for(String frameId : allFrameList){
			
			Frame frame = getFrameFullData(frameId);
			
			//Label-based similarity
			
			String frameLabel = frame.getLabel().replace("_", " ");
		
			
			
		
			double labelSim = TextSimilarityUtil.calculateTermSimilarityExactMatch(frameLabel, query,stem);
			
			if(lexicalUnits){
				for(String lexicalEntry: frame.getLexicalEntries()){
					
					
					double lexSim = TextSimilarityUtil.calculateTermSimilarityExactMatch(lexicalEntry, query,true);
					
					labelSim += lexSim;
					
				}
			}
						
			if(labelSim > 0){
				candidateFrameList.put(frame,labelSim);
			}
				
		}
		 TreeMap<Frame,Double> sorted_map = new TreeMap<Frame,Double>(bvc);
		 sorted_map.putAll(candidateFrameList);
		 
		return sorted_map;
	}
	/**
	 * Get frames matching a given query.
	 * The decision on the similarity between a frame and the query is based on the lexical similarity
	 * between the frame label and the query
	 * @param query
	 * @return
	 */
	public static Map<Frame,Double> getFramesByLabel(List<String> query,boolean stem,boolean lexicalUnits){
		
		Map<Frame,Double> candidateFrameList = new HashMap<Frame,Double>();
		 ValueComparatorFrame bvc =  new ValueComparatorFrame(candidateFrameList);

		for(String queryPart: query) {
			
			candidateFrameList.putAll( getFramesByLabel(queryPart,stem,lexicalUnits));
		}
		
		 TreeMap<Frame,Double> sorted_map = new TreeMap<Frame,Double>(bvc);
		 sorted_map.putAll(candidateFrameList);

		 return candidateFrameList;
	}
	
	
	public static Map<Frame,Double> getFramesByDefinition(String query){
		
		Map<Frame,Double> candidateFrameList = new HashMap<Frame,Double>();
		 ValueComparatorFrame bvc =  new ValueComparatorFrame(candidateFrameList);

		List<String> allFrameList = getFrames();
		
		for(String frameId : allFrameList){
			
			Frame frame = getFrameFullData(frameId);
			
			//Label-based similarity
			
			String frameLabel = frame.getLabel().replace("_", " ");
			
			String frameDef = frame.getDefinition();
			
			double similarity = 0 ;
			if(frameDef == null){
				similarity = TextSimilarityUtil.calculateWNSimilarity(frameLabel, query,WordNetSimilarityMethod.Path,false);
			}
			else{
				similarity = TextSimilarityUtil.calculateTextSimilairty(frameDef, query,TextSimilarityMethod.WordNGramContainment);
			}
						
			if(similarity > 0){
				candidateFrameList.put(frame,similarity);
			}
				
		}
		 TreeMap<Frame,Double> sorted_map = new TreeMap<Frame,Double>(bvc);
		 sorted_map.putAll(candidateFrameList);
		 
		return sorted_map;
	}
	
	/**
	 * Generate VIS code for visualizing frames
	 * @param frameId
	 * @param dispalyLU
	 * @param displaySemanticArguments
	 * @param displayFrameRelations
	 * @return
	 */
	public static String generateVisCode(String frameId,boolean dispalyLU, boolean displaySemanticArguments, boolean displayFrameRelations){
		Frame f = getFrameFullData(frameId);

		
		StringBuffer strBufferNodes = new StringBuffer();
		
		StringBuffer strBufferEdges = new StringBuffer();

		 strBufferEdges.append("var edges = new vis.DataSet([\n");
		//start
		strBufferNodes.append("var nodes = new vis.DataSet([ \n");
		
		//Nodes
		
		strBufferNodes.append("{id:1").append(", label:'").append(f.getLabel()).append("',color: {background:'yellow', border:'blue'}}").append(",\n");
		
		int i = 2;
		
		if(displayFrameRelations){
			Map<String, String> relatedFrames = f.getRelatedFrames();

			for(Entry<String, String> e: relatedFrames.entrySet()){
				
				strBufferNodes.append("{id:").append(i).append(", label:'").append(getFrameLabel(e.getKey())).append("'}").append(", \n");
				strBufferEdges.append("{from:1").append(", to:").append(i).append(", label: '").append(e.getValue()).append("', arrows:'to'},\n");
				i++;
			}
		}
		
		if(displaySemanticArguments){
		List<SemanticRole> semanticRoles = f.getRoles();
		
			for(SemanticRole sr : semanticRoles){
				
				strBufferNodes.append("{id:").append(i).append(", label:'").append(sr.getRole()).append("'}").append(", \n");
				if(sr.getType().equals("core")){

					strBufferEdges.append("{from:1").append(", to:").append(i).append(", label: '").append(sr.getType()).append("', color: 'red' , arrows:'to'},\n");	
				}
				else{

					strBufferEdges.append("{from:1").append(", to:").append(i).append(", label: '").append(sr.getType()).append("', arrows:'to'},\n");
				}
				i++;
			}
		}
		if(dispalyLU){
			List<String> lus = f.getLexicalEntries();
			for(String lu : lus){
				
				strBufferNodes.append("{id:").append(i).append(", label:'").append(lu).append("'}").append(", \n");
				strBufferEdges.append("{from:1").append(", to:").append(i).append(", label: '").append("LU").append("', arrows:'to'},\n");
				i++;
			}
		}
		//End
		strBufferNodes = new StringBuffer(strBufferNodes.toString().substring(0, strBufferNodes.toString().lastIndexOf(",")));
		
		strBufferEdges = new StringBuffer(strBufferEdges.toString().substring(0, strBufferEdges.toString().lastIndexOf(",")));
		
		strBufferNodes.append("]);");
		strBufferEdges.append("]);");

		
		return strBufferNodes.toString() + "\n" + strBufferEdges;
	}
	
	
	
	public static Map<String, Double> getFEFillerHeadWords(String frameLabel, String FE){

		Map<String, Double> allHeadwords = new HashMap<String, Double>();
		
		ValueComparator bvc =  new ValueComparator(allHeadwords);

		List<String> fillerSet = getFEFillers(frameLabel, FE);
		for(String filler : fillerSet){
			
			if(filler != null){
			
				filler = filler.replace("`", "").replace("b\"", "");
				
				Map<String, List<String>> headwordMap = StanfordNLPTools.identifyHeadWord(filler);
	
				if(headwordMap.size() > 0){
					for(Entry<String, List<String>> e : headwordMap.entrySet()){
						
						List<String> headwordOfNP = e.getValue();
						
						for(String hw : headwordOfNP){
							
							Double count =  allHeadwords.get(hw);
							if(count==null){
								allHeadwords.put(hw, 1.0);
							}
							else{
								allHeadwords.put(hw, count+1);
								
							}
						}
					}
				}
				else{
					Double count =  allHeadwords.get(filler);
					if(count==null){
						allHeadwords.put(filler, 1.0);
					}
					else{
						allHeadwords.put(filler, count+1);
						
					}
	
				}
			}
		}
		

		
		TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
		
		sorted_map.putAll(allHeadwords);
		return sorted_map;
				
	}
	public static void main(String[] args) {
		
		String frameLabel  = "Birth";
		
		String FE = "Father" ;
		Map<String, Double> headWords = getFEFillerHeadWords(frameLabel, FE);
		
		System.out.println("Frame: " + frameLabel + "\t FE: " + FE);
		
		for(Entry<String, Double> e:headWords.entrySet()){
			System.out.println(e.getKey() + "\t" + e.getValue());

		}
		
//		
//		List<String> arguments = JacksonDBAPI.getClaimArguments("P40", 10);
//		for(String arg : arguments){
//			
//			String domain = arg.split("-")[0];
//			String range = arg.split("-")[1];
//			System.out.println(JacksonDBAPI.getItemLabel(domain, "en") + " : " + JacksonDBAPI.getItemLabel(range, "en"));
//		}

	}
	
}
