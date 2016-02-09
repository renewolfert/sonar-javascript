package org.sonar.javascript.cfg;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import org.junit.Test;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.plugins.javascript.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.javascript.cfg.ControlFlowGraphTest.ControlFlowNodeAssert.assertNode;

public class ControlFlowGraphTest {

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
    assertNode(g.start()).hasSuccessors(g.end());
  }

  @Test
  public void if_then() throws Exception {
    ControlFlowGraph g = build("if (a) { foo(); }", 2);
    assertNode(g.block(0)).hasSuccessors(g.end(), g.block(1));
    assertNode(g.block(1)).hasSuccessors(g.end());
  }

  @Test
  public void if_then_else() throws Exception {
    ControlFlowGraph g = build("if (a) { foo(); } else { bar(); }", 3);
    assertNode(g.block(0)).hasSuccessors(g.block(1), g.block(2));
    assertNode(g.block(1)).hasSuccessors(g.end());
    assertNode(g.block(2)).hasSuccessors(g.end());
  }

  @Test
  public void if_then_followed_by_block() throws Exception {
    ControlFlowGraph g = build("if (a) { foo(); } bar();", 3);
    assertNode(g.block(0)).hasSuccessors(g.block(1), g.block(2));
    assertNode(g.block(1)).hasSuccessors(g.block(2));
    assertNode(g.block(2)).hasSuccessors(g.end());
  }

  private ControlFlowGraph build(String sourceCode, int expectedNumberOfBlocks) {
    Tree tree = parser.parse(sourceCode);
    ControlFlowGraph cfg = ControlFlowGraph.build(tree);
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

    public ControlFlowNodeAssert hasPredecessors(ControlFlowNode... predecessors) {
      Assertions.assertThat(actual.predecessors()).containsOnly(predecessors);
      return this;
    }

    public ControlFlowNodeAssert hasSuccessors(ControlFlowNode... successors) {
      Assertions.assertThat(actual.successors()).containsOnly(successors);
      return this;
    }

  }

}
