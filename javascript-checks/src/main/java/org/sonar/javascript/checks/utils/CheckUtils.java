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
package org.sonar.javascript.checks.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;

public class CheckUtils {

  private CheckUtils() {
  }

  public static final ImmutableSet<Kind> ASSIGNMENT_EXPRESSION = ImmutableSet.of(
    Kind.ASSIGNMENT,
    Kind.MULTIPLY_ASSIGNMENT,
    Kind.DIVIDE_ASSIGNMENT,
    Kind.REMAINDER_ASSIGNMENT,
    Kind.PLUS_ASSIGNMENT,
    Kind.MINUS_ASSIGNMENT,
    Kind.LEFT_SHIFT_ASSIGNMENT,
    Kind.RIGHT_SHIFT_ASSIGNMENT,
    Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Kind.AND_ASSIGNMENT,
    Kind.XOR_ASSIGNMENT,
    Kind.OR_ASSIGNMENT);

  public static final ImmutableSet<Kind> RELATIONAL_EXPRESSION = ImmutableSet.of(
    Kind.LESS_THAN,
    Kind.GREATER_THAN,
    Kind.LESS_THAN_OR_EQUAL_TO,
    Kind.GREATER_THAN_OR_EQUAL_TO,
    Kind.INSTANCE_OF,
    Kind.RELATIONAL_IN);

  public static final ImmutableSet<Kind> POSTFIX_EXPRESSION = ImmutableSet.of(
    Kind.POSTFIX_INCREMENT,
    Kind.POSTFIX_DECREMENT);

  public static final ImmutableSet<Kind> PREFIX_EXPRESSION = ImmutableSet.of(
    Kind.DELETE,
    Kind.VOID,
    Kind.TYPEOF,
    Kind.PREFIX_INCREMENT,
    Kind.PREFIX_DECREMENT,
    Kind.UNARY_PLUS,
    Kind.UNARY_MINUS,
    Kind.BITWISE_COMPLEMENT,
    Kind.LOGICAL_COMPLEMENT);

  public static final ImmutableSet<Kind> EQUALITY_EXPRESSION = ImmutableSet.of(
    Kind.EQUAL_TO,
    Kind.NOT_EQUAL_TO,
    Kind.STRICT_EQUAL_TO,
    Kind.STRICT_NOT_EQUAL_TO);

  public static final ImmutableSet<Kind> ITERATION_STATEMENTS = ImmutableSet.of(
    Kind.DO_WHILE_STATEMENT,
    Kind.WHILE_STATEMENT,
    Kind.FOR_IN_STATEMENT,
    Kind.FOR_OF_STATEMENT,
    Kind.FOR_STATEMENT);

  public static final ImmutableSet<Kind> FUNCTION_NODES = ImmutableSet.of(
    Kind.FUNCTION_EXPRESSION,
    Kind.FUNCTION_DECLARATION,
    Kind.METHOD,
    Kind.SET_METHOD,
    Kind.GET_METHOD,
    Kind.GENERATOR_METHOD,
    Kind.GENERATOR_DECLARATION,
    Kind.GENERATOR_FUNCTION_EXPRESSION,
    Kind.ARROW_FUNCTION);

  public static Kind[] functionNodesArray() {
    return FUNCTION_NODES.toArray(new Kind[FUNCTION_NODES.size()]);
  }

  public static Kind[] iterationStatementsArray() {
    return ITERATION_STATEMENTS.toArray(new Kind[ITERATION_STATEMENTS.size()]);
  }

  public static String asString(Tree tree) {
    if (tree.is(Kind.TOKEN)) {
      return ((SyntaxToken) tree).text();

    } else {
      StringBuilder sb = new StringBuilder();
      Iterator<Tree> treeIterator = ((JavaScriptTree) tree).childrenIterator();
      SyntaxToken prevToken = null;

      while (treeIterator.hasNext()) {
        Tree child = treeIterator.next();

        if (child != null) {
          appendChild(sb, prevToken, child);
          prevToken = ((JavaScriptTree) child).getLastToken();
        }
      }
      return sb.toString();
    }
  }

  private static void appendChild(StringBuilder sb, @Nullable SyntaxToken prevToken, Tree child) {
    if (prevToken != null) {
      SyntaxToken firstToken = ((JavaScriptTree) child).getFirstToken();
      if (isSpaceRequired(prevToken, firstToken)) {
        sb.append(" ");
      }
    }
    sb.append(asString(child));
  }

  private static boolean isSpaceRequired(SyntaxToken prevToken, SyntaxToken token) {
    return (token.line() > prevToken.line()) || (prevToken.column() + prevToken.text().length() < token.column());
  }

  public static ExpressionTree removeParenthesis(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.PARENTHESISED_EXPRESSION)) {
      return removeParenthesis(((ParenthesisedExpressionTree) expressionTree).expression());
    }
    return expressionTree;
  }

}

