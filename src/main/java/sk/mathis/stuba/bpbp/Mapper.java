/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 *
 * @author martinhudec
 */
public class Mapper {

    private final Connection connection;
    private Statement stmt;
    private final String[] locationTableColumns;
    private final String[] vehicleTableColumns;
    private final String[] poiTableColumns;
    private Long generatedKey;
    //WIIOOOOMIIIIIITTTTTT

    public Mapper() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        //DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/BPbp_schema", "root", "BPbp");
        connection.setAutoCommit(false);
        locationTableColumns = new String[]{"latitude", "longitude", "accuracy", "speed", "vehicle_id"};
        poiTableColumns = new String[]{"latitude", "longitude", "accuracy"};
        vehicleTableColumns = new String[]{"line_number"};
    }
    
    private String[] getTableColums(String table) {
        String[] columns = null;

        switch (table) {
            case "location":
                columns = locationTableColumns;
                break;
            case "poi":
                columns = poiTableColumns;
                break;
            case "vehicle":
                columns = vehicleTableColumns;
                break;
        }
        return columns;
    }

     public void insertTableRow(String table, List<String> data) throws SQLException {
        int i = 1;
        String query, tableColumns[] = null;

        tableColumns = getTableColums(table);

        query = "INSERT INTO `" + table + "` (";

        for (String column : tableColumns) {

            if (i < tableColumns.length) {
                query += column + ",";
            } else {
                query += column;
            }
            i++;
        }

        query += ") VALUES ('";

        i = 1;
        for (String item : data) {
            if (i < data.size()) {
                query += item + "','";
            } else {
                query += item + "');";
            }
            i++;
        }
        System.out.println(query + "\n");
        
        stmt = connection.createStatement();
        stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
        connection.commit();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            generatedKey = generatedKeys.getLong(1);
        }
    }
     
     public ResultSet executeQuery(String query) throws SQLException {
        ResultSet rs = null;

        System.out.println(query + "\n");
        
        stmt = connection.createStatement();
        rs = stmt.executeQuery(query);
        connection.commit();

        return rs;
    }
    
    
}
