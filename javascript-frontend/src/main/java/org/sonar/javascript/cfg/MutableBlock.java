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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.Tree;

class MutableBlock {
  
  private final Set<MutableBlock> successors = new HashSet<>();
  private final List<Tree> elements = new ArrayList<>();

  public Set<MutableBlock> successors() {
    return successors;
  }

  public void addSuccessor(MutableBlock successor) {
    successors.add(successor);
  }
  
  public List<Tree> elements() {
    return Lists.reverse(elements);
  }

  public void addElement(Tree element) {
    elements.add(element);
  }

  public boolean isEmpty() {
    return elements.isEmpty();
  }

}
