

import java.util.EmptyStackException;

import cern.colt.list.IntArrayList;

public class ArrayListIntStack<E> {
    private IntArrayList stack = new IntArrayList();

    public void push(int obj) {
	stack.add(obj);
    }

    public int pop() {
	if (stack.isEmpty()) {
	    throw new EmptyStackException();
	} else {
	    int result = stack.get(stack.size() - 1);
	    stack.remove(stack.size() - 1);
	    return result;
	}
    }

    public boolean isEmpty() {
	return stack.isEmpty();
    }

    public int size() {
	return stack.size();
    }
}
