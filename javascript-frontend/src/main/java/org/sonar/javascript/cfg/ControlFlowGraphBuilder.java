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

import com.google.common.base.Preconditions;
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
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.BreakStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.CaseClauseTree;
import org.sonar.plugins.javascript.api.tree.statement.ContinueStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForInStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForOfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.LabelledStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.tree.statement.SwitchClauseTree;
import org.sonar.plugins.javascript.api.tree.statement.SwitchStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ThrowStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.TryStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.WhileStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.WithStatementTree;

class ControlFlowGraphBuilder {

  private final Set<MutableBlock> blocks = new HashSet<>();
  private final EndBlock end = new EndBlock();
  private MutableBlock currentBlock = createSimpleBlock(end);
  private MutableBlock start;
  private final Deque<Breakable> breakables = new ArrayDeque<>();
  private final Deque<MutableBlock> throwTargets = new ArrayDeque<>();
  private String currentLabel = null;

  public ControlFlowGraph createGraph(ScriptTree tree) {
    throwTargets.push(end);
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
    if (tree.is(Kind.EXPRESSION_STATEMENT)) {
      currentBlock.addElement(((ExpressionStatementTree) tree).expression());
    } else if (tree.is(Kind.VARIABLE_STATEMENT)) {
      currentBlock.addElement(((VariableStatementTree) tree).declaration());
    } else if (tree.is(Kind.IF_STATEMENT)) {
      visitIfStatement((IfStatementTree) tree);
    } else if (tree.is(Kind.FOR_STATEMENT)) {
      visitForStatement((ForStatementTree) tree);
    } else if (tree.is(Kind.FOR_IN_STATEMENT)) {
      visitForInStatement((ForInStatementTree) tree);
    } else if (tree.is(Kind.FOR_OF_STATEMENT)) {
      visitForOfStatement((ForOfStatementTree) tree);
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
    } else if (tree.is(Kind.TRY_STATEMENT)) {
      visitTryStatement((TryStatementTree) tree);
    } else if (tree.is(Kind.THROW_STATEMENT)) {
      visitThrowStatement((ThrowStatementTree) tree);
    } else if (tree.is(Kind.SWITCH_STATEMENT)) {
      visitSwitchStatement((SwitchStatementTree) tree);
    } else if (tree.is(Kind.WITH_STATEMENT)) {
      WithStatementTree with = (WithStatementTree) tree;
      build(with.statement());
      currentBlock.addElement(with.expression());
    } else if (tree.is(Kind.FUNCTION_DECLARATION, Kind.GENERATOR_DECLARATION, Kind.CLASS_DECLARATION, Kind.DEBUGGER_STATEMENT)) {
      currentBlock.addElement(tree);
    } else if (tree.is(Kind.EMPTY_STATEMENT)) {
      // Nothing to do
    } else {
      throw new IllegalArgumentException("Cannot build CFG for " + tree);
    }
  }

  private void visitReturnStatement(Tree tree) {
    currentBlock = createSimpleBlock(tree, end);
  }

  private void visitContinueStatement(ContinueStatementTree tree) {
    MutableBlock target = null;
    String label = tree.label() == null ? null : tree.label().name();
    for (Breakable breakable : breakables) {
      if (breakable.continueTarget != null && (label == null || label.equals(breakable.label))) {
        target = breakable.continueTarget;
        break;
      }
    }
    Preconditions.checkState(target != null, "No continue target can be found for label " + label);
    currentBlock = createSimpleBlock(tree, target);
  }

  private void visitBreakStatement(BreakStatementTree tree) {
    MutableBlock target = null;
    String label = tree.label() == null ? null : tree.label().name();
    for (Breakable breakable : breakables) {
      if (label == null || label.equals(breakable.label)) {
        target = breakable.breakTarget;
        break;
      }
    }
    Preconditions.checkState(target != null, "No break target can be found for label " + label);
    currentBlock = createSimpleBlock(tree, target);
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

    MutableBlock loopBodyBlock = buildLoopBody(tree.statement(), updateBlock);

    conditionBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(tree.init(), conditionBlock);
  }

