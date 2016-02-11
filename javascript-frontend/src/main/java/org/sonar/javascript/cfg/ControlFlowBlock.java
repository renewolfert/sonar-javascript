package org.sonar.javascript.cfg;

import java.util.List;
import org.sonar.plugins.javascript.api.tree.Tree;

public interface ControlFlowBlock extends ControlFlowNode {

  List<Tree> elements();

}

