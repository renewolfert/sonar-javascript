package org.sonar.javascript.cfg;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
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
    private final Set<ControlFlowBlock> nextPredecessors = new HashSet<>();
    private ControlFlowBlock currentBlock = null;

    public ControlFlowGraph build() {
      return new ControlFlowGraph(blocks, ImmutableSet.<ControlFlowNode>copyOf(nextPredecessors));
    }

    @Override
    public void visitScript(ScriptTree tree) {
      super.visitScript(tree);

      if (currentBlock != null) {
        nextPredecessors.add(currentBlock);
      }
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      if (currentBlock == null) {
        currentBlock = createBlock();
      }
      ControlFlowBlock thenBlock = createBlock();
      currentBlock.addSuccessor(thenBlock);
      nextPredecessors.add(thenBlock);
      if (tree.elseClause() != null) {
        ControlFlowBlock elseBlock = createBlock();
        currentBlock.addSuccessor(elseBlock);
        nextPredecessors.add(elseBlock);
      } else {
        nextPredecessors.add(currentBlock);
      }
      super.visitIfStatement(tree);
      currentBlock = null;
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      if (currentBlock == null) {
        currentBlock = createBlock();
        for (ControlFlowBlock predecessor : nextPredecessors) {
          predecessor.addSuccessor(currentBlock);
        }
        nextPredecessors.clear();
        nextPredecessors.add(currentBlock);
      }

      super.visitExpressionStatement(tree);
    }

    private ControlFlowBlock createBlock() {
      ControlFlowBlock block = new ControlFlowBlock();
      blocks.add(block);
      return block;
    }

  }

}
