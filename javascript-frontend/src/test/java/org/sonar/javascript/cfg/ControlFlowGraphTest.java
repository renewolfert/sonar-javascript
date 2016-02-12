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
import java.util.Set;
import java.util.TreeSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;

import static org.fest.assertions.Assertions.assertThat;

public class ControlFlowGraphTest {

  private static final int END = -1;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ActionParser<Tree> parser = JavaScriptParserBuilder.createParser(Charsets.UTF_8);

  @Test
  public void no_block() throws Exception {
    ControlFlowGraph g = build("", 0);
    assertThat(g.start()).isEqualTo(g.end());
  }

  @Test
  public void single_basic_block() throws Exception {
    ControlFlowGraph g = build("foo();", 1);
    assertThat(g.start()).isEqualTo(g.block(0));
    assertThat(g.start().predecessors()).isEmpty();
    assertBlock(g, 0).hasSuccessors(END);
  }

  @Test
  public void simple_statements() throws Exception {
    ControlFlowGraph g = build("foo(); var a; a = 2;", 1);
    assertBlock(g, 0).hasSuccessors(END);
    assertThat(g.block(0).elements()).hasSize(3);
    ExpressionStatementTree firstElement = (ExpressionStatementTree) (g.block(0).elements().get(0));
    assertThat(firstElement.expression().is(Kind.CALL_EXPRESSION)).isTrue();
  }

  @Test
  public void if_then() throws Exception {
    ControlFlowGraph g = build("if (a) { foo(); }", 2);
    assertBlock(g, 0).hasSuccessors(1, END);
    assertBlock(g, 1).hasSuccessors(END);
  }

  @Test
  public void if_then_else() throws Exception {
    ControlFlowGraph g = build("if (a) { f1(); } else { f2(); }", 3);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(END);
    assertBlock(g, 2).hasSuccessors(END);
  }

  @Test
  public void nested_if() throws Exception {
    ControlFlowGraph g = build("if (a) { if (b) { f1(); } f2(); } f3();", 5);
    assertBlock(g, 0).hasSuccessors(1, 4);
    assertBlock(g, 1).hasSuccessors(2, 3);
    assertBlock(g, 2).hasSuccessors(3);
    assertBlock(g, 3).hasSuccessors(4);
    assertBlock(g, 4).hasSuccessors(END);
  }

  @Test
  public void return_statement() throws Exception {
    ControlFlowGraph g = build("if (a) { return; } f1();", 3);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(END);
    assertBlock(g, 2).hasSuccessors(END);

    g = build("if (a) { f1() } else { return; } f2();", 4);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(3);
    assertBlock(g, 2).hasSuccessors(END);
    assertBlock(g, 3).hasSuccessors(END);
  }

  @Test
  public void while_loop() throws Exception {
    ControlFlowGraph g = build("while (a) { f1(); }", 2);
    assertBlock(g, 0).hasSuccessors(1, END);
    assertBlock(g, 1).hasSuccessors(0);
  }

  @Test
  public void do_while_loop() throws Exception {
    ControlFlowGraph g = build("f1(); do { f2(); } while(a); f3();", 4);
    assertBlock(g, 0).hasSuccessors(1);
    assertBlock(g, 1).hasSuccessors(2);
    assertBlock(g, 2).hasSuccessors(1, 3);
    assertBlock(g, 3).hasSuccessors(END);
  }

  @Test
  public void continue_in_while() throws Exception {
    ControlFlowGraph g = build("while (a) { if (b) { continue; } f1(); } f2();", 5);
    assertBlock(g, 0).hasSuccessors(1, 4);
    assertBlock(g, 1).hasSuccessors(2, 3);
    assertBlock(g, 2).hasSuccessors(0);
    assertBlock(g, 3).hasSuccessors(0);
    assertBlock(g, 4).hasSuccessors(END);
  }

  @Test
  public void break_in_while() throws Exception {
    ControlFlowGraph g = build("while (a) { if (b) { break; } f1(); } f2();", 5);
    assertBlock(g, 0).hasSuccessors(1, 4);
    assertBlock(g, 1).hasSuccessors(2, 3);
    assertBlock(g, 2).hasSuccessors(4);
    assertBlock(g, 3).hasSuccessors(0);
    assertBlock(g, 4).hasSuccessors(END);
  }

  @Test
  public void continue_in_do_while() throws Exception {
    ControlFlowGraph g = build("do { if (a) { continue; } f1(); } while(a);", 4);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(3);
    assertBlock(g, 2).hasSuccessors(3);
    assertBlock(g, 3).hasSuccessors(0, END);
  }

  @Test
  public void break_in_do_while() throws Exception {
    ControlFlowGraph g = build("do { if (a) { break; } f1(); } while(a);", 4);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(END);
    assertBlock(g, 2).hasSuccessors(3);
    assertBlock(g, 3).hasSuccessors(0, END);
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


  public static BlockAssert assertBlock(ControlFlowGraph cfg, int blockIndex) {
    return new BlockAssert(cfg, blockIndex);
  }

  public static class BlockAssert {

    private final ControlFlowGraph cfg;
    private final int blockIndex;

    public BlockAssert(ControlFlowGraph cfg, int blockIndex) {
      this.cfg = cfg;
      this.blockIndex = blockIndex;
    }

    public void hasSuccessors(int... expectedSuccessorIndexes) {
      Set<String> actual = new TreeSet<>();
      for (ControlFlowNode successor : cfg.block(blockIndex).successors()) {
        actual.add(successor == cfg.end() ? "END" : Integer.toString(cfg.blocks().indexOf(successor)));
      }
      Set<String> expected = new TreeSet<>();
      for (int expectedSuccessorIndex : expectedSuccessorIndexes) {
        expected.add(expectedSuccessorIndex == END ? "END" : Integer.toString(expectedSuccessorIndex));
      }
      assertThat(actual).as("Successors of block " + blockIndex).isEqualTo(expected);
    }
  }

}
