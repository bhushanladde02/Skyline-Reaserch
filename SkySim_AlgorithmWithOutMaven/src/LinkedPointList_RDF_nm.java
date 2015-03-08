

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/*
 * For performance reasons (class inheritance comes with a cost),
 * this class is a near-copy of
 * ifis.skysim2.common.datastructures.SimpleLinkedList<float[]>.
 */
public class LinkedPointList_RDF_nm implements PointSource, PointList {

    private  int d;
    private final DataNode head;
    private DataNode last;
    private final DataNode tail;
    private int size = 0;
    private final ListOrder listOrder;
    private PointComparator_RDF comparator;

    public LinkedPointList_RDF_nm(int d, PointComparator_RDF comparator) {
        this(d, ListOrder.Unsorted, comparator);
    }

    public LinkedPointList_RDF_nm(int d, ListOrder listOrder, PointComparator_RDF comparator) {
        tail = new DataNode(new float[0], Float.NEGATIVE_INFINITY, null);
        head = new DataNode(new float[0], Float.POSITIVE_INFINITY, tail);
        last = head;
        this.d = d;
        this.listOrder = listOrder;
        this.comparator = comparator;
    }

    @Override
    public boolean add(float[] data) {
        return addDirect(data);
    }

    @Override
    public boolean addDirect(float[] data) {
        // new node will be inserted between pred and curr
        DataNode pred;
        DataNode curr;
        float key = 0;
        switch (listOrder) {
            case SortedByVolume:
                key = PointComputations.getVolume(data);
                // find insertion point, i.e. find the first node with key < volume
                pred = head;
                curr = pred.next;
                while (curr.key > key) {
                    pred = curr;
                    curr = curr.next;
                }
                break;
            default:
                pred = last;
                curr = tail;
                break;
        }
        final DataNode newNode = new DataNode(data, key, curr);
        pred.next = newNode;

        if (pred == last) {
            last = newNode;
        }
        size++;
        return true;
    }

    public boolean addDirectMarkedAsDeleted(float[] data) {
        // new node will be inserted between pred and curr
        DataNode pred;
        DataNode curr;
        float key = 0;
        switch (listOrder) {
            case SortedByVolume:
                key = PointComputations.getVolume(data);
                // find insertion point, i.e. find the first node with key < volume
                pred = head;
                curr = pred.next;
                while (curr.key > key) {
                    pred = curr;
                    curr = curr.next;
                }
                break;
            default:
                pred = last;
                curr = tail;
                break;
        }
        final DataNode newNode = new DataNode(data, key, curr);
        newNode.isDeleted = true;
        pred.next = newNode;

        if (pred == last) {
            last = newNode;
        }
        size++;
        return true;
    }

    @Override
    public void add(int index, float[] data) {
        addDirect(index, data);
    }

