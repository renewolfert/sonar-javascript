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
package org.sonar.javascript.tree.impl.expression;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.javascript.tree.impl.declaration.ParameterListTreeImpl;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.javascript.tree.impl.statement.BlockTreeImpl;
import org.sonar.javascript.tree.symbols.type.FunctionType;
import org.sonar.javascript.tree.symbols.type.TypableTree;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.symbols.TypeSet;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.ParameterListTree;
import org.sonar.plugins.javascript.api.tree.expression.FunctionExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitor;

public class FunctionExpressionTreeImpl extends JavaScriptTree implements FunctionExpressionTree, TypableTree {

  private final SyntaxToken functionKeyword;
  @Nullable
  private final SyntaxToken star;
  @Nullable
  private final IdentifierTree name;
  private final ParameterListTree parameters;
  private final BlockTreeImpl body;
  private final Kind kind;
  private Type functionType;

  /**
   * Constructor for named generator expression and  generator declaration
   */
  public FunctionExpressionTreeImpl(
    Kind kind, InternalSyntaxToken functionKeyword, InternalSyntaxToken star, IdentifierTreeImpl name,
    ParameterListTreeImpl parameters, BlockTreeImpl body
  ) {

    this.functionKeyword = functionKeyword;
    this.star = star;
    this.name = name;
    this.parameters = parameters;
    this.body = body;

    this.kind = kind;

    this.functionType = FunctionType.create(this);
  }

  /**
   * Constructor for NOT named generator expression
   */
  public FunctionExpressionTreeImpl(
    Kind kind, InternalSyntaxToken functionKeyword, InternalSyntaxToken star,
    ParameterListTreeImpl parameters, BlockTreeImpl body
  ) {

    this.functionKeyword = functionKeyword;
    this.star = star;
    this.name = null;
    this.parameters = parameters;
    this.body = body;

    this.kind = kind;


    this.functionType = FunctionType.create(this);
  }

  /**
   * Constructor for named function expression and function declaration
   */
  public FunctionExpressionTreeImpl(
    Kind kind, InternalSyntaxToken functionKeyword, IdentifierTreeImpl name,
    ParameterListTreeImpl parameters, BlockTreeImpl body
  ) {

    this.functionKeyword = functionKeyword;
    this.star = null;
    this.name = name;
    this.parameters = parameters;
    this.body = body;

    this.kind = kind;

    this.functionType = FunctionType.create(this);
  }

  /**
   * Constructor for NOT named function expression
   */
  public FunctionExpressionTreeImpl(
    Kind kind, InternalSyntaxToken functionKeyword, ParameterListTreeImpl parameters,
    BlockTreeImpl body
  ) {

    this.functionKeyword = functionKeyword;
    this.star = null;
    this.name = null;
    this.parameters = parameters;
    this.body = body;

    this.kind = kind;

    this.functionType = FunctionType.create(this);
  }

  @Override
  public SyntaxToken functionKeyword() {
    return functionKeyword;
  }

  @Nullable
  @Override
  public SyntaxToken star() {
    return star;
  }

  @Nullable
  @Override
  public IdentifierTree name() {
    return name;
  }

  @Override
  public ParameterListTree parameterClause() {
    return parameters;
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public BlockTreeImpl body() {
    return body;
  }

  @Override
  public List<Tree> parametersList() {
    return parameters.parameters();
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(functionKeyword, star, name, parameters, body);
  }

  @Override
  public void accept(DoubleDispatchVisitor visitor) {
    visitor.visitFunctionExpression(this);
  }

  @Override
  public TypeSet types() {
    TypeSet set = TypeSet.emptyTypeSet();
    set.add(functionType);
    return set;
  }

  @Override
  public void add(Type type) {
    throw new UnsupportedOperationException();
  }
}
