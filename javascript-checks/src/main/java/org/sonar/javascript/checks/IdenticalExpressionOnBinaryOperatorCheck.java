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
package org.sonar.javascript.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.tree.SyntacticEquivalence;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.LiteralTree;
import org.sonar.plugins.javascript.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1764",
  name = "Identical expressions should not be used on both sides of a binary operator",
  priority = Priority.CRITICAL,
  tags = {Tags.BUG, Tags.CERT})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("2min")
public class IdenticalExpressionOnBinaryOperatorCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Correct one of the identical sub-expressions on both sides of operator \"%s\"";

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (!tree.is(Kind.MULTIPLY, Kind.PLUS, Kind.ASSIGNMENT)
      && SyntacticEquivalence.areEquivalent(tree.leftOperand(), tree.rightOperand()) && isExcluded(tree)) {

      String message = String.format(MESSAGE, tree.operator().text());
      newIssue(tree.rightOperand(), message)
        .secondary(tree.leftOperand());
    }

    super.visitBinaryExpression(tree);
  }

  private static boolean isExcluded(BinaryExpressionTree tree) {
    return !isOneOntoOneShifting(tree) && !isPotentialNanComparison(tree);
  }

  private static boolean isPotentialNanComparison(BinaryExpressionTree tree) {
    return tree.is(Kind.STRICT_NOT_EQUAL_TO, Kind.STRICT_EQUAL_TO)
      && (tree.leftOperand().is(
      Kind.IDENTIFIER_REFERENCE,
      Kind.IDENTIFIER,
      Kind.BRACKET_MEMBER_EXPRESSION,
      Kind.DOT_MEMBER_EXPRESSION) || tree.leftOperand() instanceof UnaryExpressionTree);

  }

  private static boolean isOneOntoOneShifting(BinaryExpressionTree tree) {
    return tree.is(Kind.LEFT_SHIFT)
      && tree.leftOperand().is(Kind.NUMERIC_LITERAL)
      && "1".equals(((LiteralTree) tree.leftOperand()).value());
  }

}
