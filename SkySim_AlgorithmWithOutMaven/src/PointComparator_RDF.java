/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author Christoph
 */
public interface PointComparator_RDF {

    public PointRelationship compare(final float[] pointA, final float[] pointB);

    public void setDelta(float delta);
}
