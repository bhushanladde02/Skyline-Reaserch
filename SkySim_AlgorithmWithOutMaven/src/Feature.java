

public abstract class Feature {
    protected String type;
    protected int length;
    protected String name;

    protected Feature(String name, int length) {
	this.name = name;
	this.length = length;
    }

    public int getLength() {
	return length;
    }

    public String getName() {
	return name;
    }

    public String formatValue(Object value) {
	throw new IllegalArgumentException("Data type '" + value.getClass() + "' not supported.");
    }

    public String formatValuePlain(Object value) {
	throw new IllegalArgumentException("Data type '" + value.getClass() + "' not supported.");
    }

    public Object add(Object a, Object b) {
	if (a == null) {
	    return b;
	} else {
	    throw new IllegalArgumentException("Data types '" + a.getClass() + "' and '" + b.getClass() + "' not supported.");
	}
    }

    public Object divide(Object a, int b) {
	throw new IllegalArgumentException("Data type '" + a.getClass() + "' not supported.");
    }
}
