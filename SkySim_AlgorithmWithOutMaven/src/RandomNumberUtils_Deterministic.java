import cern.jet.random.ExponentialPower;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;




/**
 * Some utility methods which might be useful during artificial data
 * generation..
 * 
 * @author Christoph
 */
public class RandomNumberUtils_Deterministic {

    /** the random engine reference */
    private static RandomEngine randomEngine;
    private int seed;

    public RandomNumberUtils_Deterministic(int seed) {
        this.seed = seed;
    }

    /**
     * Returns an implementation of a random engine (usually Marsenne Twister).
     *
     * @return a random engine
     */
    public  RandomEngine getRandomEngine() {
        if (randomEngine == null) {
            randomEngine = new MersenneTwister(seed);
        }
        return randomEngine;
    }

    /**
     * Returns a random integer between min and max.
     *
     * @param min
     *            minimal value
     * @param max
     *            maximum value
     * @return a random int
     */
    public  int getNextInt(int min, int max) {
        if (min == max) {
            return min;
        }
        return min + (Math.abs(getRandomEngine().nextInt()) % (max - min));
    }

    /**
     * Returns a random double between min and max.
     *
     * @param min
     *            minimal value
     * @param max
     *            maximum value
     * @return a random double
     */
    public  double getNextDouble(double min, double max) {
        return min + (getRandomEngine().nextDouble() * (max - min));
    }

    /**
     * Returns a random float between min and max.
     *
     * @param min
     *            minimal value
     * @param max
     *            maximum value
     * @return a random float
     */
    public  float getNextFloat(float min, float max) {
        return min + (getRandomEngine().nextFloat() * (max - min));
    }

    /**
     * Returns a new uniform distribution of given size.
     *
     * @param size
     *            the number of buckets
     * @return a new Uniform distribution
     */
    public  Uniform createUniformDistribution(double min, double max) {
        return new Uniform(min, max, getRandomEngine());
    }

    /**
     * Returns a new normal distribution
     *
     * @param mean
     *            the mean value
     * @param variance
     *            the variance
     * @return a new normal dsitribution
     */
    public  Normal createNormalDistribution(double mean, double variance) {
        return new Normal(mean, variance, getRandomEngine());
    }

    /**
     * Returns a new restricted normal distribution which is just cut off beyond
     * min and max.
     *
     * @param mean
     *            the mean value
     * @param variance
     *            the variance
     * @param min
     *            the minimal value
     * @param max
     *            the maximum value
     * @return a new normal dsitribution
     */
    public  RestrictedNormalDistribution createRestrictedNormalDistribution(
            double mean, double variance, double min, double max) {
        return new RestrictedNormalDistribution(mean, variance, min, max);
    }

    /**
     * Creates a new Restricted Zipf Distribution which is cut beyond min and
     * max.
     *
     * @param skew
     *            the skew
     * @param min
     *            min value
     * @param max
     *            max value
     * @param inverted
     *            true if inverted
     * @return A new Zipf distribution.
     */
    public  RestrictedZipfDistribution createZipfDistribution(
            double skew, int min, int max, boolean inverted) {
        return new RestrictedZipfDistribution(skew, min, max, inverted);
    }

    /**
     * Creates a new Exponential Power distribution.
     *
     * @param tau
     *            >=1
     * @return A new power law distribution.
     */
    public  ExponentialPower createExponentialPowerDistribution(double tau) {
        return new ExponentialPower(tau, getRandomEngine());
    }

    /**
     * Creates a new Exponential Power distribution which is just cut off beyonf
     * min and max.
     *
     * @param tau
     *            >=1
     * @param min
     *            min value
     * @param max
     *            max value
     * @return A new power law distribution.
     */
    public  RestrictedExponentialPowerDistribution createRestrictedExponentialPowerDistribution(
            double tau, double min, double max) {
        return new RestrictedExponentialPowerDistribution(tau, min, max);
    }
}
