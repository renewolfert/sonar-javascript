package org.sonar.javascript.cfg;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;

class ControlFlowGraphBuilder {

  private final List<MutableBlock> blocks = new ArrayList<>();
  private final Set<MutableBlock> endPredecessors = new HashSet<>();
  private MutableBlock currentBlock = createBlock();

  public ControlFlowGraph createGraph(ScriptTree tree) {
    endPredecessors.add(currentBlock);
    if(tree.items() != null) {
      build(tree.items().items());
    }
    removeEmptyBlocks();
    return new ControlFlowGraph(Lists.reverse(blocks), endPredecessors);
  }

  private void removeEmptyBlocks() {
    List<MutableBlock> blocksToRemove = new ArrayList<>();
    for (MutableBlock endPredecessor : endPredecessors) {
      if (endPredecessor.isEmpty()) {
        blocksToRemove.add(endPredecessor);
      }
    }
    for (MutableBlock block : blocks) {
      boolean removed = block.successors().removeAll(blocksToRemove);
      if (removed) {
        endPredecessors.add(block);
      }
    }
    blocks.removeAll(blocksToRemove);
    endPredecessors.removeAll(blocksToRemove);
  }

  private void build(List<? extends Tree> trees) {
    for (Tree tree : Lists.reverse(trees)) {
      build(tree);
    }
  }

  private void build(Tree tree) {
    if (tree.is(Kind.EXPRESSION_STATEMENT, Kind.VARIABLE_STATEMENT)) {
      visitSimpleStatement(tree);
    } else if (tree.is(Kind.IF_STATEMENT)) {
      visitIfStatement((IfStatementTree) tree);
    } else if (tree.is(Kind.RETURN_STATEMENT)) {
      visitReturnStatement(tree);
    } else if (tree.is(Kind.BLOCK)) {
      build(((BlockTree) tree).statements());
    } else {
      throw new IllegalArgumentException("Cannot build CFG for " + tree);
    }
  }

  private void visitReturnStatement(Tree tree) {
    currentBlock.addElement(tree);
    currentBlock.successors().clear();
    endPredecessors.add(currentBlock);
  }

  private void visitIfStatement(IfStatementTree tree) {
    MutableBlock successor = currentBlock;
    if (tree.elseClause() != null) {
      buildSubFlow(tree.elseClause().statement(), successor);
    }
    MutableBlock elseBlock = currentBlock;
    buildSubFlow(tree.statement(), successor);
    MutableBlock thenBlock = currentBlock;
    currentBlock = createBlock(thenBlock, elseBlock);
    currentBlock.addElement(tree.condition());
  }

  private void buildSubFlow(StatementTree subFlowTree, MutableBlock successor) {
    currentBlock = createBlock(successor);
    build(subFlowTree);
  }

  private void visitSimpleStatement(Tree tree) {
    currentBlock.addElement(tree);
  }
  
  private MutableBlock createBlock(MutableBlock... successors) {
    MutableBlock block = new MutableBlock();
    blocks.add(block);
    for (MutableBlock successor : successors) {
      block.addSuccessor(successor);
    }
    return block;
  }

}
