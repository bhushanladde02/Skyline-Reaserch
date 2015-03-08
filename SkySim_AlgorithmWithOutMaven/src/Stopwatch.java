

public class Stopwatch {
    private long t;

    public void tic() {
	t = System.nanoTime();
    }

    public void toc() {
	toc(null, 1);
    }

    public void toc(String desc) {
	toc(desc, 1);
    }

    public void toc(String desc, int div) {
	long u = System.nanoTime();
	if ((desc == null) || (desc.isEmpty())) {
	    desc = "Time";
	}
	System.out.format("%s: %.2f ms%n", desc, (u - t) * 1e-6 / div);
    }
}
