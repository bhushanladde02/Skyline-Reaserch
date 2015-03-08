

import java.util.List;

// currently, a point source simply is a list of float[],
// which may or may not fit our needs...

public interface PointSource extends List<float[]> {
	public void setD(int d);
    public int getD();
    public float[] toFlatArray();
}
