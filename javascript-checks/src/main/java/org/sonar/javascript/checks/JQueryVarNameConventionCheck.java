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

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2713",
  name = "JQuery cache variables should comply with a convention name",
  priority = Priority.MINOR,
  tags = {Tags.JQUERY, Tags.CONVENTION})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class JQueryVarNameConventionCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Rename variable \"%s\" to match the regular expression %s.";

  private static final String DEFAULT_FORMAT = "^\\$[a-z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the variable names against",
    defaultValue = "" + DEFAULT_FORMAT)
  private String format = DEFAULT_FORMAT;

  public void setFormat(String format) {
    this.format = format;
  }

  @Override
  public void visitScript(ScriptTree tree) {
    Pattern pattern = Pattern.compile(format);
    SymbolModel symbolModel = getContext().getSymbolModel();
    for (Symbol symbol : symbolModel.getSymbols(Symbol.Kind.VARIABLE)) {
      boolean onlyJQuerySelectorType = symbol.types().containsOnly(Type.Kind.JQUERY_SELECTOR_OBJECT);
      if (!symbol.builtIn() && onlyJQuerySelectorType && !pattern.matcher(symbol.name()).matches()) {
        raiseIssuesOnDeclarations(symbol, String.format(MESSAGE, symbol.name(), format));
      }
    }
  }

  protected void raiseIssuesOnDeclarations(Symbol symbol, String message) {
    Preconditions.checkArgument(!symbol.builtIn());

    boolean issueRaised = false;
    for (Usage usage : symbol.usages()) {
      if (usage.isDeclaration()) {
        newIssue(usage.identifierTree(), message);
        issueRaised = true;
      }
    }

    if (!issueRaised) {
      newIssue(symbol.usages().iterator().next().identifierTree(), message);
    }

  }

}
