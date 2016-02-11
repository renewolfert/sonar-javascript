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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.typed.ActionParser;
import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.javascript.cfg.ControlFlowGraphTest.ControlFlowNodeAssert.assertNode;

public class ControlFlowGraphTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActionParser<Tree> parser = JavaScriptParserBuilder.createParser(Charsets.UTF_8);

  @Test
  public void no_block() throws Exception {
    ControlFlowGraph g = build("", 0);
    assertNode(g.start())
      .isEqualTo(g.end())
      .hasNoPredecessor();
  }

  @Test
  public void single_basic_block() throws Exception {
    ControlFlowGraph g = build("foo();", 1);
    assertNode(g.start())
      .isEqualTo(g.block(0))
      .hasNoPredecessor()
      .hasSuccessors(g.end());
  }

  @Test
  public void simple_statements() throws Exception {
    ControlFlowGraph g = build("foo(); var a; a = 2;", 1);
    assertNode(g.block(0)).hasSuccessors(g.end());
    assertThat(g.block(0).elements()).hasSize(3);
    ExpressionStatementTree firstElement = (ExpressionStatementTree) (g.block(0).elements().get(0));
    assertThat(firstElement.expression().is(Kind.CALL_EXPRESSION)).isTrue();
  }

  @Test
  public void if_then() throws Exception {
    ControlFlowGraph g = build("if (a) { foo(); }", 2);
    assertNode(g.block(0)).hasSuccessors(g.end(), g.block(1));
    assertNode(g.block(1)).hasSuccessors(g.end());
  }

  @Test
  public void if_then_else() throws Exception {
    ControlFlowGraph g = build("if (a) { f1(); } else { f2(); }", 3);
    assertNode(g.block(0)).hasSuccessors(g.block(1), g.block(2));
    assertNode(g.block(1)).hasSuccessors(g.end());
    assertNode(g.block(2)).hasSuccessors(g.end());
  }

  @Test
  public void nested_if() throws Exception {
    ControlFlowGraph g = build("if (a) { if (b) { f1(); } f2(); } f3();", 5);
    assertNode(g.block(0)).hasSuccessors(g.block(1), g.block(4));
    assertNode(g.block(1)).hasSuccessors(g.block(2), g.block(3));
    assertNode(g.block(2)).hasSuccessors(g.block(3));
    assertNode(g.block(3)).hasSuccessors(g.block(4));
    assertNode(g.block(4)).hasSuccessors(g.end());
  }

  @Test
  public void return_statement() throws Exception {
    ControlFlowGraph g = build("if (a) { return; } f1();", 3);
    assertNode(g.block(0)).hasSuccessors(g.block(1), g.block(2));
    assertNode(g.block(1)).hasSuccessors(g.end());
    assertNode(g.block(2)).hasSuccessors(g.end());

    g = build("if (a) { f1() } else { return; } f2();", 4);
    assertNode(g.block(0)).hasSuccessors(g.block(1), g.block(2));
    assertNode(g.block(1)).hasSuccessors(g.block(3));
    assertNode(g.block(2)).hasSuccessors(g.end());
    assertNode(g.block(3)).hasSuccessors(g.end());
  }

  @Test
  public void invalid_empty_block() throws Exception {
    MutableBlock block = new MutableBlock();
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot build block 0");
    new ControlFlowGraph(ImmutableList.of(block), ImmutableSet.of(block));
  }

  private ControlFlowGraph build(String sourceCode, int expectedNumberOfBlocks) {
    Tree tree = parser.parse(sourceCode);
    ControlFlowGraph cfg = ControlFlowGraph.build((ScriptTree) tree);
    assertThat(cfg.blocks()).hasSize(expectedNumberOfBlocks);
    assertThat(cfg.end().successors()).isEmpty();
    if (!cfg.blocks().isEmpty()) {
      assertThat(cfg.block(0)).isEqualTo(cfg.start());
    }
    return cfg;
  }

  public static class ControlFlowNodeAssert extends GenericAssert<ControlFlowNodeAssert, ControlFlowNode> {

    protected ControlFlowNodeAssert(ControlFlowNode actual) {
      super(ControlFlowNodeAssert.class, actual);
    }

    public static ControlFlowNodeAssert assertNode(ControlFlowNode actual) {
      return new ControlFlowNodeAssert(actual);
    }

    public ControlFlowNodeAssert hasNoPredecessor() {
      Assertions.assertThat(actual.predecessors()).isEmpty();
      return this;
    }

    public ControlFlowNodeAssert hasSuccessors(ControlFlowNode... successors) {
      Assertions.assertThat(actual.successors()).containsOnly(successors);
      return this;
    }

  }

}
