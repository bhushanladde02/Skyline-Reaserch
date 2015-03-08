

import java.util.Arrays;



public final class PointComparator {

    // compares two data points a and b
    // returns DOMINATES if a > b
    // returns IS_DOMINATED_BY if a < b
    // returns IS_INCOMPARABLE_TO if a <> b
    // returns EQUALS if a == b
    public final static PointRelationship compare(final float[] pointA, final float[] pointB) {

  //  	System.out.println("Point A :("+pointA[0]+","+pointA[1]+") and PonintB: ("+pointB[0]+","+pointB[1]+")");
// Only needed for equality handling:
//	int i = pointA.length;
//
//	while (--i >= 0) {
//	    if (pointA[i] != pointB[i]) {
//		break;
//	    }
//	}
//
//	if (i < 0) {
//	    return PointRelationship.EQUALS;
//	}

    	
	int i = pointA.length - 1;

	if (pointA[i] >= pointB[i]) {
	    while (--i >= 0) {
		if (pointA[i] < pointB[i]) {
		    return PointRelationship.IS_INCOMPARABLE_TO;
		}
	    }
	  return PointRelationship.DOMINATES;
	   // return PointRelationship.IS_DOMINATED_BY;
	} else {
	    while (--i >= 0) {
		if (pointA[i] > pointB[i]) {
		    return PointRelationship.IS_INCOMPARABLE_TO;
		}
	    }
	    return PointRelationship.IS_DOMINATED_BY;
	   // return PointRelationship.DOMINATES;
	}
    	
    			/*if( pointA[0]==pointB[0])// Consider it as 1..
    			{
    				System.out.println("In First IF");
    				if(pointA[1]==pointB[1]) { return PointRelationship.EQUALS;}	 	 
    				if(pointA[1]<pointB[1])  { return PointRelationship.DOMINATES;} //DOMINATES
    				if(pointA[1]>pointB[1])  {return PointRelationship.IS_DOMINATED_BY;} //IS_DOMINATED_BY
    				
    			}
    			if( pointA[0]<pointB[0])// Consider it as 2..
    			{
    				System.out.println("In second IF");
    				//System.out.println("------------------------------------------------------------------");
    				if(pointA[1]==pointB[1]) { return PointRelationship.DOMINATES;}	 	 
    				if(pointA[1]<pointB[1])  { return PointRelationship.DOMINATES;}
    				if(pointA[1]>pointB[1])  { return PointRelationship.IS_INCOMPARABLE_TO;}
    			}	
    			if(pointA[0]>pointB[0])// Consider it as 3..
    			{
    				System.out.println("In Third IF");
    			//	System.out.println("------------------------------------------------------------------");
    				if(pointA[1]==pointB[1]) {return PointRelationship.IS_DOMINATED_BY;}	 	 
    				if(pointA[1]<pointB[1])  {return PointRelationship.IS_INCOMPARABLE_TO;}
    				if(pointA[1]>pointB[1])  {return PointRelationship.IS_DOMINATED_BY;}
    			}*/
    /*	if( pointA[0]==pointB[0])// Consider it as 1..
		{
			System.out.println("In First IF");
			if(pointA[1]==pointB[1]) { return PointRelationship.EQUALS;}	 	 
			if(pointA[1]<pointB[1])  { return PointRelationship.IS_DOMINATED_BY;} //DOMINATES
			if(pointA[1]>pointB[1])  {return PointRelationship.DOMINATES;} //IS_DOMINATED_BY
			
		}
		if( pointA[0]<pointB[0])// Consider it as 2..
		{
			System.out.println("In second IF");
			//System.out.println("------------------------------------------------------------------");
			if(pointA[1]==pointB[1]) { return PointRelationship.IS_DOMINATED_BY;}	 	 
			if(pointA[1]<pointB[1])  { return PointRelationship.IS_DOMINATED_BY;}
			if(pointA[1]>pointB[1])  { return PointRelationship.IS_INCOMPARABLE_TO;}
		}	
		if(pointA[0]>pointB[0])// Consider it as 3..
		{
			System.out.println("In Third IF");
		//	System.out.println("------------------------------------------------------------------");
			if(pointA[1]==pointB[1]) {return PointRelationship.DOMINATES;}	 	 
			if(pointA[1]<pointB[1])  {return PointRelationship.IS_INCOMPARABLE_TO;}
			if(pointA[1]>pointB[1])  {return PointRelationship.DOMINATES;}
		}
    		
    		
    			 return PointRelationship.IS_DOMINATED_BY;*/
    			
    	
    }

