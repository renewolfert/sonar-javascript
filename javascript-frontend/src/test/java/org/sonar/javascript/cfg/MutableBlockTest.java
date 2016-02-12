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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.plugins.javascript.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MutableBlockTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final MutableBlock BLOCK1 = MutableBlock.create();
  private static final MutableBlock BLOCK2 = MutableBlock.create();
  private static final MutableBlock END = MutableBlock.createEnd();

  @Test
  public void add_successor() throws Exception {
    assertThat(BLOCK1.successors()).isEmpty();
    BLOCK1.addSuccessor(BLOCK2);
    assertThat(BLOCK1.successors()).containsOnly(BLOCK2);
  }

  @Test(expected = NullPointerException.class)
  public void cannot_add_null_as_successor() throws Exception {
    BLOCK1.addSuccessor(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_add_itself_as_successor() throws Exception {
    BLOCK1.addSuccessor(BLOCK1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void cannot_add_successor_to_end() throws Exception {
    END.addSuccessor(BLOCK1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void cannot_add_element_to_end() throws Exception {
    END.addElement(mock(Tree.class));
  }

}