    @Override
    public void addDirect(int index, float[] data) {
        if (index != 0) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            addDirect(data);
        }
    }

    @Override
    public float[] remove(int index) {
        if (index != 0) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            DataNode first = head.next;
            if (first == last) {
                last = head;
            }
            head.next = first.next;
            size--;
            return first.data;
        }
    }

    @Override
    public boolean addAll(Collection<? extends float[]> c) {
        if (!(c instanceof LinkedPointList_RDF_nm)) {
            for (float[] p : c) {
                if (p.length == d) {
                    add(p);
                }
            }
            return true;
        } else {
            LinkedPointList_RDF_nm ll = (LinkedPointList_RDF_nm) c;
            PointListIterator pllIter = ll.listIterator();
            while (pllIter.hasNext()) {
                add(pllIter.next());
            }
            return true;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public PointListIterator listIterator() {
        return new LinkedPointListIterator();
    }

    @Override
    public PointListIterator listIterator(float[] referencePoint) {
        return new LinkedPointListIterator(referencePoint);
    }

    @Override
    public Iterator<float[]> iterator() {
        return listIterator();
    }

    @Override
    public float[] getDirect(int index) {
        if (index == 0) {
            return head.next.data;
        } else {
            Iterator<float[]> iterator = this.iterator();
            float[] result = iterator.next();
            for (int i = 0; i < index; i++) {
                result = iterator.next();
            }
            return result;
        }
    }

    @Override
    public float[] get(int index) {
        float[] point = getDirect(index);
        return Arrays.copyOf(point, d);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public float[] toFlatArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Steals a given number of elements from the head of the victim list and
     * add them to the tail of the current list.. Simply
     * steals all elements when you try to steal more than there is available
     * (no exceptions thrown).
     * @param victim the list from where to steal elements
     * @param elementsToMove the number of elements to steal
     */
    public void stealFromList(LinkedPointList_RDF_nm victim, int elementsToMove) {
        int num = Math.min(victim.size(), elementsToMove);
        if (num > 0) {
            DataNode vCurr = victim.head;
            for (int i = 0; i < num; i++) {
                vCurr = vCurr.next;
            }
            // vCurr now points to the num-th node of the victim list
            // append all points before (and including vCurr) to this
            DataNode vFirst = victim.head.next;
            DataNode vNext = vCurr.next;
            if (num > 0) {
                last.next = vFirst;
                vCurr.next = tail;
                last = vCurr;
                //
                victim.head.next = vNext;
                //
                size += num;
                victim.size -= num;
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("[[  ");
        DataNode curr = head;
        while (curr.next != tail) {
            curr = curr.next;
//	    result.append(cursor.prev.hashCode());
//	    result.append("/");
            result.append(curr.hashCode());
            result.append(" (" + curr.key + ")");
//	    result.append("/");
//	    result.append(cursor.next.hashCode());
            if (curr.next != tail) {
                result.append("  ||  ");
            }
        }
        result.append("  ]]");
        return result.toString();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addAll(int index, Collection<? extends float[]> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float[] set(int index, float[] element) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<float[]> listIterator(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<float[]> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // swaps the two nodes immediately after node
    private void swapNeighboringNodes(DataNode node) {
        DataNode node1 = node.next;
        DataNode node2 = node1.next;
        DataNode node3 = node2.next;
        node.next = node2;
        node2.next = node1;
        node1.next = node3;
        if (node2 == last) {
            last = node1;
        }
    }

    protected static class DataNode {

        private final float[] data;
        private float key;
        private DataNode next;
        private boolean isNewInLoosers = false;
        private boolean isDeleted = false;
        private boolean isInSkyline = false;
        private boolean isInLoosers = false;
        private boolean isInTest = true;

        private DataNode(final float[] point, final float key, final DataNode next) {
            // Make a copy for better memory locality
            data = Arrays.copyOf(point, point.length);
            this.key = key;
            this.next = next;
        }
    }

    protected class LinkedPointListIterator implements PointListIterator {

        private DataNode predpred; // needed for BubbleUpSimple
        private DataNode pred;
        private DataNode curr = head;
        private final float[] referencePoint;

        public LinkedPointListIterator() {
            this(null);
        }

        public LinkedPointListIterator(float[] referencePoint) {
            this.referencePoint = referencePoint;
        }

        @Override
        public boolean hasNext() {
            return curr.next != tail;
        }

        @Override
        public float[] next() {
            return Arrays.copyOf(nextDirect(), d);
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         */
        public float[] nextInTestButNotThis(DataNode notThis) {
            outerloop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInTest && curr != notThis) {
                    return curr.data;
                }
            }
            return null;
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         */
        public float[] nextInTestButNotThisAndNoLooser(DataNode notThis) {
            outerloop:
            loop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInTest) {
                    if (curr.isNewInLoosers) {
                        curr.isNewInLoosers = false;
                        continue loop;
                    }
                    if (!curr.isInLoosers && curr != notThis) {
                        return curr.data;
                    }
                }
            }
            return null;
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         * Skips those new in loosers, and makes them old
         */
        public float[] nextInTestButNotThisAndLooser(DataNode notThis) {
            outerloop:
            loop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInTest && (curr.isInLoosers) && curr != notThis) {
                    if (curr.isNewInLoosers) {
                        curr.isNewInLoosers = false;
                        continue loop;
                    }
                    return curr.data;
                }
            }
            return null;
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         */
        public float[] nextInSkyline() {
            outerloop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInSkyline) {
                    return curr.data;
                }
            }
            return null;
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         */
        public float[] nextInTest() {
            outerloop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInTest) {
                    return curr.data;
                }
            }
            return null;
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         */
        public float[] nextInTestMloosers() {
            outerloop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInTest && !curr.isInLoosers) {
                    return curr.data;
                }
            }
            return null;
        }

        /**
         * May return null if there is no next. Do not use hasNext to test for next available...!
         */
        public float[] nextInLoosers() {
            outerloop:
            while (hasNext()) {
                stepCursor();
                if (curr.isInLoosers) {
                    return curr.data;
                }
            }
            return null;
        }

        @Override
        public float[] nextDirect() {
            stepCursor();
            return curr.data;
        }

        /*
         * curr = curr.next
         */
        private void stepCursor() {
            predpred = pred;
            pred = curr;
            curr = curr.next;
            if (listOrder == ListOrder.BubbleUp) {
                // swap curr and curr.next if the latter has a larger key
                DataNode next = curr.next;
                if (curr.key < next.key) {
                    swapNeighboringNodes(pred);
                    curr = next;
                }
            }
        }

        @Override
        public void remove() {
            pred.next = curr.next;
            if (curr == last) {
                last = pred;
            }
            curr = pred;
            size--;
        }

        /**
         * 
         * @return returns true if node was not deleted before; if it was already deleted, returns false
         */
        public boolean markAsDeleted() {
            if (curr.isDeleted) {
                return false;
            }
            curr.isDeleted = true;
            return true;
        }

        public boolean isDeleted() {
            return curr.isDeleted;
        }

        public void setInLoosers(boolean looser) {
            curr.isInLoosers = looser;
        }

        public void setNewInLoosers(boolean looser) {
            curr.isNewInLoosers = looser;
        }

        public boolean isInLoosers() {
            return curr.isInLoosers;
        }

        public boolean isInSkyline() {
            return curr.isInSkyline;
        }

        public void setInSkyline(boolean skyline) {
            curr.isInSkyline = skyline;
        }

        public void setInTest(boolean compare) {
            curr.isInTest = compare;
        }

        public boolean isInTest() {
            return curr.isInTest;
        }

        protected DataNode getDataNode() {
            return curr;
        }

        @Override
        public void moveToFront() {
            if (curr != head.next) {
                DataNode newFirst = curr;
                remove();
                size++;
                newFirst.next = head.next;
                head.next = newFirst;
            }
        }

        @Override
        public void promotePoint() {
            switch (listOrder) {
                case MoveToFront:
                    moveToFront();
                    break;
                case BubbleUp:
                    curr.key++;
                    break;
                case BubbleUpSimple:
                    // move cursor node one node to the left
                    if (predpred != null) {
                        swapNeighboringNodes(predpred);
                    }
                    break;
            }
        }

        @Override
        public PointRelationship nextAndCompareNextTo(float[] point) {
            stepCursor();
            return comparator.compare(curr.data, point);
        }

        @Override
        public PointRelationship nextAndCompareNextToReferencePoint() {
            stepCursor();
            return comparator.compare(curr.data, referencePoint);
        }

        @Override
        public boolean hasPrevious() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float[] previous() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int nextIndex() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void set(float[] e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void add(float[] e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

	@Override
	public void setD(int d) {
		// TODO Auto-generated method stub
		this.d=d;
		
	}
}
