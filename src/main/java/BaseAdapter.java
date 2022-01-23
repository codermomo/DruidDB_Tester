import java.sql.SQLException;

public interface BaseAdapter {

    // Generate the URLs for writing and making queries, which will be used in other methods
    public void initConnect(String ip, String port, String user, String password);

    public double insertData();

    public double query1a() throws SQLException, ClassNotFoundException;
    public double query1b() throws SQLException, ClassNotFoundException;
    public double query1c() throws SQLException, ClassNotFoundException;
    public double query2() throws SQLException, ClassNotFoundException;
    public double query3() throws SQLException, ClassNotFoundException;
}
