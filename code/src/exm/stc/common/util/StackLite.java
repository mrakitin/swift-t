package exm.stc.common.util;

import java.util.ArrayList;

/**
 * Lightweight wrapper around ArrayList
 * @param <T>
 */
public class StackLite<T> extends ArrayList<T> {

  private static final long serialVersionUID = 1L;

  public void push(T o) {
    this.add(o);
  }
  
  public T pop() {
    return this.remove(this.size() - 1);
  }

  public T peek() {
    return this.get(this.size() - 1);
  }
}
