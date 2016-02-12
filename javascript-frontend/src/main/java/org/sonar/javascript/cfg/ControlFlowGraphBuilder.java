/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.javascript.cfg;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.BreakStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ContinueStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForInStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.LabelledStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.tree.statement.WhileStatementTree;

class ControlFlowGraphBuilder {

  private final Set<MutableBlock> blocks = new HashSet<>();
  private final EndBlock end = new EndBlock();
  private MutableBlock currentBlock = createSimpleBlock(end);
  private MutableBlock start;
  private final Deque<Loop> loops = new ArrayDeque<>();
  private String currentLabel = null;

  public ControlFlowGraph createGraph(ScriptTree tree) {
    if(tree.items() != null) {
      build(tree.items().items());
    }
    start = currentBlock;
    removeEmptyBlocks();
    return new ControlFlowGraph(blocks, start, end);
  }

  private void removeEmptyBlocks() {
    Map<MutableBlock, MutableBlock> emptyBlockReplacements = new HashMap<>();
    for (SimpleBlock block : Iterables.filter(blocks, SimpleBlock.class)) {
      if (block.isEmpty()) {
        emptyBlockReplacements.put(block, block.firstNonEmptySuccessor());
      }
    }

    blocks.removeAll(emptyBlockReplacements.keySet());

    for (MutableBlock block : blocks) {
      block.replaceSuccessors(emptyBlockReplacements);
    }

    if (emptyBlockReplacements.containsKey(start)) {
      start = emptyBlockReplacements.get(start);
    }
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
    } else if (tree.is(Kind.FOR_STATEMENT)) {
      visitForStatement((ForStatementTree) tree);
    } else if (tree.is(Kind.FOR_IN_STATEMENT)) {
      visitForInStatement((ForInStatementTree) tree);
    } else if (tree.is(Kind.WHILE_STATEMENT)) {
      visitWhileStatement((WhileStatementTree) tree);
    } else if (tree.is(Kind.DO_WHILE_STATEMENT)) {
      visitDoWhileStatement((DoWhileStatementTree) tree);
    } else if (tree.is(Kind.CONTINUE_STATEMENT)) {
      visitContinueStatement((ContinueStatementTree) tree);
    } else if (tree.is(Kind.BREAK_STATEMENT)) {
      visitBreakStatement((BreakStatementTree) tree);
    } else if (tree.is(Kind.RETURN_STATEMENT)) {
      visitReturnStatement(tree);
    } else if (tree.is(Kind.BLOCK)) {
      build(((BlockTree) tree).statements());
    } else if (tree.is(Kind.LABELLED_STATEMENT)) {
      visitLabelledStatement((LabelledStatementTree) tree);
    } else {
      throw new IllegalArgumentException("Cannot build CFG for " + tree);
    }
  }

  private void visitReturnStatement(Tree tree) {
    currentBlock = createSimpleBlock(tree, end);
  }

  private void visitContinueStatement(ContinueStatementTree tree) {
    currentBlock = createSimpleBlock(tree, getLoop(tree.label()).continueTarget);
  }

  private Loop getLoop(IdentifierTree label) {
    if (label != null) {
      for (Loop loop : loops) {
        if (label.name().equals(loop.label)) {
          return loop;
        }
      }
    }
    return loops.peek();
  }

  private void visitBreakStatement(BreakStatementTree tree) {
    currentBlock = createSimpleBlock(tree, getLoop(tree.label()).breakTarget);
  }

  private void visitIfStatement(IfStatementTree tree) {
    MutableBlock successor = currentBlock;
    if (tree.elseClause() != null) {
      buildSubFlow(tree.elseClause().statement(), successor);
    }
    MutableBlock elseBlock = currentBlock;
    MutableBlock thenBlock = buildSubFlow(tree.statement(), successor);
    BranchingBlock branchingBlock = createBranchingBlock(tree.condition());
    branchingBlock.setSuccessors(thenBlock, elseBlock);
    currentBlock = branchingBlock;
  }

  private void visitForStatement(ForStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock conditionBlock = createBranchingBlock(tree.condition());
    SimpleBlock updateBlock = createSimpleBlock(tree.update(), conditionBlock);

    pushLoop(updateBlock, currentBlock);
    MutableBlock loopBodyBlock = buildSubFlow(tree.statement(), updateBlock);
    popLoop();

    conditionBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(tree.init(), conditionBlock);
  }

  private void visitForInStatement(ForInStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock assignmentBlock = createBranchingBlock(tree.variableOrExpression());

    pushLoop(assignmentBlock, currentBlock);
    MutableBlock loopBodyBlock = buildSubFlow(tree.statement(), assignmentBlock);
    popLoop();

    assignmentBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(tree.expression(), assignmentBlock);
  }

  private void visitWhileStatement(WhileStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock conditionBlock = createBranchingBlock(tree.condition());

    pushLoop(conditionBlock, currentBlock);
    MutableBlock loopBodyBlock = buildSubFlow(tree.statement(), conditionBlock);
    popLoop();

    conditionBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = conditionBlock;
  }

  private void visitDoWhileStatement(DoWhileStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock conditionBlock = createBranchingBlock(tree.condition());
    pushLoop(conditionBlock, currentBlock);
    MutableBlock loopBodyBlock = buildSubFlow(tree.statement(), conditionBlock);
    popLoop();
    conditionBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(loopBodyBlock);
  }

  private void visitLabelledStatement(LabelledStatementTree tree) {
    currentLabel = tree.label().name();
    build(tree.statement());
  }

  private void pushLoop(MutableBlock continueTarget, MutableBlock breakTarget) {
    loops.push(new Loop(continueTarget, breakTarget, currentLabel));
    currentLabel = null;
  }

  private void popLoop() {
    loops.pop();
  }

  private MutableBlock buildSubFlow(StatementTree subFlowTree, MutableBlock successor) {
    currentBlock = createSimpleBlock(successor);
    build(subFlowTree);
    return currentBlock;
  }

  private void visitSimpleStatement(Tree tree) {
    currentBlock.addElement(tree);
  }
  
  private BranchingBlock createBranchingBlock(Tree element) {
    BranchingBlock block = new BranchingBlock(element);
    blocks.add(block);
    return block;
  }

  private SimpleBlock createSimpleBlock(Tree element, MutableBlock successor) {
    SimpleBlock block = createSimpleBlock(successor);
    block.addElement(element);
    return block;
  }

  private SimpleBlock createSimpleBlock(MutableBlock successor) {
    SimpleBlock block = new SimpleBlock(successor);
    blocks.add(block);
    return block;
  }

  private static class Loop {

    final MutableBlock continueTarget;
    final MutableBlock breakTarget;
    final String label;

    public Loop(MutableBlock continueTarget, MutableBlock breakTarget, String label) {
      this.continueTarget = continueTarget;
      this.breakTarget = breakTarget;
      this.label = label;
    }

  }

}
