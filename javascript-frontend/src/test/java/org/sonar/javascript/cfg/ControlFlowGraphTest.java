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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sonar.sslr.api.typed.ActionParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.javascript.utils.SourceBuilder;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;

import static org.fest.assertions.Assertions.assertThat;

public class ControlFlowGraphTest {

  private static final int END = -1;

  private static final Ordering<ControlFlowBlock> BLOCK_ORDERING = Ordering.from(new BlockTokenIndexComparator());

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
    assertThat(g.start()).isEqualTo(g.blocks().iterator().next());
    assertThat(g.start().predecessors()).isEmpty();
    assertBlock(g, 0).hasSuccessors(END);
  }

  @Test
  public void simple_statements() throws Exception {
    ControlFlowGraph g = build("foo(); var a; a = 2;", 1);
    assertBlock(g, 0).hasSuccessors(END);
    ControlFlowBlock block = g.blocks().iterator().next();
    assertThat(block.elements()).hasSize(3);
    assertThat(block.elements().get(0).is(Kind.CALL_EXPRESSION)).isTrue();
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
  public void break_with_label() throws Exception {
    ControlFlowGraph g = build("outer: while (b0) { inner: while (b1) { b2(); break outer; } b3(); }", 4);
    assertBlock(g, 0).hasSuccessors(1, END);
    assertBlock(g, 1).hasSuccessors(2, 3);
    assertBlock(g, 2).hasSuccessors(END);
    assertBlock(g, 3).hasSuccessors(0);

    g = build("outer: while (b0) { inner: while (b1) { b2(); break xxx; } b3(); }", 4);
    assertBlock(g, 0).hasSuccessors(1, END);
    assertBlock(g, 1).hasSuccessors(2, 3);
    assertBlock(g, 2).hasSuccessors(3);
    assertBlock(g, 3).hasSuccessors(0);
  }

  @Test
  public void for_loop() throws Exception {
    ControlFlowGraph g = build("for(i=0; i<10; i++) { f1(); } ", 4);
    assertBlock(g, 0).hasSuccessors(1);
    assertBlock(g, 1).hasSuccessors(3, END);
    assertBlock(g, 2).hasSuccessors(1);
    assertBlock(g, 3).hasSuccessors(2);
  }

  @Test
  public void continue_in_for() throws Exception {
    ControlFlowGraph g = build("for(i=0; i<10; i++) { if (a) continue; f1(); } ", 6);
    assertBlock(g, 0).hasSuccessors(1);
    assertBlock(g, 1).hasSuccessors(3, END);
    assertBlock(g, 2).hasSuccessors(1);
    assertBlock(g, 3).hasSuccessors(4, 5);
    assertBlock(g, 4).hasSuccessors(2);
    assertBlock(g, 5).hasSuccessors(2);
  }

  @Test
  public void break_in_for() throws Exception {
    ControlFlowGraph g = build("for(i=0; i<10; i++) { if (a) break; f1(); } ", 6);
    assertBlock(g, 0).hasSuccessors(1);
    assertBlock(g, 1).hasSuccessors(3, END);
    assertBlock(g, 2).hasSuccessors(1);
    assertBlock(g, 3).hasSuccessors(4, 5);
    assertBlock(g, 4).hasSuccessors(END);
    assertBlock(g, 5).hasSuccessors(2);
  }

  @Test
  public void for_in() throws Exception {
    ControlFlowGraph g = build("for(var i in obj) { f2(); } ", 3, 1);
    assertBlock(g, 0).hasSuccessors(2, END);
    assertBlock(g, 1).hasSuccessors(0);
    assertBlock(g, 2).hasSuccessors(0);

    g = build("f1(); for(var i in obj) { f2(); } ", 3);
    assertBlock(g, 0).hasSuccessors(1);
    assertBlock(g, 1).hasSuccessors(2, END);
    assertBlock(g, 2).hasSuccessors(1);
  }

  @Test
  public void continue_in_for_in() throws Exception {
    ControlFlowGraph g = build("for(var b0 in b1) { if (b2) { b3(); continue; } b4(); } ", 5, 1);
    assertBlock(g, 0).hasSuccessors(2, END);
    assertBlock(g, 1).hasSuccessors(0);
    assertBlock(g, 2).hasSuccessors(3, 4);
    assertBlock(g, 3).hasSuccessors(0);
    assertBlock(g, 4).hasSuccessors(0);
  }

  @Test
  public void for_of() throws Exception {
    ControlFlowGraph g = build("for(let b0 of b1) { b2(); } ", 3, 1);
    assertBlock(g, 0).hasSuccessors(2, END);
    assertBlock(g, 1).hasSuccessors(0);
    assertBlock(g, 2).hasSuccessors(0);
  }

  @Test
  public void try_catch() throws Exception {
    ControlFlowGraph g = build("try { b0(); } catch(e) { foo(); } ", 2);
    assertBlock(g, 0).hasSuccessors(1).hasElements("b0()");
    assertBlock(g, 1).hasSuccessors(END).hasElements("e", "foo()");
  }

  @Test
  public void try_finally() throws Exception {
    ControlFlowGraph g = build("try { b0(); } finally { bar(); } ", 2);
    assertBlock(g, 0).hasSuccessors(1).hasElements("b0()");
    assertBlock(g, 1).hasSuccessors(END).hasElements("bar()");
  }

  @Test
  public void throw_without_try() throws Exception {
    ControlFlowGraph g = build("if (b0) { throw b1; } b2(); ", 3);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(END);
    assertBlock(g, 2).hasSuccessors(END);
  }

  @Test
  public void throw_in_try() throws Exception {
    ControlFlowGraph g = build("try { if (b0) { throw b1; } b2(); } catch(b3) { foo(); } ", 4);
    assertBlock(g, 0).hasSuccessors(1, 2);
    assertBlock(g, 1).hasSuccessors(3);
    assertBlock(g, 2).hasSuccessors(3);
    assertBlock(g, 3).hasSuccessors(END);
  }

  @Test
  public void invalid_empty_block() throws Exception {
    EndBlock end = new EndBlock();
    MutableBlock block = new SimpleBlock(end);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot build block 0");
    new ControlFlowGraph(ImmutableSet.of(block), block, end);
  }

  private ControlFlowGraph build(String sourceCode, int expectedNumberOfBlocks) {
    return build(sourceCode, expectedNumberOfBlocks, 0);
  }

  private ControlFlowGraph build(String sourceCode, int expectedNumberOfBlocks, int expectedStartIndex) {
    Tree tree = parser.parse(sourceCode);
    ControlFlowGraph cfg = ControlFlowGraph.build((ScriptTree) tree);
    assertThat(cfg.blocks()).hasSize(expectedNumberOfBlocks);
    assertThat(cfg.end().successors()).isEmpty();
    if (!cfg.blocks().isEmpty()) {
      assertThat(sortBlocks(cfg.blocks()).get(expectedStartIndex)).as("Start block").isEqualTo(cfg.start());
    }
    return cfg;
  }


  public static BlockAssert assertBlock(ControlFlowGraph cfg, int blockIndex) {
    return new BlockAssert(cfg, blockIndex);
  }

  public static class BlockAssert {

    private final ControlFlowGraph cfg;
    private final int blockIndex;
    private final List<ControlFlowBlock> blocks;

    public BlockAssert(ControlFlowGraph cfg, int blockIndex) {
      this.cfg = cfg;
      this.blockIndex = blockIndex;
      this.blocks = sortBlocks(cfg.blocks());
    }

    public BlockAssert hasSuccessors(int... expectedSuccessorIndexes) {
      Set<String> actual = new TreeSet<>();
      for (ControlFlowNode successor : blocks.get(blockIndex).successors()) {
        actual.add(successor == cfg.end() ? "END" : Integer.toString(blocks.indexOf(successor)));
      }

      Set<String> expected = new TreeSet<>();
      for (int expectedSuccessorIndex : expectedSuccessorIndexes) {
        expected.add(expectedSuccessorIndex == END ? "END" : Integer.toString(expectedSuccessorIndex));
      }

      assertThat(actual).as("Successors of block " + blockIndex).isEqualTo(expected);

      return this;
    }

    public BlockAssert hasElements(String... elementSources) {
      List<String> actual = new ArrayList<>();
      for (Tree element : blocks.get(blockIndex).elements()) {
        actual.add(SourceBuilder.build(element).trim());
      }
      assertThat(actual).isEqualTo(ImmutableList.copyOf(elementSources));

      return this;
    }

  }

  private static List<ControlFlowBlock> sortBlocks(Iterable<ControlFlowBlock> blocks) {
    return BLOCK_ORDERING.sortedCopy(blocks);
  }

  private static class BlockTokenIndexComparator implements Comparator<ControlFlowBlock> {

    @Override
    public int compare(ControlFlowBlock b1, ControlFlowBlock b2) {

      return Ints.compare(tokenIndex(b1), tokenIndex(b2));
    }

    private static int tokenIndex(ControlFlowBlock block) {
      Preconditions.checkArgument(!block.elements().isEmpty(), "Cannot sort empty block");
      JavaScriptTree tree = (JavaScriptTree) block.elements().get(0);
      InternalSyntaxToken token = (InternalSyntaxToken) tree.getFirstToken();
      return token.startIndex();
    }

  }

}