    // compares two points located in a storage array
    public final static PointRelationship compare(float[] data, int indexA, int indexB, int d) {
	// this kind of indirection does not impose any performance penalty
	return compare(data, indexA, data, indexB, d);
    }

    // Checks whether a point @p is dominated by some point in a list of @d-dimensional points,
    // which is stored as an array @data, containing @m points.
    public final static boolean isDominated(float[] p, float[] data, int m, int d) {
	// equality handling not supported yet
	dataloop:
	for (int posDataStart = m * d - 1; posDataStart >= 0; posDataStart -= d) {
	    int posData = posDataStart;
	    int posP = d - 1;
	    if (p[posP] >= data[posData]) {
		while (posP > 0) {
		    posP--;
		    posData--;
		    if (p[posP] < data[posData]) {
			continue dataloop; // incomparable
		    }
		}
		// @p dominates (or is equal)
	    } else {
		while (posP > 0) {
		    posP--;
		    posData--;
		    if (p[posP] > data[posData]) {
			continue dataloop; // incomparable
		    }
		}
		return true; // @p is dominated
	    }
	}
	return false;
    }

    // Finds some skyline point in a list of @d-dimensional points,
    // which is stored as an array @data, containing @m points.
    public final static float[] findUndominated(float[] data, int m, int d) {
	// equality handling not supported yet
	int md = m * d;
	float[] candidate = Arrays.copyOfRange(data, md - d, md);
	dataloop:
	for (int posDataStart = md - d - 1; posDataStart >= 0; posDataStart -= d) {
	    int posData = posDataStart;
	    int posCand = d - 1;
	    if (candidate[posCand] >= data[posData]) {
		while (posCand > 0) {
		    posCand--;
		    posData--;
		    if (candidate[posCand] < data[posData]) {
			continue dataloop; // incomparable
		    }
		}
		// @cand dominates
	    } else {
		while (posCand > 0) {
		    posCand--;
		    posData--;
		    if (candidate[posCand] > data[posData]) {
			continue dataloop; // incomparable
		    }
		}
		candidate = Arrays.copyOfRange(data, posDataStart - (d - 1), posDataStart + 1); // @cand is dominated
	    }
	}
	return candidate;
    }

    public final static PointRelationship compare(float[] dataA, int indexA, float[] dataB, int indexB, int d) {

// Only needed for equality handling:
//	int i = d;
//	int positionA = indexA + d;
//	int positionB = indexB + d;
//
//	while (--i >= 0) {
//	    positionA--;
//	    positionB--;
//	    if (dataA[positionA] != dataB[positionB]) {
//		break;
//	    }
//	}
//
//	if (i < 0) {
//	    return PointRelationship.EQUALS;
//	}

	int i = d - 1;
	int positionA = indexA;
	int positionB = indexB;

	if (dataA[positionA] >= dataB[positionB]) {
	    while (--i >= 0) {
		positionA++;
		positionB++;
		if (dataA[positionA] < dataB[positionB]) {
		    return PointRelationship.IS_INCOMPARABLE_TO;
		}
	    }
	    return PointRelationship.DOMINATES;
	} else {
	    while (--i >= 0) {
		positionA++;
		positionB++;
		if (dataA[positionA] > dataB[positionB]) {
		    return PointRelationship.IS_INCOMPARABLE_TO;
		}
	    }
	    return PointRelationship.IS_DOMINATED_BY;
	}
    }

    public final static PointRelationship compare(float[] data, int index, float[] point, int d) {
	return compare(data, index, point, 0, d);
    }

    /*
     * A binary vector indicating the relationship between a's and b's coordinates.
     * result's (i + 1)-th least significant bit is 1  iff  a[i] >= b[i].
     * This is my personal modified definition; it is also used in
     * Zhang's object-based space partitioning (modified such that max = good).
     */
    public static long getRegionIDOfBRelativeToAZhang(float[] a, float[] b, int d) {
	long successorship = 0;
	for (int i = d - 1; i >= 0; i--) {
	    successorship = successorship << 1;
	    if (a[i] >= b[i]) {
		successorship++;
	    }
	}
	return successorship;
    }

    public static long getRegionIDOfBRelativeToAZhang2(float[] a, float[] b, int d) {
	long successorship = 0;
	for (int i = d - 1; i >= 0; i--) {
	    successorship = successorship << 1;
	    if (a[i] > b[i]) {
		successorship++;
	    }
	}
	return successorship;
    }
}
