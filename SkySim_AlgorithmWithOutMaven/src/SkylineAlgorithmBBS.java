

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;



/*
 * R-tree-based BBS skyline algorithm
 * Papadias, Tao, Fu, Seeger: Progressive Skyline Computationin Database Systems
 */
public class SkylineAlgorithmBBS extends AbstractSkylineAlgorithm implements SkylineAlgorithm {

    private long ioCost;
    private long cpuCost;
    private long totalTimeNS;

    public SkylineAlgorithmBBS() {
    }

    @Override
    public List<float[]> compute(PointSource data) {
	long start = System.nanoTime();
	BulkLoader bl = new BulkLoaderSTR();
	RTree rtree = bl.bulkLoad(data);
	//System.out.println(rtree.getRoot());
	long stop = System.nanoTime();
	//System.out.format("Generate R-tree: %.2f s\n", (double) (stop - start) / 1000000000);

	long startTime = System.nanoTime();
	ioCost = 0;
	cpuCost = 0;

	PriorityQueueDouble<RTree.Node> heap = new PriorityQueueDouble<RTree.Node>();
	int d = data.getD();
	
	//System.out.println(d);
	List<float[]> window = new LinkedPointList(d);

	RTree.Node root = rtree.getRoot();
	heap.add(root, getMaxdist(root));
	
	
	
	while (!heap.isEmpty()) {
	
		
	    RTree.Node node = heap.remove();
	    
	    if (!node.isLeaf()) {
	    	
		// next heap entry is a node
		// check if the node's mbr is dominated by some window point
		boolean nodeIsDominated = false;
		float[] mbrMaxNode = node.getMBR().getHigh();
		ListIterator<float[]> windowIter = window.listIterator();
		while (windowIter.hasNext()) {
		    final float[] windowPoint = windowIter.next();
		    PointRelationship dom = PointComparator.compare(windowPoint, mbrMaxNode);
		   // System.out.print(" Results"+dom.toString());
		    cpuCost++;
		    if (dom == PointRelationship.DOMINATES) {
			nodeIsDominated = true;
			break;
		    }
		}
		// if so, discard node; otherwise, process node
		if (!nodeIsDominated) {
		    // add all children of node to queue
			
		    RTree.Node[] children = node.getChildren();
		    for (RTree.Node child : children) {
		    	//System.out.println("Child::::");
		    	
		    	//System.out.println(child.deepToString());
			heap.add(child, getMaxdist(child));
		    }
		}
	    } else {
	    //	System.out.println("only leaf::"+node.deepToString());
		// next heap entry is a point
		float[] point = node.getItem();
		ListIterator<float[]> windowIter2 = window.listIterator();
	//	ListIterator<float[]> check=  window.listIterator();
		boolean pointIsDominated = false;
		//-------------------------------------------------------------
		while (windowIter2.hasNext()) {
		    final float[] windowPoint = windowIter2.next();
		    PointRelationship dom = PointComparator.compare(windowPoint, point);
		    /*System.out.println("Results"+dom.toString());
		    System.out.println("Window::");
		    while(check.hasNext()){
		    	float[] windowPoint1 = check.next();
		    	System.out.println(windowPoint1[0]+"  "+windowPoint1[1]);
		    }*/
		    cpuCost++;
		    if (dom == PointRelationship.DOMINATES ) {
			pointIsDominated = true;
			break;
		    }
		  //  System.out.println("_----------------------------------------------------------");
		}
		if (!pointIsDominated) {
		    window.add(point);
		}
	    }
	}
	totalTimeNS = System.nanoTime() - startTime;
	return window;
    }

    private static double getMaxdist(RTree.Node node) {
	double maxdist = 0;
	int d = node.getD();
	float[] mbrMax = node.getMBR().getHigh();
	for (int i = 0; i < d; i++) {
	    maxdist += 1 - mbrMax[i];
	}
	return maxdist;
    }

    @Override
    public long getIOcost() {
	return ioCost;
    }

    @Override
    public long getCPUcost() {
	return cpuCost;
    }

    @Override
    public long getTotalTimeNS() {
	return totalTimeNS;
    }

    @Override
    public long getReorgTimeNS() {
	return 0;
    }

