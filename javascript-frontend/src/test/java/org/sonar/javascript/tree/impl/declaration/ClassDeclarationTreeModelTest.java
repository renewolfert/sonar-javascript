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
package org.sonar.javascript.tree.impl.declaration;

import org.junit.Test;
import org.sonar.javascript.utils.JavaScriptTreeModelTest;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.ClassTree;

import static org.fest.assertions.Assertions.assertThat;

public class ClassDeclarationTreeModelTest extends JavaScriptTreeModelTest {

  @Test
  public void without_members() throws Exception {
    ClassTree tree = parse("class C { }", Kind.CLASS_DECLARATION);

    assertThat(tree.is(Kind.CLASS_DECLARATION)).isTrue();
    assertThat(tree.classToken().text()).isEqualTo("class");
    assertThat(tree.name().name()).isEqualTo("C");
    assertThat(tree.extendsToken()).isNull();
    assertThat(tree.superClass()).isNull();
    assertThat(tree.openCurlyBraceToken().text()).isEqualTo("{");
    assertThat(tree.elements()).isEmpty();
    assertThat(tree.methods()).isEmpty();
    assertThat(tree.semicolons()).isEmpty();
    assertThat(tree.closeCurlyBraceToken().text()).isEqualTo("}");
  }

  @Test
  public void with_members() throws Exception {
    ClassTree tree = parse("class C { m() {} static m(){} ; }", Kind.CLASS_DECLARATION);

    assertThat(tree.is(Kind.CLASS_DECLARATION)).isTrue();
    assertThat(tree.classToken().text()).isEqualTo("class");
    assertThat(tree.name().name()).isEqualTo("C");
    assertThat(tree.extendsToken()).isNull();
    assertThat(tree.superClass()).isNull();
    assertThat(tree.openCurlyBraceToken().text()).isEqualTo("{");
    assertThat(tree.elements()).hasSize(3);
    assertThat(tree.methods()).hasSize(2);
    assertThat(tree.semicolons()).hasSize(1);
    assertThat(tree.closeCurlyBraceToken().text()).isEqualTo("}");
  }

  @Test
  public void extends_clause() throws Exception {
    ClassTree tree = parse("class C extends S { }", Kind.CLASS_DECLARATION);

    assertThat(tree.extendsToken().text()).isEqualTo("extends");
    assertThat(tree.superClass()).isNotNull();
  }

  @Test
  public void not_global_class_should_be_declaration() throws Exception {
    ClassTree tree = parse("if (true) { class A{} }", Kind.CLASS_DECLARATION);

    assertThat(tree.is(Kind.CLASS_DECLARATION)).isTrue();
    assertThat(tree.name().name()).isEqualTo("A");
  }
}
