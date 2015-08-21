package hms.db;



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;


public class ProtegeDBConnection {

	/* H2 embedded database */
//	public static final String DB_URL =  "jdbc:h2:file:embeddedUby/ubymedium070";
//	public static final String DB_DRIVER = "org.h2.Driver";
//	public static final String DB_DRIVER_NAME = "h2";
//	public static final String DB_USERNAME = "sa";
//	public static final String DB_PASSWORD = "";
// 	

	//MySQL database 
	public static final String DB_URL =  "jdbc:mysql://localhost/protege";
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";	
	public static final String DB_DRIVER_NAME = "mysql";
	public static final String DB_USERNAME = "root";
	public static final String DB_PASSWORD = "admin";

	public static Connection conn = null;
    Statement stmt = null;
    static{
        try{
           //STEP 2: Register JDBC driver
           Class.forName("com.mysql.jdbc.Driver");

           //STEP 3: Open a connection
//           System.out.println("Connecting to database...");
           conn = DriverManager.getConnection(DB_URL,DB_USERNAME,DB_PASSWORD);
        }
        catch(SQLException e){
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static PreparedStatement createPreparedStatemen(String sql){
    	
    	try {
			return conn.prepareStatement(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
	
}
