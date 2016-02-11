package org.sonar.javascript.cfg;

import java.util.Set;

public interface ControlFlowNode {

  Set<ControlFlowNode> predecessors();

  Set<ControlFlowNode> successors();

}
