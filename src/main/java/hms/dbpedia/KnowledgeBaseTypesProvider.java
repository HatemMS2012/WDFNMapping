package hms.dbpedia;

import hms.db.ProtegeDBConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KnowledgeBaseTypesProvider {

	public static final String DOMAIN_RANGE_SEP = "_";
	private static final String SELECT_PROPERTY_DOMAIN_RANGE_DBPedia = "SELECT   domain, dbpedia_properties.range as \"range\" FROM protege.dbpedia_properties where property_url = ?";
	private static final String SELECT_PROPERTY_DOMAIN_RANGE_SCHEMA_ORG = "SELECT  domains as domain, ranges as \"range\" FROM protege.schema_org where id = ?";

	public static String getProeprtyDomainAndRange(String propertyLabel, KnowledgeBaseName knowledgeBabse) {

		String result = null;

		PreparedStatement st = null;
		
		if(knowledgeBabse.equals(KnowledgeBaseName.DBpedia)){
			st = ProtegeDBConnection.createPreparedStatemen(SELECT_PROPERTY_DOMAIN_RANGE_DBPedia);
		}
		else if(knowledgeBabse.equals(KnowledgeBaseName.SchemaOrg)){
			st = ProtegeDBConnection.createPreparedStatemen(SELECT_PROPERTY_DOMAIN_RANGE_SCHEMA_ORG);
		}

		try {
			st.setString(1, propertyLabel);

			ResultSet res = st.executeQuery();

			if (res.next()) {

				String domain = res.getString("domain");
				String range = res.getString("range");
				result = domain + DOMAIN_RANGE_SEP + range;
			}

			res.close();
			st.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
	
	public static void main(String[] args) {
		System.out.println(getProeprtyDomainAndRange("deathDate", KnowledgeBaseName.SchemaOrg));
	}

}
