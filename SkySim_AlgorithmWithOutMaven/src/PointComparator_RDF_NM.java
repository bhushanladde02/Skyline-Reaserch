


public final class PointComparator_RDF_NM implements PointComparator_RDF {

    private float delta = 0.2f;

    public PointComparator_RDF_NM(float delta) {
        this.delta = delta;
    }

    // compares two data points a and b
    // returns DOMINATES if a > b
    // returns IS_DOMINATED_BY if a < b
    // returns IS_INCOMPARABLE_TO if a <> b
    // returns EQUALS if a == b
    public final PointRelationship compare(final float[] pointA, final float[] pointB) {

        // Equals
        int i = pointA.length;
        // equalas
        outerloop:
        while (--i >= 0) {
            if (pointA[i] != pointB[i]) {
                break outerloop;
            }
        }
        if (i < 0) {
            return PointRelationship.EQUALS;
        }

        // dominance
        i = pointA.length - 1;
        PointRelationship result = null;

        // first phase: check for overall pareto dominance
        outerif:
        if (pointA[i] >= pointB[i]) {
            while (--i >= 0) {
                if (pointA[i] < pointB[i]) {
                    result = PointRelationship.IS_INCOMPARABLE_TO;
                    break outerif;
                }
            }
            return PointRelationship.DOMINATES;
        } else {
            while (--i >= 0) {
                if (pointA[i] > pointB[i]) {
                    result = PointRelationship.IS_INCOMPARABLE_TO;
                    break outerif;
                }
            }
            return PointRelationship.IS_DOMINATED_BY;
        }
        // if it was incomparable, check if we get a malleabe dominance
        // 
        { // pareto dominance for first part
            i = (pointA.length / 2) - 1;
            result = null;
            outer2if:
            if (pointA[i] >= pointB[i]) {
                while (--i >= 0) {
                    if (pointA[i] < pointB[i]) {
                        return PointRelationship.IS_INCOMPARABLE_TO;
                    }
                }
                result = PointRelationship.DOMINATES;
            } else {
                while (--i >= 0) {
                    if (pointA[i] > pointB[i]) {
                        return PointRelationship.IS_INCOMPARABLE_TO;
                    }
                }
                result = PointRelationship.IS_DOMINATED_BY;
            }
            // now, we have a data domincance
            // test for malleability domaincane
            i = (pointA.length - 1);
            int u = (pointA.length / 2);
            if (result.equals(PointRelationship.DOMINATES)) {
                outer2if:
                if (pointA[i] + delta >= pointB[i]) {
                    while (--i >= u) {
                        if (pointA[i] + delta < pointB[i]) {
                            return PointRelationship.IS_INCOMPARABLE_TO;

                        }
                    }
                    return PointRelationship.DOMINATES;
                }
            }
            if (result.equals(PointRelationship.IS_DOMINATED_BY)) {
                outer2if:
                if (pointB[i] + delta >= pointA[i]) {
                    while (--i >= u) {
                        if (pointB[i] + delta < pointA[i]) {
                            return PointRelationship.IS_INCOMPARABLE_TO;

                        }
                    }
                    return PointRelationship.IS_DOMINATED_BY;
                }
            }

            return PointRelationship.IS_INCOMPARABLE_TO;
        }
    }

    @Override
    public String toString() {
        return "Comparator_RDF_NM(delta=" + delta + ")";
    }

    @Override
    public void setDelta(float delta) {
        this.delta = delta;
    }
}
