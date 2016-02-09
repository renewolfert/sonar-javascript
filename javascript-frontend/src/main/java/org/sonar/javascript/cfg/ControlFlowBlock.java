package org.sonar.javascript.cfg;

import java.util.HashSet;
import java.util.Set;

public class ControlFlowBlock implements ControlFlowNode {

  private Set<ControlFlowNode> predecessors = new HashSet<>();
  private Set<ControlFlowNode> successors = new HashSet<>();

  @Override
  public Set<ControlFlowNode> predecessors() {
    return predecessors;
  }

  @Override
  public Set<ControlFlowNode> successors() {
    return successors;
  }

  public void addSuccessor(ControlFlowBlock successor) {
    successors.add(successor);
    successor.predecessors.add(this);
  }

}
