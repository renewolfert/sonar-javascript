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
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.javascript.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2432",
  name = "Setters should not return values",
  priority = Priority.CRITICAL,
  tags = {Tags.BUG})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
public class ReturnInSetterCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Remove this return statement.";

  private final DoubleDispatchVisitorCheck forbiddenReturnVisitor = new ForbiddenReturnVisitor();

  @Override
  public void visitMethodDeclaration(MethodDeclarationTree tree) {
    if (tree.is(Tree.Kind.SET_METHOD)) {
      tree.body().accept(forbiddenReturnVisitor);
    }
    super.visitMethodDeclaration(tree);
  }

  private class ForbiddenReturnVisitor extends DoubleDispatchVisitorCheck {

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (tree.expression() != null) {
        ReturnInSetterCheck check = ReturnInSetterCheck.this;
        check.addLineIssue(tree, MESSAGE);
      }
      super.visitReturnStatement(tree);
    }

  }

}
