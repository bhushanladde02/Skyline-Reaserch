/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christoph
 */
public class DbDumpFileCreator {

    // full JDBC URL
    public enum PrefType {

        LABEL, NUMERIC_ASC, NUMERIC_DESC, CATEGORY
    }
    private String url = "jdbc:db2://is60.idb.cs.tu-bs.de:50000/prefs";
    // name of desired columns (must be numeric!)
    private String[] columns = {"gp", "pts", "reb", "asts", "fgm", "ftm"};
    // preferences for the columns
    private PrefType[] colPref = {PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC};
    // the desired full qualified table name
    private String table = "nba.player_regular_season";
    // additional filter which is inserted into where clause
    private String filter = "NOT team='TOT'";
    // the filename of the dump file
    private String dumpfileName = "nba_dump.txt";
    private boolean notNullFilter = true;
    // max values for columns
    private float[] max;
    private float[] min;
    private String user;
    private String password;
    static Connection connection;

    public Connection getConnection() throws SQLException {
        if (connection == null) {
            connection = createNewConnection(user, password, url);
        }
        return connection;
    }

    public static Connection createNewConnection(String user, String password, String url) throws SQLException {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("retrieveMessagesFromServerOnGetMessage", "true");
        Connection connection = DriverManager.getConnection(url, properties);
        return connection;
    }

    /**
     * The SQL string for returning all tuples (max=false) or the max values of all colums (max=true).
     * @param max see above
     * @return a string
     */
    private String buildSQL(boolean max) {
        StringBuffer sql = new StringBuffer("SELECT ");
        // SELECT
        for (int i = 0; i < columns.length; i++) {
            if (!max) {
                sql.append(columns[i]);
            } else {
                sql.append("max(").append(columns[i]).append(")");
                sql.append(", min(").append(columns[i]).append(")");
            }
            if (i < columns.length - 1) {
                sql.append(',');
            }
        }
        // FROM
        sql.append(" FROM ").append(table);
        String filterString = getFilterString();
        if (filterString.length() > 0) {
            sql.append(" WHERE ").append(filterString);
        }
        Logger.getLogger(DbDumpFileCreator.class.getName()).finest("Using SQL : " + sql.toString());
        return sql.toString();
    }

    public String getFilterString() {
        StringBuffer sql = new StringBuffer();
        // WHERE
        if (notNullFilter) {
            StringBuffer nnFilter = new StringBuffer();
            for (int i = 0; i < columns.length; i++) {
                nnFilter.append("(").append(columns[i]);
                nnFilter.append(" IS NOT NULL)");
                if (i < columns.length - 1) {
                    nnFilter.append(" AND ");
                }
            }
            sql.append(nnFilter.toString());
        }
        if (filter != null && filter.length() > 0) {
            if (notNullFilter) {
                sql.append(" AND");
            }
            sql.append(" " + filter);
        }
        return sql.toString();
    }

    /**
     * Returns all maximum values for the desired colums.
     * @return array containing max values
     */
    private void getMaxMinValues(Statement stmt) throws SQLException {
        max = new float[columns.length];
        min = new float[columns.length];
        ResultSet rs = stmt.executeQuery(buildSQL(true));
        // iterate over all result tuples
        while (rs.next()) {
            // for each column, do:
            for (int i = 0; i < columns.length; i++) {
                if (colPref[i].equals(PrefType.NUMERIC_ASC) || colPref[i].equals(PrefType.NUMERIC_DESC)) {
                    max[i] = rs.getFloat((i * 2) + 1);
                    min[i] = rs.getFloat((i * 2) + 2);
                } else {
                    max[i] = Float.MAX_VALUE;
                    min[i] = Float.MAX_VALUE;
                }
            }
        }
        rs.close();
    }

