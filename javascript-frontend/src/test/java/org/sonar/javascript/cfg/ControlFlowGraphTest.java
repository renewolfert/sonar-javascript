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
      .isNotEqualTo(g.end())
      .hasNoPredecessor()
      .hasSuccessors(g.end());
  }

  private ControlFlowGraph build(String sourceCode, int expectedNumberOfBlocks) {
    Tree tree = parser.parse(sourceCode);
    ControlFlowGraph cfg = ControlFlowGraph.build(tree);
    assertThat(cfg.blocks()).hasSize(expectedNumberOfBlocks);
    assertThat(cfg.end().successors()).isEmpty();
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
