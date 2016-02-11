package org.sonar.javascript.cfg;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;

public class ControlFlowGraph {

  private final ControlFlowNode start;
  private final ControlFlowNode end = new EndNode();
  private final List<ControlFlowBlock> blocks;
  private final ImmutableSetMultimap<ControlFlowNode, ControlFlowNode> predecessors;
  private final ImmutableSetMultimap<ControlFlowNode, ControlFlowNode> successors;

  ControlFlowGraph(List<MutableBlock> blocks, Set<MutableBlock> endPredecessors) {

    Map<MutableBlock, ControlFlowNode> immutableBlockByMutable = new HashMap<>();
    ImmutableList.Builder<ControlFlowBlock> blockListBuilder = ImmutableList.builder();
    int index = 0;
    for (MutableBlock mutableBlock : blocks) {
      ImmutableBlock immutableBlock = new ImmutableBlock(index, mutableBlock.elements());
      immutableBlockByMutable.put(mutableBlock, immutableBlock);
      blockListBuilder.add(immutableBlock);
      index++;
    }
    
    ImmutableSetMultimap.Builder<ControlFlowNode, ControlFlowNode> successorBuilder = ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<ControlFlowNode, ControlFlowNode> predecessorBuilder = ImmutableSetMultimap.builder();
    for (MutableBlock mutableBlock : blocks) {
      ControlFlowNode immutableBlock = immutableBlockByMutable.get(mutableBlock);
      for (MutableBlock mutableBlockSuccessor : mutableBlock.successors()) {
        predecessorBuilder.put(immutableBlockByMutable.get(mutableBlockSuccessor), immutableBlock);
        successorBuilder.put(immutableBlock, immutableBlockByMutable.get(mutableBlockSuccessor));
      }
    }
    
    for (MutableBlock endPredecessor : endPredecessors) {
      ControlFlowNode immutableBlock = immutableBlockByMutable.get(endPredecessor);
      successorBuilder.put(immutableBlock, end);
      predecessorBuilder.put(end, immutableBlock);
    }
    
    this.start = blocks.isEmpty() ? end : immutableBlockByMutable.get(blocks.get(0));
    this.blocks = blockListBuilder.build();
    this.predecessors = predecessorBuilder.build();
    this.successors = successorBuilder.build();
  }

  public static ControlFlowGraph build(ScriptTree tree) {
    return new ControlFlowGraphBuilder().createGraph(tree);
  }

  public ControlFlowNode start() {
    return start;
  }

  public ControlFlowNode end() {
    return end;
  }

  public List<ControlFlowBlock> blocks() {
    return blocks;
  }

  public ControlFlowBlock block(int blockIndex) {
    return blocks().get(blockIndex);
  }

  private class ImmutableBlock implements ControlFlowBlock {
    
    private final int index;
    private final List<Tree> elements;
    
    public ImmutableBlock(int index, List<Tree> elements) {
      Preconditions.checkArgument(!elements.isEmpty(), "Cannot build block " + index + " without any element");
      this.index = index;
      this.elements = elements;
    }

    @Override
    public Set<ControlFlowNode> predecessors() {
      return predecessors.get(this);
    }

    @Override
    public Set<ControlFlowNode> successors() {
      return successors.get(this);
    }

    @Override
    public List<Tree> elements() {
      return elements;
    }

    @Override
    public String toString() {
      return "Block" + index;
    }

  }
  
  private class EndNode implements ControlFlowNode {

    @Override
    public Set<ControlFlowNode> predecessors() {
      return predecessors.get(this);
    }

    @Override
    public Set<ControlFlowNode> successors() {
      return ImmutableSet.of();
    }

    @Override
    public String toString() {
      return "End";
    }
    
  }
  
}