  private void visitForInStatement(ForInStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock assignmentBlock = createBranchingBlock(tree.variableOrExpression());

    MutableBlock loopBodyBlock = buildLoopBody(tree.statement(), assignmentBlock);

    assignmentBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(tree.expression(), assignmentBlock);
  }

  private void visitForOfStatement(ForOfStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock assignmentBlock = createBranchingBlock(tree.variableOrExpression());

    MutableBlock loopBodyBlock = buildLoopBody(tree.statement(), assignmentBlock);

    assignmentBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(tree.expression(), assignmentBlock);
  }

  private void visitWhileStatement(WhileStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock conditionBlock = createBranchingBlock(tree.condition());

    MutableBlock loopBodyBlock = buildLoopBody(tree.statement(), conditionBlock);

    conditionBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = conditionBlock;
  }

  private void visitDoWhileStatement(DoWhileStatementTree tree) {
    MutableBlock successor = currentBlock;
    BranchingBlock conditionBlock = createBranchingBlock(tree.condition());
    MutableBlock loopBodyBlock = buildLoopBody(tree.statement(), conditionBlock);
    conditionBlock.setSuccessors(loopBodyBlock, successor);
    currentBlock = createSimpleBlock(loopBodyBlock);
  }

  private void visitLabelledStatement(LabelledStatementTree tree) {
    currentLabel = tree.label().name();
    build(tree.statement());
  }

  private void visitTryStatement(TryStatementTree tree) {
    if (tree.finallyBlock() != null) {
      currentBlock = createSimpleBlock(currentBlock);
      build(tree.finallyBlock());
    }

    if (tree.catchBlock() != null) {
      currentBlock = createSimpleBlock(currentBlock);
      build(tree.catchBlock().block());
      currentBlock.addElement(tree.catchBlock().parameter());
    }

    throwTargets.push(currentBlock);
    currentBlock = createSimpleBlock(currentBlock);
    build(tree.block());
    throwTargets.pop();
  }

  private void visitThrowStatement(ThrowStatementTree tree) {
    currentBlock = createSimpleBlock(tree.expression(), throwTargets.peek());
  }

  private void visitSwitchStatement(SwitchStatementTree tree) {
    breakables.addFirst(new Breakable(null, currentBlock, null));
    MutableBlock nextStatementBlock = currentBlock;
    for (SwitchClauseTree switchCaseClause : Lists.reverse(tree.cases())) {
      MutableBlock successor = currentBlock;
      currentBlock = createSimpleBlock(successor);
      build(switchCaseClause.statements());
      if (!switchCaseClause.statements().isEmpty()) {
        nextStatementBlock = currentBlock;
      }
      if (switchCaseClause.is(Kind.CASE_CLAUSE)) {
        CaseClauseTree caseClause = (CaseClauseTree) switchCaseClause;
        BranchingBlock caseBlock = createBranchingBlock(caseClause.expression());
        caseBlock.setSuccessors(nextStatementBlock, successor);
        currentBlock = caseBlock;
      }
    }
    breakables.removeFirst();
    currentBlock.addElement(tree.expression());
  }

  private MutableBlock buildLoopBody(StatementTree body, MutableBlock conditionBlock) {
    breakables.addFirst(new Breakable(conditionBlock, currentBlock, currentLabel));
    currentLabel = null;
    MutableBlock loopBodyBlock = buildSubFlow(body, conditionBlock);
    breakables.removeFirst();
    return loopBodyBlock;
  }

  private MutableBlock buildSubFlow(StatementTree subFlowTree, MutableBlock successor) {
    currentBlock = createSimpleBlock(successor);
    build(subFlowTree);
    return currentBlock;
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

  private static class Breakable {

    final MutableBlock continueTarget;
    final MutableBlock breakTarget;
    final String label;

    public Breakable(MutableBlock continueTarget, MutableBlock breakTarget, String label) {
      this.continueTarget = continueTarget;
      this.breakTarget = breakTarget;
      this.label = label;
    }

  }

}
