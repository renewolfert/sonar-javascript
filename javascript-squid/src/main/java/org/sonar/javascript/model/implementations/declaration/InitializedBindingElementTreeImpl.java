/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.model.implementations.declaration;

import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.javascript.ast.visitors.TreeVisitor;
import org.sonar.javascript.model.implementations.JavaScriptTree;
import org.sonar.javascript.model.implementations.lexical.InternalSyntaxToken;
import org.sonar.javascript.model.interfaces.Tree;
import org.sonar.javascript.model.interfaces.declaration.BindingElementTree;
import org.sonar.javascript.model.interfaces.declaration.InitializedBindingElementTree;
import org.sonar.javascript.model.interfaces.expression.ExpressionTree;
import org.sonar.javascript.model.interfaces.lexical.SyntaxToken;

import java.util.Iterator;

public class InitializedBindingElementTreeImpl extends JavaScriptTree implements InitializedBindingElementTree {

  private BindingElementTree left;
  private final SyntaxToken equalToken;
  private final ExpressionTree right;

  public InitializedBindingElementTreeImpl(InternalSyntaxToken equalToken, ExpressionTree right) {
    super(Kind.INITIALIZED_BINDING_ELEMENT);

    this.equalToken = equalToken;
    this.right = right;

    addChildren(equalToken, (AstNode) right);
  }

  public InitializedBindingElementTreeImpl completeWithLeft(BindingElementTree left) {
    this.left = left;

    prependChildren((AstNode) left);
    return this;
  }

  @Override
  public BindingElementTree left() {
    return left;
  }

  @Override
  public SyntaxToken equalToken() {
    return equalToken;
  }

  @Override
  public ExpressionTree right() {
    return right;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(left, right);
  }

  @Override
  public AstNodeType getKind() {
    return Kind.INITIALIZED_BINDING_ELEMENT;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitInitializedBindingElement(this);
  }
}
