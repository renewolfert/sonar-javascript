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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.BreakStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ContinueStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.tree.statement.WhileStatementTree;

class ControlFlowGraphBuilder {

  private final List<MutableBlock> blocks = new LinkedList<>();
  private final Set<MutableBlock> endPredecessors = new HashSet<>();
  private MutableBlock currentBlock = createBlock();
  private final Deque<Loop> loops = new ArrayDeque<>();

  public ControlFlowGraph createGraph(ScriptTree tree) {
    endPredecessors.add(currentBlock);
    if(tree.items() != null) {
      build(tree.items().items());
    }
    removeEmptyBlocks();
    return new ControlFlowGraph(Lists.reverse(blocks), endPredecessors);
  }

  private void removeEmptyBlocks() {
    Set<MutableBlock> emptyBlocks = new HashSet<>();
    SetMultimap<MutableBlock, MutableBlock> replacements = HashMultimap.create();
    for (MutableBlock block : blocks) {
      if (block.isEmpty()) {
        emptyBlocks.add(block);
        replacements.putAll(block, block.successors());
      }
    }

    Queue<MutableBlock> queue = new ArrayDeque<>(replacements.keySet());
    while (!queue.isEmpty()) {
      MutableBlock block = queue.remove();
      Set<MutableBlock> blockReplacements = replacements.get(block);
      Set<MutableBlock> emptyBlockReplacements = Sets.intersection(blockReplacements, emptyBlocks);
      blockReplacements.removeAll(emptyBlockReplacements);
      for (MutableBlock emptyBlock : emptyBlockReplacements) {
        blockReplacements.addAll(replacements.get(emptyBlock));
      }
      if (!Sets.intersection(blockReplacements, emptyBlocks).isEmpty()) {
        queue.add(block);
      }
    }

    for (MutableBlock block : blocks) {
      for (MutableBlock emptySuccessor : Sets.intersection(emptyBlocks, block.successors())) {
        block.successors().remove(emptySuccessor);
        block.successors().addAll(replacements.get(emptySuccessor));
        if (endPredecessors.contains(emptySuccessor)) {
          endPredecessors.add(block);
        }
      }
    }

    blocks.removeAll(emptyBlocks);
    endPredecessors.removeAll(emptyBlocks);
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
    } else {
      throw new IllegalArgumentException("Cannot build CFG for " + tree);
    }
  }

  private void visitReturnStatement(Tree tree) {
    currentBlock.addElement(tree);
    currentBlock.successors().clear();
    endPredecessors.add(currentBlock);
  }

  private void visitContinueStatement(ContinueStatementTree tree) {
    currentBlock.addElement(tree);
    currentBlock.successors().clear();
    currentBlock.addSuccessor(loops.peek().continueTarget);
  }

  private void visitBreakStatement(BreakStatementTree tree) {
    currentBlock.addElement(tree);
    currentBlock.successors().clear();
    currentBlock.addSuccessor(loops.peek().breakTarget);
  }

  private void visitIfStatement(IfStatementTree tree) {
    MutableBlock successor = currentBlock;
    if (tree.elseClause() != null) {
      buildSubFlow(tree.elseClause().statement(), successor);
    }
    MutableBlock elseBlock = currentBlock;
    MutableBlock thenBlock = buildSubFlow(tree.statement(), successor);
    currentBlock = createBlock(tree.condition(), thenBlock, elseBlock);
  }

  private void visitWhileStatement(WhileStatementTree tree) {
    MutableBlock conditionBlock = new MutableBlock();
    conditionBlock.addSuccessor(currentBlock);
    conditionBlock.addElement(tree.condition());

    loops.push(new Loop(conditionBlock, currentBlock));
    MutableBlock loopBodyBlock = buildSubFlow(tree.statement(), conditionBlock);
    loops.pop();

    conditionBlock.addSuccessor(loopBodyBlock);
    currentBlock = conditionBlock;

    // This has to be done at the end because blocks have to be in the correct order
    blocks.add(conditionBlock);
  }

  private void visitDoWhileStatement(DoWhileStatementTree tree) {
    MutableBlock conditionBlock = createBlock(tree.condition(), currentBlock);
    loops.push(new Loop(conditionBlock, currentBlock));
    MutableBlock loopBodyBlock = buildSubFlow(tree.statement(), conditionBlock);
    loops.pop();
    conditionBlock.addSuccessor(loopBodyBlock);
    currentBlock = createBlock(loopBodyBlock);
  }

  private MutableBlock buildSubFlow(StatementTree subFlowTree, MutableBlock successor) {
    currentBlock = createBlock(successor);
    build(subFlowTree);
    return currentBlock;
  }

  private void visitSimpleStatement(Tree tree) {
    currentBlock.addElement(tree);
  }
  
  private MutableBlock createBlock(Tree element, MutableBlock... successors) {
    MutableBlock block = createBlock(successors);
    block.addElement(element);
    return block;
  }

  private MutableBlock createBlock(MutableBlock... successors) {
    MutableBlock block = new MutableBlock();
    blocks.add(block);
    for (MutableBlock successor : successors) {
      block.addSuccessor(successor);
    }
    return block;
  }

  private static class Loop {

    final MutableBlock continueTarget;
    final MutableBlock breakTarget;

    public Loop(MutableBlock continueTarget, MutableBlock breakTarget) {
      this.continueTarget = continueTarget;
      this.breakTarget = breakTarget;
    }

  }

}