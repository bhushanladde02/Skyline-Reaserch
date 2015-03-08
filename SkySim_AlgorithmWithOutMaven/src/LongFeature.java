

public class LongFeature extends Feature {

    public LongFeature(String name, int length) {
	super(name, length);
	type = "%" + length + "d";
    }

    @Override
    public String formatValue(Object value) {
	if (value instanceof Long) {
	    return String.format(type, value);
	} else {
	    return super.formatValue(value);
	}
    }

    @Override
    public String formatValuePlain(Object value) {
	return formatValue(value);
    }

    @Override
    public Object add(Object a, Object b) {
	if ((a instanceof Long) && (b instanceof Long)) {
	    return (Long)a + (Long)b;
	} else {
	    return super.add(a, b);
	}
    }

    @Override
    public Object divide(Object a, int b) {
	if (a instanceof Long) {
	    return Math.round((double)(Long)a / b);
	} else {
	    return super.divide(a, b);
	}
    }
}
