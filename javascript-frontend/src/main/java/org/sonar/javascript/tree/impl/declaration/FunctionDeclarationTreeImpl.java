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

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.javascript.tree.impl.expression.IdentifierTreeImpl;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.javascript.tree.impl.statement.BlockTreeImpl;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.ParameterListTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitor;

public class FunctionDeclarationTreeImpl extends JavaScriptTree implements FunctionDeclarationTree {

  private final SyntaxToken functionKeyword;
  private final SyntaxToken starToken;
  private final IdentifierTree name;
  private final ParameterListTree parameters;
  private final BlockTree body;
  private final Kind kind;

  public FunctionDeclarationTreeImpl(
    InternalSyntaxToken functionKeyword, InternalSyntaxToken starToken,
    IdentifierTreeImpl name, ParameterListTreeImpl parameters, BlockTreeImpl body
  ) {

    this.functionKeyword = functionKeyword;
    this.starToken = starToken;
    this.name = name;
    this.parameters = parameters;
    this.body = body;
    this.kind = Kind.GENERATOR_DECLARATION;

  }

  public FunctionDeclarationTreeImpl(
    InternalSyntaxToken functionKeyword, IdentifierTreeImpl name,
    ParameterListTreeImpl parameters, BlockTreeImpl body
  ) {

    this.functionKeyword = functionKeyword;
    this.starToken = null;
    this.name = name;
    this.parameters = parameters;
    this.body = body;
    this.kind = Kind.FUNCTION_DECLARATION;

  }

  @Override
  public SyntaxToken functionKeyword() {
    return functionKeyword;
  }

  @Nullable
  @Override
  public SyntaxToken starToken() {
    return starToken;
  }

  @Override
  public IdentifierTree name() {
    return name;
  }

  @Override
  public ParameterListTree parameterClause() {
    return parameters;
  }

  @Override
  public BlockTree body() {
    return body;
  }

  @Override
  public List<Tree> parametersList() {
    return parameters.parameters();
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(functionKeyword, starToken, name, parameters, body);
  }

  @Override
  public void accept(DoubleDispatchVisitor visitor) {
    visitor.visitFunctionDeclaration(this);
  }
}
