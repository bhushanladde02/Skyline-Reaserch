


public class DoubleFeature extends Feature {

    private String suffix;

    public DoubleFeature(String name, int preDecimalPlaces, int decimalPlaces) {
	this(name, preDecimalPlaces, decimalPlaces, "");
    }

    public DoubleFeature(String name, int length, int decimalPlaces, String suffix) {
	super(name, length);
	this.suffix = suffix;
	type = "%" + (length - suffix.length()) + "." + decimalPlaces + "f";
    }

    @Override
    public String formatValue(Object value) {
	if (value instanceof Double) {
	    return String.format(type, value) + suffix;
	} else {
	    return super.formatValue(value);
	}
    }

    @Override
    public String formatValuePlain(Object value) {
	if (value instanceof Double) {
	    return StringUtils.repeat(' ', suffix.length()) + String.format(type, value);
	} else {
	    return super.formatValue(value);
	}
    }

    @Override
    public Object add(Object a, Object b) {
	if ((a instanceof Double) && (b instanceof Double)) {
	    return (Double)a + (Double)b;
	} else {
	    return super.add(a, b);
	}
    }

    @Override
    public Object divide(Object a, int b) {
	if (a instanceof Double) {
	    return (Double)a / b;
	} else {
	    return super.divide(a, b);
	}
    }
}
