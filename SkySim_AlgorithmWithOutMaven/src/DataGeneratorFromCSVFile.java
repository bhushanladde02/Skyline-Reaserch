

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataGeneratorFromCSVFile implements DataGenerator {

    private final String shortName;
    private final int n;
    private final int d;
    private final float[] data;

    private static final String SEPARATOR = ",";

    public DataGeneratorFromCSVFile(String filename) {
	BufferedReader reader = null;
	String line = null;
	StringTokenizer st;
	//System.out.println("In Main method.........");
	try {
	    reader = new BufferedReader(new FileReader(filename));
	   // System.out.println(reader);
	} catch (FileNotFoundException ex) {
	    Logger.getLogger(DataGeneratorFromCSVFile.class.getName()).log(Level.SEVERE, null, ex);
	}

	// Read header (d, n, and shortName).
	try {
	    line = reader.readLine();
	   // System.out.println(line);
	} catch (IOException ex) {
	    Logger.getLogger(DataGeneratorFromCSVFile.class.getName()).log(Level.SEVERE, null, ex);
	}
	st = new StringTokenizer(line, SEPARATOR);
	//System.out.println(st);
	d = Integer.parseInt(st.nextToken());
	n = Integer.parseInt(st.nextToken());
	//System.out.println("d:"+d);
    //System.out.println("n:"+n);
	shortName = st.nextToken();
	//System.out.println(shortName);
     
	data = new float[d * n];   
	

	// Read data, skipping all comment lines.
	int pos = 0;
	try {
	    while ((line = reader.readLine()) != null) {
		if (line.startsWith("#")) {
		    // Comment line found.
		    continue;
		}
		st = new StringTokenizer(line, SEPARATOR);
		while (st.hasMoreTokens()) {
			//System.out.println("Tokens"+st.nextToken());
		    data[pos] = Float.parseFloat(st.nextToken());
		    pos++;
		}
	    }
	} catch (IOException ex) {
	    Logger.getLogger(DataGeneratorFromCSVFile.class.getName()).log(Level.SEVERE, null, ex);
	}
	try {
	    reader.close();
	} catch (IOException ex) {
	    Logger.getLogger(DataGeneratorFromCSVFile.class.getName()).log(Level.SEVERE, null, ex);
	}
    }

    @Override
    public float[] generate(int d, int n) {
	if ((d != this.d) || (n != this.n)) {
	    throw new UnsupportedOperationException("Wrong data description (d/n).");
	}
	return data;
    }

    @Override
    public float[] generate(int d, int n, int[] levels) {
	if (levels == null) {
	    return generate(d, n);
	} else {
	    throw new UnsupportedOperationException("Not supported yet.");
	}
    }

    @Override
    public void resetToDefaultSeed() {
    }

    @Override
    public float[] generate(int d, int n, int levels) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generate(int d, int n, File file, int bytesPerRecord) throws IOException {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generate(int d, int n, int[] levels, File file, int bytesPerRecord) throws IOException {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generate(int d, int n, int levels, File file, int bytesPerRecord) throws IOException {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getShortName() {
	return shortName;
    }

    @Override
    public boolean providesRandomData() {
	return false;
    }

    public int getD() {
        return d;
    }

    public int getN() {
        return n;
    }

    
}
