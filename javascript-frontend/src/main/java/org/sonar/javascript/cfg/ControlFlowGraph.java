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

  ControlFlowGraph(List<MutableBlock> blocks, MutableBlock end) {

    Map<MutableBlock, ControlFlowNode> immutableBlockByMutable = new HashMap<>();
    ImmutableList.Builder<ControlFlowBlock> blockListBuilder = ImmutableList.builder();
    int index = 0;
    for (MutableBlock mutableBlock : blocks) {
      ImmutableBlock immutableBlock = new ImmutableBlock(index, mutableBlock.elements());
      immutableBlockByMutable.put(mutableBlock, immutableBlock);
      blockListBuilder.add(immutableBlock);
      index++;
    }
    immutableBlockByMutable.put(end, this.end);
    
    ImmutableSetMultimap.Builder<ControlFlowNode, ControlFlowNode> successorBuilder = ImmutableSetMultimap.builder();
    ImmutableSetMultimap.Builder<ControlFlowNode, ControlFlowNode> predecessorBuilder = ImmutableSetMultimap.builder();
    for (MutableBlock mutableBlock : blocks) {
      ControlFlowNode immutableBlock = immutableBlockByMutable.get(mutableBlock);
      for (MutableBlock mutableBlockSuccessor : mutableBlock.successors()) {
        predecessorBuilder.put(immutableBlockByMutable.get(mutableBlockSuccessor), immutableBlock);
        successorBuilder.put(immutableBlock, immutableBlockByMutable.get(mutableBlockSuccessor));
      }
    }
    
    this.start = blocks.isEmpty() ? this.end : immutableBlockByMutable.get(blocks.get(0));
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