    /**
     * Writes the file header (4 lines: SQL, #columns, #rows, blank).
     * @param out 
     * @param stm
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    private void writeFileHeader(Writer out, Statement stm) throws SQLException, IOException {
        StringBuffer sql = new StringBuffer("SELECT count(*) FROM ").append(table);
        if (filter != null && filter.length() > 0) {
            sql.append(" WHERE ").append(filter);
        }
        ResultSet rs = stm.executeQuery(sql.toString());
        rs.next();
        int rows = rs.getInt(1);
        rs.close();
        //

        out.write("" + columns.length + ",");
        out.write("" + rows + ",dump\n");
        out.write("#\n# used SQL: " + buildSQL(false) + "\n#\n");
    }

    public float translate(int attributeIndex, String stringvalue) throws SQLException {
        if (max == null) {
            Statement stm = getConnection().createStatement();
            getMaxMinValues(stm);
            stm.close();
        }
        //
        float value = 0;
        if (stringvalue == null) {
            stringvalue = "0.0";
        }
        // get value depending on preference type
        if (colPref[attributeIndex].equals(PrefType.NUMERIC_ASC)) {
            value = (Float.valueOf(stringvalue) - min[attributeIndex]) / (max[attributeIndex] - min[attributeIndex]);
        } else if (colPref[attributeIndex].equals(PrefType.NUMERIC_DESC)) {
            value = (Float.valueOf(stringvalue) - min[attributeIndex]) / (max[attributeIndex] - min[attributeIndex]);
            value = 1 - value;
        } else if (colPref[attributeIndex].equals(PrefType.CATEGORY)) {
            Class[] args = {String.class};
            try {
                Method numerifiyer = this.getClass().getMethod("numerify" + columns[attributeIndex], args);
                value = (Float) numerifiyer.invoke(this, stringvalue);
            } catch (Exception ex) {
                throw new RuntimeException("You must provide a method named numerify" + columns[attributeIndex] + "(String value) in you class.", ex);
            }
        }
        return value;
    }

    public void dumpDataToFile() throws SQLException, IOException {
        // init file
        File file = new File(dumpfileName);
        FileWriter out = new FileWriter(file);

        // connect to db
        Connection conn = getConnection();
        String sql = buildSQL(false);
        System.out.println("Used SQL " + sql);
        Statement stm = conn.createStatement();
        getMaxMinValues(stm);

        // write header
        writeFileHeader(out, stm);

        // iterate over all result tuples
        ResultSet rs = stm.executeQuery(sql.toString());
        int count = 1;
        while (rs.next()) {
            // for each column, do:
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                String stringValue = rs.getString(i + 1);
                float value = translate(i, stringValue);

                out.write(String.valueOf(value));
                if (i < rs.getMetaData().getColumnCount() - 1) {
                    out.write(",");
                } else {
                    out.write('\n');
                }

            }
            count++;
        }
        //
        out.close();
        conn.close();
        Logger.getLogger(DbDumpFileCreator.class.getName()).info(count + " tuples writen to dumpfile " + file.getAbsolutePath());
    }

    public PrefType[] getColPref() {
        return colPref;
    }

    public DbDumpFileCreator setColPref(PrefType[] colPref) {
        this.colPref = colPref;
        return this;
    }

    public String[] getColumns() {
        return columns;
    }

    public DbDumpFileCreator setColumns(String[] columns) {
        this.columns = columns;
        return this;
    }

    public String getDumpfileName() {
        return dumpfileName;
    }

    public DbDumpFileCreator setDumpfileName(String dumpfileName) {
        this.dumpfileName = dumpfileName;
        return this;
    }

    public String getFilter() {
        return filter;
    }

    public DbDumpFileCreator setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public String getTable() {
        return table;
    }

    public DbDumpFileCreator setTable(String table) {
        this.table = table;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DbDumpFileCreator setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DbDumpFileCreator setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUser() {
        return user;
    }

    public DbDumpFileCreator setUser(String user) {
        this.user = user;
        return this;
    }

    public boolean isNotNullFilter() {
        return notNullFilter;
    }

    public void setNotNullFilter(boolean notNullFilter) {
        this.notNullFilter = notNullFilter;
    }

    public static void main(String[] args) {
        try {
            DbDumpFileCreator generator = new DbDumpFileCreator();
            String user = null;
            String password = null;
            if ((user == null) || (password == null)) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Please enter your DB user name: ");
                user = scanner.nextLine();
                System.out.print("Please enter your DB password: ");
                password = scanner.nextLine();
            }
            generator.setUser(user).setPassword(password);
            generator.dumpDataToFile();
        } catch (Exception ex) {
            Logger.getLogger(DbDumpFileCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
