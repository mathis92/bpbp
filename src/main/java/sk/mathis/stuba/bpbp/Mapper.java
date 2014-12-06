/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author martinhudec
 */
public class Mapper {

    private final Connection connection;
    private final String[] locationTableColumns;
    private final String[] vehicleTableColumns;

    public Mapper() throws SQLException {
        connection = DriverManager.getConnection("jdbc:mysql://bpbp.ctrgn.net/BPbp_schema", "root", "BPbp");
        connection.setAutoCommit(false);
        locationTableColumns = new String[]{"latitude", "longitude", "vehicle_id"};
        vehicleTableColumns = new String[]{"line_number"};
    }

    public void insertInto(String table){
        
    }
    
    
}
