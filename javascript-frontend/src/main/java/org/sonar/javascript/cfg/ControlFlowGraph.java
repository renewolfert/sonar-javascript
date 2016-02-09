package org.sonar.javascript.cfg;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.visitors.BaseTreeVisitor;

public class ControlFlowGraph {

  private final ControlFlowNode start;
  private final ControlFlowNode end;
  private final List<ControlFlowNode> blocks;

  private ControlFlowGraph(List<ControlFlowNode> blocks, Set<ControlFlowNode> endPredecessors) {
    this.blocks = blocks;
    this.end = new EndNode(endPredecessors);
    this.start = blocks.isEmpty() ? end : blocks.get(0);
    for (ControlFlowNode endPredecessor : endPredecessors) {
      end.predecessors().add(endPredecessor);
      endPredecessor.successors().add(end);
    }
  }

  public static ControlFlowGraph build(Tree tree) {
    ControlFlowParser controlFlowParser = new ControlFlowParser();
    tree.accept(controlFlowParser);
    return controlFlowParser.build();
  }

  public ControlFlowNode start() {
    return start;
  }

  public ControlFlowNode end() {
    return end;
  }

  public List<ControlFlowNode> blocks() {
    return blocks;
  }

  public ControlFlowNode block(int blockIndex) {
    return blocks().get(blockIndex);
  }

  private static class EndNode implements ControlFlowNode {

    private final Set<ControlFlowNode> predecessors;

    public EndNode(Set<ControlFlowNode> predecessors) {
      this.predecessors = predecessors;
    }

    @Override
    public Set<ControlFlowNode> predecessors() {
      return predecessors;
    }

    @Override
    public Set<ControlFlowNode> successors() {
      return ImmutableSet.of();
    }

  }

  private static class ControlFlowParser extends BaseTreeVisitor {

    private final List<ControlFlowNode> blocks = new ArrayList<>();
    private final Set<ControlFlowNode> endPredecessors = new HashSet<>();

    public ControlFlowGraph build() {
      return new ControlFlowGraph(blocks, endPredecessors);
    }

    @Override
    public void visitScript(ScriptTree tree) {
      if (tree.items() != null) {
        ControlFlowBlock block = new ControlFlowBlock();
        blocks.add(block);
        endPredecessors.add(block);
      }
      super.visitScript(tree);
    }

  }

}