    @Override
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("BBS");
	return buf.toString();
    }

    @Override
    public String getShortName() {
	StringBuffer sb = new StringBuffer("BBS");
	return sb.toString();
    }
    
    public static void main(String args[])
    {
    	PointSource data=new LinkedPointList(13);
    	float a[]={1.0f,9.0f,0.0f,0.0f};
    	float b[]={2.0f,10.0f,0.0f,0.0f};
    	float c[]={4.0f,8.0f,0.0f,0.0f};
    	float d[]={6.0f,7.0f,0.0f,0.0f};
    	float e[]={9.0f,10.0f,0.0f,0.0f};
    	float f[]={7.0f,5.0f,0.0f,0.0f};
    	float g[]={5.0f,6.0f,0.0f,0.0f};
    	float h[]={4.0f,3.0f,0.0f,0.0f};
    	float i[]={3.0f,2.0f,0.0f,0.0f};
    	float j[]={10.0f,4.0f,0.0f,0.0f};
    	float k[]={9.0f,1.0f,0.0f,0.0f};
    	float l[]={6.0f,2.0f,0.0f,0.0f};
    	float m[]={8.0f,3.0f,0.0f,0.0f};
    	//float n[]={14.0f,5.0f,0.0f,0.0f};
    	
    	//float n[]={10.0f,10.0f,0.0f,0.0f};
    	
    	/*
    	float x[]={1.0f,2.0f,4.0f,6.0f,9.0f,7.0f,5.0f,4.0f,3.0f,10.0f,9.0f,6.0f,8.0f};
    	float y[]={9.0f,10.0f,8.0f,7.0f,10.0f,5.0f,6.0f,3.0f,2.0f,4.0f,1.0f,2.0f,3.0f};
    	data.add(x);
    	data.add(y);
    	*/
    	
    	/*
    	float a[]={1.0f,9.0f,1.0f};
    	float b[]={2.0f,10.0f,1.0f};
    	float c[]={4.0f,8.0f,1.0f};
    	float d[]={6.0f,7.0f,1.0f};
    	float e[]={9.0f,10.0f,1.0f};
    	float f[]={7.0f,5.0f,1.0f};
    	float g[]={5.0f,6.0f,1.0f};
    	float h[]={4.0f,3.0f,1.0f};
    	float i[]={3.0f,2.0f,1.0f};
    	float j[]={10.0f,4.0f,1.0f};
    	float k[]={9.0f,1.0f,1.0f};
    	float l[]={6.0f,2.0f,1.0f};
    	float m[]={8.0f,3.0f,1.0f};
    	*/    	
    	/*	
     	float a[]={9.0f,1.0f,1.0f};
    	float b[]={10.0f,2.0f,1.0f};
    	float c[]={8.0f,4.0f,1.0f};
    	float d[]={7.0f,6.0f,1.0f};
    	float e[]={10.0f,9.0f,1.0f};
    	float f[]={5.0f,7.0f,1.0f};
    	float g[]={6.0f,5.0f,1.0f};
    	float h[]={3.0f,4.0f,1.0f};
    	float i[]={2.0f,3.0f,1.0f};
    	float j[]={4.0f,10.0f,1.0f};
    	float k[]={1.0f,9.0f,1.0f};
    	float l[]={2.0f,6.0f,1.0f};
    	float m[]={3.0f,8.0f,1.0f};
    	*/
    	
    	data.add(a);
    	data.add(b);
    	data.add(c);
    	data.add(d);
    	data.add(e);
    	data.add(f);
    	data.add(g);
    	data.add(h);
    	data.add(i);
    	data.add(j);
    	data.add(k);
    	data.add(l);
    	data.add(m);
    	data.setD(3);
      // 	data.add(n);
        
    	
    	SkylineAlgorithmBBS skyline=new SkylineAlgorithmBBS();
    	List<float[]> result=skyline.compute(data);
        Iterator itr=result.iterator();
        while(itr.hasNext())
        {
        	float p[]=(float[])itr.next();
        	System.out.println(p[0]+","+p[1]);	        	
        	System.out.println("----------------------------------");
            System.out.println();	
        }
        	
      
    }
    
}