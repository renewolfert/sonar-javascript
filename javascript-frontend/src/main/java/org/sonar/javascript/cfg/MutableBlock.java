package org.sonar.javascript.cfg;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.Tree;

class MutableBlock {
  
  private final Set<MutableBlock> successors = new HashSet<>();
  private final List<Tree> elements = new ArrayList<>();

  public Set<MutableBlock> successors() {
    return successors;
  }

  public void addSuccessor(MutableBlock successor) {
    successors.add(successor);
  }
  
  public List<Tree> elements() {
    return Lists.reverse(elements);
  }

  public void addElement(Tree element) {
    elements.add(element);
  }

  public boolean isEmpty() {
    return elements.isEmpty();
  }

}
