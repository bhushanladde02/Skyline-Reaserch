/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christoph
 */
public class NotebookDumpFileCreator extends DbDumpFileCreator {

    public float numerifyCPU(String value) {
        if (value.startsWith("AMD Turion 64")) {
            return new Float(0.5);
        } else if (value.startsWith("Intel Core 2 Quad")) {
            return new Float(1.0);
        } else if (value.startsWith("Intel Core 2 Duo")) {
            return new Float(0.8);
        } else if (value.startsWith("Intel Core Duo")) {
            return new Float(0.4);
        } else if (value.startsWith("Intel Core Solo")) {
            return new Float(0.1);
        } else {
            return new Float(0.0);
        }
    }

    public float numerifyMANUFACTURER(String value) {
        return new Float(0.0);
    }

    public static void main(String[] args) {
        try {
            NotebookDumpFileCreator generator = new NotebookDumpFileCreator();
            generator.setUrl("jdbc:db2://is60.idb.cs.tu-bs.de:50000/prefs").setTable("NOTEBOOKS_DOOYOO.PRODUCTS_JUL09");
            generator.setDumpfileName("data\\notebooks.csv");
            String[] columns = {"NAME", "CPU", "CPU_FREQ", "RAM_INSTALLED", "HARDDRIVE", "DISPLAY_TYPE", "WEIGHT", "MANUFACTURER"};
            generator.setColumns(columns);
            PrefType[] colPrefs = {PrefType.LABEL, PrefType.CATEGORY, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_ASC, PrefType.NUMERIC_DESC, PrefType.CATEGORY};
            generator.setColPref(colPrefs);
            generator.setFilter(null);
            //
            String user = "lofi";
            String password = null;
            if (user == null) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Please enter your DB user name: ");
                user = scanner.nextLine();
            }
            if (password == null) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Please enter your DB password: ");
                password = scanner.nextLine();
            }
            generator.setUser(user).setPassword(password);
            generator.dumpDataToFile();
        } catch (Exception ex) {
            Logger.getLogger(NotebookDumpFileCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
