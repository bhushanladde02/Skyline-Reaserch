

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reporter {
    private static final String CALIBRATION = "CALIBRATION";

    private String header;
    private String gnuplotHeader;
    private String divider;

    private String shortName;
    private final int numberOfTrials;
    private int trialsReported = 0;

    private int widthOfFirstColumn;

    private final List<Feature> features;
    private final Map<Feature, Object> sums = new HashMap<Feature, Object>();

    public Reporter(String shortName, int numberOfTrails, List<Feature> features) {
	this.shortName = shortName;
	this.numberOfTrials = numberOfTrails;
	this.features = features;
	for (Feature f : features) {
	    sums.put(f, null);
	}
	setup();
    }

    public String getHeader() {
	return String.format("%s%n", header);
    }

    public String getGnuplotHeader() {
	return String.format("%s%n", gnuplotHeader);
    }

    public String getDivider() {
	return String.format("%s%n", divider);
    }

    private String generateEntry(String name, Map<Feature, Object> report) {
	String reportStr = String.format("| %" + widthOfFirstColumn + "s", name);
	for (Feature f : features) {
	    reportStr += " | ";
	    if (report.containsKey(f)) {
		reportStr += f.formatValue(report.get(f));
	    } else {
		throw new IllegalArgumentException("Missing feature in report: '" + f.getName() + "'.");
	    }
	}
	reportStr += String.format(" |%n");
	return reportStr;
    }

    public String reportCalibration(Map<Feature, Object> report) {
	return generateEntry(CALIBRATION, report);
    }

    public String reportTrial(Map<Feature, Object> report) {
	trialsReported++;
	String name = trialsReported + " / " + numberOfTrials;
	String reportStr = generateEntry(name, report);
	for (Feature f : features) {
	    if (report.containsKey(f)) {
		Object oldSum = sums.get(f);
		Object newSum = f.add(oldSum, report.get(f));
		sums.put(f, newSum);
	    } else {
		throw new IllegalArgumentException("Missing feature in report: '" + f.getName() + "'.");
	    }
	}
	return reportStr;
    }

    public String getAverages() {
	if (trialsReported != numberOfTrials) {
	    throw new IllegalStateException("Not all trials have been reported yet.");
	} else {
	    String avgStr = String.format("| %" + widthOfFirstColumn + "s", shortName);
	    for (Feature f : features) {
		avgStr += " | ";
		Object sum = sums.get(f);
		Object avg = f.divide(sum, trialsReported);
		avgStr += f.formatValue(avg);
	    }
	    avgStr += String.format(" |%n");
	    return avgStr;
	}
    }

    public String getGnuplotAverages() {
	if (trialsReported != numberOfTrials) {
	    throw new IllegalStateException("Not all trials have been reported yet.");
	} else {
	    String avgStr = String.format("  %" + widthOfFirstColumn + "s", shortName);
	    for (Feature f : features) {
		avgStr += "   ";
		Object sum = sums.get(f);
		Object avg = f.divide(sum, trialsReported);
		avgStr += f.formatValuePlain(avg);
	    }
	    avgStr += String.format("%n");
	    return avgStr;
	}
    }

    private void setup() {
	widthOfFirstColumn = Math.max(Math.max(2 * Integer.toString(numberOfTrials).length() + 3, shortName.length()), CALIBRATION.length());
	header = String.format("| %" + widthOfFirstColumn + "s", "");
	gnuplotHeader = String.format("# %" + widthOfFirstColumn + "s", "");
	divider = "+-" + StringUtils.repeat('-', widthOfFirstColumn);
	for (Feature f : features) {
	    header += String.format(" | %" + f.getLength() + "s", f.getName());
	    gnuplotHeader += String.format("   %" + f.getLength() + "s", f.getName());
	    divider += "-+-" + StringUtils.repeat('-', f.getLength());
	}
	header += " |";
	divider += "-+";
    }
}
