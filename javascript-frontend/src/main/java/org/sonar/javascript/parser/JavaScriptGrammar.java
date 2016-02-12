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
package org.sonar.javascript.parser;

import com.sonar.sslr.api.typed.GrammarBuilder;
import org.sonar.javascript.lexer.JavaScriptKeyword;
import org.sonar.javascript.lexer.JavaScriptPunctuator;
import org.sonar.javascript.lexer.JavaScriptTokenType;
import org.sonar.javascript.tree.impl.SeparatedList;
import org.sonar.javascript.tree.impl.declaration.ArrayBindingPatternTreeImpl;
import org.sonar.javascript.tree.impl.declaration.DefaultExportDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.FromClauseTreeImpl;
import org.sonar.javascript.tree.impl.declaration.FunctionDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ImportClauseTreeImpl;
import org.sonar.javascript.tree.impl.declaration.InitializedBindingElementTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ModuleTreeImpl;
import org.sonar.javascript.tree.impl.declaration.NamedExportDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ObjectBindingPatternTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ParameterListTreeImpl;
import org.sonar.javascript.tree.impl.declaration.ScriptTreeImpl;
import org.sonar.javascript.tree.impl.declaration.SpecifierListTreeImpl;
import org.sonar.javascript.tree.impl.declaration.SpecifierTreeImpl;
import org.sonar.javascript.tree.impl.expression.ArrayLiteralTreeImpl;
import org.sonar.javascript.tree.impl.expression.ArrowFunctionTreeImpl;
import org.sonar.javascript.tree.impl.expression.BracketMemberExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.ClassTreeImpl;
import org.sonar.javascript.tree.impl.expression.ComputedPropertyNameTreeImpl;
import org.sonar.javascript.tree.impl.expression.DotMemberExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.FunctionExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.IdentifierTreeImpl;
import org.sonar.javascript.tree.impl.expression.LiteralTreeImpl;
import org.sonar.javascript.tree.impl.expression.ObjectLiteralTreeImpl;
import org.sonar.javascript.tree.impl.expression.PairPropertyTreeImpl;
import org.sonar.javascript.tree.impl.expression.ParenthesisedExpressionTreeImpl;
import org.sonar.javascript.tree.impl.expression.RestElementTreeImpl;
import org.sonar.javascript.tree.impl.expression.SuperTreeImpl;
import org.sonar.javascript.tree.impl.expression.TaggedTemplateTreeImpl;
import org.sonar.javascript.tree.impl.expression.YieldExpressionTreeImpl;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.javascript.tree.impl.statement.BlockTreeImpl;
import org.sonar.javascript.tree.impl.statement.BreakStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.CaseClauseTreeImpl;
import org.sonar.javascript.tree.impl.statement.CatchBlockTreeImpl;
import org.sonar.javascript.tree.impl.statement.ContinueStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.DebuggerStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.DefaultClauseTreeImpl;
import org.sonar.javascript.tree.impl.statement.DoWhileStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ElseClauseTreeImpl;
import org.sonar.javascript.tree.impl.statement.EmptyStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ExpressionStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ForInStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ForOfStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ForStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.IfStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.LabelledStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ReturnStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.SwitchStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.ThrowStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.TryStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.VariableDeclarationTreeImpl;
import org.sonar.javascript.tree.impl.statement.VariableStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.WhileStatementTreeImpl;
import org.sonar.javascript.tree.impl.statement.WithStatementTreeImpl;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.DeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.ExportDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.ImportModuleDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.NameSpaceExportDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.SpecifierListTree;
import org.sonar.plugins.javascript.api.tree.declaration.SpecifierTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.MemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateCharactersTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateLiteralTree;
import org.sonar.plugins.javascript.api.tree.statement.DebuggerStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;

public class JavaScriptGrammar {

  private final GrammarBuilder<InternalSyntaxToken> b;
  private final TreeFactory f;

  public JavaScriptGrammar(GrammarBuilder<InternalSyntaxToken> b, TreeFactory f) {
    this.b = b;
    this.f = f;
  }

  /**
   * A.4 Statement
   */

  public EmptyStatementTreeImpl EMPTY_STATEMENT() {
    return b.<EmptyStatementTreeImpl>nonterminal(Kind.EMPTY_STATEMENT)
      .is(f.emptyStatement(b.token(JavaScriptPunctuator.SEMI)));
  }

  public DebuggerStatementTree DEBUGGER_STATEMENT() {
    return b.<DebuggerStatementTreeImpl>nonterminal(Kind.DEBUGGER_STATEMENT)
      .is(f.debuggerStatement(b.token(JavaScriptKeyword.DEBUGGER), b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public VariableStatementTreeImpl VARIABLE_STATEMENT() {
    return b.<VariableStatementTreeImpl>nonterminal(Kind.VARIABLE_STATEMENT)
      .is(f.variableStatement(VARIABLE_DECLARATION(), b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public VariableDeclarationTreeImpl VARIABLE_DECLARATION() {
    return b.<VariableDeclarationTreeImpl>nonterminal()
      .is(
        f.variableDeclaration1(
          b.firstOf(
            b.token(JavaScriptKeyword.VAR),
            b.token(JavaScriptLegacyGrammar.LET),
            b.token(JavaScriptKeyword.CONST)),
          BINDING_ELEMENT_LIST()));
  }

  public VariableDeclarationTreeImpl VARIABLE_DECLARATION_NO_IN() {
    return b.<VariableDeclarationTreeImpl>nonterminal()
      .is(
        f.variableDeclaration2(
          b.firstOf(
            b.token(JavaScriptKeyword.VAR),
            b.token(JavaScriptLegacyGrammar.LET),
            b.token(JavaScriptKeyword.CONST)),
          BINDING_ELEMENT_NO_IN_LIST()));
  }

  public SeparatedList<BindingElementTree> BINDING_ELEMENT_LIST() {
    return b.<SeparatedList<BindingElementTree>>nonterminal()
      .is(f.bindingElementList1(BINDING_ELEMENT(), b.zeroOrMore(f.newTuple1(b.token(JavaScriptPunctuator.COMMA), BINDING_ELEMENT()))));
  }

  public SeparatedList<BindingElementTree> BINDING_ELEMENT_NO_IN_LIST() {
    return b.<SeparatedList<BindingElementTree>>nonterminal()
      .is(f.bindingElementList2(BINDING_ELEMENT_NO_IN(), b.zeroOrMore(f.newTuple30(b.token(JavaScriptPunctuator.COMMA), BINDING_ELEMENT_NO_IN()))));
  }

  public LabelledStatementTreeImpl LABELLED_STATEMENT() {
    return b.<LabelledStatementTreeImpl>nonterminal(Kind.LABELLED_STATEMENT)
      .is(f.labelledStatement(LABEL_IDENTIFIER(), b.token(JavaScriptPunctuator.COLON), STATEMENT()));
  }

  public ContinueStatementTreeImpl CONTINUE_STATEMENT() {
    return b.<ContinueStatementTreeImpl>nonterminal(Kind.CONTINUE_STATEMENT)
      .is(f.completeContinueStatement(
        b.token(JavaScriptKeyword.CONTINUE),
        b.firstOf(
          CONTINUE_WITH_LABEL(),
          CONTINUE_WITHOUT_LABEL())
      ));
  }

  public ContinueStatementTreeImpl CONTINUE_WITH_LABEL() {
    return b.<ContinueStatementTreeImpl>nonterminal()
      .is(f.newContinueWithLabel(
        IDENTIFIER_NO_LB(),
        b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public ContinueStatementTreeImpl CONTINUE_WITHOUT_LABEL() {
    return b.<ContinueStatementTreeImpl>nonterminal()
      .is(f.newContinueWithoutLabel(b.token(JavaScriptLegacyGrammar.EOS_NO_LB)));
  }

  public BreakStatementTreeImpl BREAK_STATEMENT() {
    return b.<BreakStatementTreeImpl>nonterminal(Kind.BREAK_STATEMENT)
      .is(f.completeBreakStatement(
        b.token(JavaScriptKeyword.BREAK),
        b.firstOf(
          BREAK_WITH_LABEL(),
          BREAK_WITHOUT_LABEL())
      ));
  }

  public BreakStatementTreeImpl BREAK_WITH_LABEL() {
    return b.<BreakStatementTreeImpl>nonterminal()
      .is(f.newBreakWithLabel(
        IDENTIFIER_NO_LB(),
        b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public BreakStatementTreeImpl BREAK_WITHOUT_LABEL() {
    return b.<BreakStatementTreeImpl>nonterminal()
      .is(f.newBreakWithoutLabel(b.token(JavaScriptLegacyGrammar.EOS_NO_LB)));
  }

  public ReturnStatementTreeImpl RETURN_STATEMENT() {
    return b.<ReturnStatementTreeImpl>nonterminal(Kind.RETURN_STATEMENT)
      .is(f.completeReturnStatement(
        b.token(JavaScriptKeyword.RETURN),
        b.firstOf(
          RETURN_WITH_EXPRESSION(),
          RETURN_WITHOUT_EXPRESSION())
      ));
  }

  public ReturnStatementTreeImpl RETURN_WITH_EXPRESSION() {
    return b.<ReturnStatementTreeImpl>nonterminal()
      .is(
        f.newReturnWithExpression(
          EXPRESSION_NO_LINE_BREAK(),
          b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public ReturnStatementTreeImpl RETURN_WITHOUT_EXPRESSION() {
    return b.<ReturnStatementTreeImpl>nonterminal()
      .is(f.newReturnWithoutExpression(b.token(JavaScriptLegacyGrammar.EOS_NO_LB)));
  }

  public ThrowStatementTreeImpl THROW_STATEMENT() {
    return b.<ThrowStatementTreeImpl>nonterminal(Kind.THROW_STATEMENT)
      .is(
        f.newThrowStatement(
          b.token(JavaScriptKeyword.THROW),
          EXPRESSION_NO_LINE_BREAK(),
          b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public WithStatementTreeImpl WITH_STATEMENT() {
    return b.<WithStatementTreeImpl>nonterminal(Kind.WITH_STATEMENT)
      .is(f.newWithStatement(
        b.token(JavaScriptKeyword.WITH),
        b.token(JavaScriptPunctuator.LPARENTHESIS),
        EXPRESSION(),
        b.token(JavaScriptPunctuator.RPARENTHESIS),
        STATEMENT()));
  }

  public BlockTreeImpl BLOCK() {
    return b.<BlockTreeImpl>nonterminal(Kind.BLOCK)
      .is(f.newBlock(
        b.token(JavaScriptPunctuator.LCURLYBRACE),
        b.optional(b.oneOrMore(STATEMENT())),
        b.token(JavaScriptPunctuator.RCURLYBRACE)));
  }

  public TryStatementTreeImpl TRY_STATEMENT() {
    return b.<TryStatementTreeImpl>nonterminal(Kind.TRY_STATEMENT)
      .is(f.completeTryStatement(
        b.token(JavaScriptKeyword.TRY),
        BLOCK(),
        b.firstOf(
          f.newTryStatementWithCatch(CATCH_CLAUSE(), b.optional(FINALLY_CLAUSE())),
          FINALLY_CLAUSE())
      ));
  }

  public TryStatementTreeImpl FINALLY_CLAUSE() {
    return b.<TryStatementTreeImpl>nonterminal(JavaScriptLegacyGrammar.FINALLY)
      .is(f.newTryStatementWithFinally(b.token(JavaScriptKeyword.FINALLY), BLOCK()));
  }

  public CatchBlockTreeImpl CATCH_CLAUSE() {
    return b.<CatchBlockTreeImpl>nonterminal(Kind.CATCH_BLOCK)
      .is(f.newCatchBlock(
        b.token(JavaScriptKeyword.CATCH),
        b.token(JavaScriptPunctuator.LPARENTHESIS),
        b.firstOf(
          BINDING_IDENTIFIER(),
          BINDING_PATTERN()
        ),
        b.token(JavaScriptPunctuator.RPARENTHESIS),
        BLOCK()));
  }

  public SwitchStatementTreeImpl SWITCH_STATEMENT() {
    return b.<SwitchStatementTreeImpl>nonterminal(Kind.SWITCH_STATEMENT)
      .is(f.completeSwitchStatement(
        b.token(JavaScriptKeyword.SWITCH),
        b.token(JavaScriptPunctuator.LPARENTHESIS),
        EXPRESSION(),
        b.token(JavaScriptPunctuator.RPARENTHESIS),
        CASE_BLOCK()));
  }

  public SwitchStatementTreeImpl CASE_BLOCK() {
    return b.<SwitchStatementTreeImpl>nonterminal()
      .is(f.newSwitchStatement(
        b.token(JavaScriptPunctuator.LCURLYBRACE),
        b.zeroOrMore(CASE_CLAUSE()),
        b.optional(f.newTuple2(DEFAULT_CLAUSE(), b.zeroOrMore(CASE_CLAUSE()))),
        b.token(JavaScriptPunctuator.RCURLYBRACE)));
  }

  public CaseClauseTreeImpl CASE_CLAUSE() {
    return b.<CaseClauseTreeImpl>nonterminal(Kind.CASE_CLAUSE)
      .is(
        f.caseClause(
          b.token(JavaScriptKeyword.CASE),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.COLON),
          b.optional(b.oneOrMore(STATEMENT()))));
  }

  public DefaultClauseTreeImpl DEFAULT_CLAUSE() {
    return b.<DefaultClauseTreeImpl>nonterminal(Kind.DEFAULT_CLAUSE)
      .is(f.defaultClause(
        b.token(JavaScriptKeyword.DEFAULT),
        b.token(JavaScriptPunctuator.COLON),
        b.optional(b.oneOrMore(STATEMENT()))));
  }

  public IfStatementTreeImpl IF_STATEMENT() {
    return b.<IfStatementTreeImpl>nonterminal(Kind.IF_STATEMENT)
      .is(
        f.ifStatement(
          b.token(JavaScriptKeyword.IF),
          b.token(JavaScriptPunctuator.LPARENTHESIS),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RPARENTHESIS),
          STATEMENT(),
          b.optional(ELSE_CLAUSE())));
  }

  public ElseClauseTreeImpl ELSE_CLAUSE() {
    return b.<ElseClauseTreeImpl>nonterminal(Kind.ELSE_CLAUSE)
      .is(f.elseClause(
        b.token(JavaScriptKeyword.ELSE),
        STATEMENT()));
  }

  public WhileStatementTreeImpl WHILE_STATEMENT() {
    return b.<WhileStatementTreeImpl>nonterminal(Kind.WHILE_STATEMENT)
      .is(
        f.whileStatement(
          b.token(JavaScriptKeyword.WHILE),
          b.token(JavaScriptPunctuator.LPARENTHESIS),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RPARENTHESIS),
          STATEMENT()));
  }

  public DoWhileStatementTreeImpl DO_WHILE_STATEMENT() {
    return b.<DoWhileStatementTreeImpl>nonterminal(Kind.DO_WHILE_STATEMENT)
      .is(
        f.doWhileStatement(
          b.token(JavaScriptKeyword.DO),
          STATEMENT(),
          b.token(JavaScriptKeyword.WHILE),
          b.token(JavaScriptPunctuator.LPARENTHESIS),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RPARENTHESIS),
          b.token(JavaScriptLegacyGrammar.EOS)));
  }

  public ExpressionStatementTreeImpl EXPRESSION_STATEMENT() {
    return b.<ExpressionStatementTreeImpl>nonterminal(Kind.EXPRESSION_STATEMENT)
      .is(f.expressionStatement(b.token(JavaScriptLegacyGrammar.NEXT_NOT_LCURLY_AND_FUNCTION), EXPRESSION(), b.token(JavaScriptLegacyGrammar.EOS)));
  }

  /**
   * ECMAScript 6
   */
  public ForOfStatementTreeImpl FOR_OF_STATEMENT() {
    return b.<ForOfStatementTreeImpl>nonterminal(Kind.FOR_OF_STATEMENT)
      .is(f.forOfStatement(
        b.token(JavaScriptKeyword.FOR),
        b.token(JavaScriptPunctuator.LPARENTHESIS),
        b.firstOf(
          VARIABLE_DECLARATION(),
          f.skipLookahead3(b.token(JavaScriptLegacyGrammar.NEXT_NOT_LET), LEFT_HAND_SIDE_EXPRESSION())),
        b.token(JavaScriptLegacyGrammar.OF),
        ASSIGNMENT_EXPRESSION(),
        b.token(JavaScriptPunctuator.RPARENTHESIS),
        STATEMENT()));
  }

  public ForInStatementTreeImpl FOR_IN_STATEMENT() {
    return b.<ForInStatementTreeImpl>nonterminal(Kind.FOR_IN_STATEMENT)
      .is(
        f.forInStatement(
          b.token(JavaScriptKeyword.FOR),
          b.token(JavaScriptPunctuator.LPARENTHESIS),
          b.firstOf(
            VARIABLE_DECLARATION(),
            f.skipLookahead2(b.token(JavaScriptLegacyGrammar.NEXT_NOT_LET_AND_BRACKET), LEFT_HAND_SIDE_EXPRESSION())),
          b.token(JavaScriptKeyword.IN),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RPARENTHESIS),
          STATEMENT()));
  }

  public ForStatementTreeImpl FOR_STATEMENT() {
    return b.<ForStatementTreeImpl>nonterminal(Kind.FOR_STATEMENT)
      .is(
        f.forStatement(
          b.token(JavaScriptKeyword.FOR),
          b.token(JavaScriptPunctuator.LPARENTHESIS),

          b.optional(
            b.firstOf(
              VARIABLE_DECLARATION_NO_IN(),
              f.skipLookahead1(b.token(JavaScriptLegacyGrammar.NEXT_NOT_LET_AND_BRACKET), EXPRESSION_NO_IN()))),
          b.token(JavaScriptPunctuator.SEMI),

          b.optional(EXPRESSION()),
          b.token(JavaScriptPunctuator.SEMI),

          b.optional(EXPRESSION()),
          b.token(JavaScriptPunctuator.RPARENTHESIS),
          STATEMENT()));
  }

  public StatementTree ITERATION_STATEMENT() {
    return b.<StatementTree>nonterminal(JavaScriptLegacyGrammar.ITERATION_STATEMENT)
      .is(
        b.firstOf(
          DO_WHILE_STATEMENT(),
          WHILE_STATEMENT(),
          FOR_IN_STATEMENT(),
          ES6(FOR_OF_STATEMENT()),
          FOR_STATEMENT()));
  }

  public StatementTree STATEMENT() {
    return b.<StatementTree>nonterminal(JavaScriptLegacyGrammar.STATEMENT)
      .is(
        b.firstOf(
          BLOCK(),
          VARIABLE_STATEMENT(),
          EMPTY_STATEMENT(),
          LABELLED_STATEMENT(),
          CLASS_DECLARATION(),
          EXPRESSION_STATEMENT(),
          IF_STATEMENT(),
          ITERATION_STATEMENT(),
          CONTINUE_STATEMENT(),
          BREAK_STATEMENT(),
          RETURN_STATEMENT(),
          WITH_STATEMENT(),
          SWITCH_STATEMENT(),
          THROW_STATEMENT(),
          TRY_STATEMENT(),
          DEBUGGER_STATEMENT(),
          FUNCTION_AND_GENERATOR_DECLARATION()));
  }

  /**
   * A.4 [END] Statement
   */

  /**
   * A.3 Expressions
   */

  public LiteralTreeImpl LITERAL() {
    return b.<LiteralTreeImpl>nonterminal(JavaScriptLegacyGrammar.LITERAL)
      .is(b.firstOf(
        f.nullLiteral(b.token(JavaScriptKeyword.NULL)),
        f.booleanLiteral(b.firstOf(b.token(JavaScriptKeyword.TRUE), b.token(JavaScriptKeyword.FALSE))),
        NUMERIC_LITERAL(),
        STRING_LITERAL(),
        f.regexpLiteral(b.token(JavaScriptTokenType.REGULAR_EXPRESSION_LITERAL))));
  }

  public LiteralTreeImpl NUMERIC_LITERAL() {
    return b.<LiteralTreeImpl>nonterminal(Kind.NUMERIC_LITERAL)
      .is(f.numericLiteral(b.token(JavaScriptTokenType.NUMERIC_LITERAL)));
  }

  public LiteralTreeImpl STRING_LITERAL() {
    return b.<LiteralTreeImpl>nonterminal(Kind.STRING_LITERAL)
      .is(f.stringLiteral(b.token(JavaScriptLegacyGrammar.STRING_LITERAL)));
  }

  public ExpressionTree ARRAY_LITERAL_ELEMENT() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.ARRAY_LITERAL_ELEMENT)
      .is(f.arrayInitialiserElement(b.optional(b.token(JavaScriptPunctuator.ELLIPSIS)), ASSIGNMENT_EXPRESSION()));
  }

  public ArrayLiteralTreeImpl ARRAY_ELEMENT_LIST() {
    return b.<ArrayLiteralTreeImpl>nonterminal(JavaScriptLegacyGrammar.ELEMENT_LIST)
      .is(f.newArrayLiteralWithElements(
        b.zeroOrMore(b.token(JavaScriptPunctuator.COMMA)),
        ARRAY_LITERAL_ELEMENT(),
        b.zeroOrMore(
          f.newTuple3(b.oneOrMore(b.token(JavaScriptPunctuator.COMMA)), ARRAY_LITERAL_ELEMENT())),
        b.zeroOrMore(b.token(JavaScriptPunctuator.COMMA))
      ));
  }

  public ParameterListTreeImpl FORMAL_PARAMETER_LIST() {
    return b.<ParameterListTreeImpl>nonterminal(Kind.FORMAL_PARAMETER_LIST)
      .is(f.completeFormalParameterList(
        b.token(JavaScriptPunctuator.LPARENTHESIS),
        b.optional(b.firstOf(
          f.newFormalParameterList(
            BINDING_ELEMENT(),
            b.zeroOrMore(f.newTuple4(b.token(JavaScriptPunctuator.COMMA), BINDING_ELEMENT())),
            b.optional(ES6(f.newTuple5(b.token(JavaScriptPunctuator.COMMA), BINDING_REST_ELEMENT())))),
          ES6(f.newFormalRestParameterList(BINDING_REST_ELEMENT()))
        )),
        b.token(JavaScriptPunctuator.RPARENTHESIS)
      ));
  }

  /**
   * ECMAScript 6
   */
  public RestElementTreeImpl BINDING_REST_ELEMENT() {
    return b.<RestElementTreeImpl>nonterminal(JavaScriptLegacyGrammar.BINDING_REST_ELEMENT)
      .is(f.bindingRestElement(b.token(JavaScriptPunctuator.ELLIPSIS), BINDING_IDENTIFIER()));
  }

  public ArrayLiteralTreeImpl ARRAY_LITERAL() {
    return b.<ArrayLiteralTreeImpl>nonterminal(Kind.ARRAY_LITERAL)
      .is(f.completeArrayLiteral(
        b.token(JavaScriptPunctuator.LBRACKET),
        b.optional(b.firstOf(
          ARRAY_ELEMENT_LIST(),
          f.newArrayLiteralWithElidedElements(b.oneOrMore(b.token(JavaScriptPunctuator.COMMA))))),
        b.token(JavaScriptPunctuator.RBRACKET)
      ));
  }

  /**
   * ECMAScript 6
   */
  public FunctionExpressionTreeImpl GENERATOR_EXPRESSION() {
    return b.<FunctionExpressionTreeImpl>nonterminal(Kind.GENERATOR_FUNCTION_EXPRESSION)
      .is(
        f.generatorExpression(
          b.token(JavaScriptKeyword.FUNCTION),
          b.token(JavaScriptPunctuator.STAR),
          b.optional(BINDING_IDENTIFIER()),
          FORMAL_PARAMETER_LIST(),
          BLOCK()));
  }

  public FunctionExpressionTreeImpl FUNCTION_EXPRESSION() {
    return b.<FunctionExpressionTreeImpl>nonterminal(Kind.FUNCTION_EXPRESSION)
      .is(
        f.functionExpression(
          b.token(JavaScriptKeyword.FUNCTION),
          b.optional(b.token(JavaScriptTokenType.IDENTIFIER)),
          FORMAL_PARAMETER_LIST(),
          BLOCK()));
  }

  public ExpressionTree CONDITIONAL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.CONDITIONAL_EXPRESSION)
      .is(f.completeConditionalExpression(
        CONDITIONAL_OR_EXPRESSION(),
        b.optional(f.newConditionalExpression(
          b.token(JavaScriptPunctuator.QUERY),
          ASSIGNMENT_EXPRESSION(),
          b.token(JavaScriptPunctuator.COLON),
          ASSIGNMENT_EXPRESSION()))
      ));
  }

  public ExpressionTree CONDITIONAL_EXPRESSION_NOT_ES6_ASSIGNMENT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal()
      .is(f.skipLookahead4(CONDITIONAL_EXPRESSION(), b.token(JavaScriptLegacyGrammar.NEXT_NOT_ES6_ASSIGNMENT_EXPRESSION)));
  }

  public ExpressionTree CONDITIONAL_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.completeConditionalExpressionNoIn(
        CONDITIONAL_OR_EXPRESSION_NO_IN(),
        b.optional(f.newConditionalExpressionNoIn(
          b.token(JavaScriptPunctuator.QUERY),
          ASSIGNMENT_EXPRESSION(),
          b.token(JavaScriptPunctuator.COLON),
          ASSIGNMENT_EXPRESSION_NO_IN()
        ))));
  }

  public ExpressionTree CONDITIONAL_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.CONDITIONAL_OR)
      .is(f.newConditionalOr(
        CONDITIONAL_AND_EXPRESSION(),
        b.zeroOrMore(f.newTuple6(
          b.token(JavaScriptPunctuator.OROR),
          CONDITIONAL_AND_EXPRESSION()
        ))
      ));
  }

  public ExpressionTree CONDITIONAL_OR_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newConditionalOrNoIn(
        CONDITIONAL_AND_EXPRESSION_NO_IN(),
        b.zeroOrMore(f.newTuple19(
          b.token(JavaScriptPunctuator.OROR),
          CONDITIONAL_AND_EXPRESSION_NO_IN()
        ))
      ));
  }

  public ExpressionTree CONDITIONAL_AND_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.CONDITIONAL_AND)
      .is(f.newConditionalAnd(
        BITWISE_OR_EXPRESSION(),
        b.zeroOrMore(f.newTuple7(
          b.token(JavaScriptPunctuator.ANDAND),
          BITWISE_OR_EXPRESSION()
        ))
      ));
  }

  public ExpressionTree CONDITIONAL_AND_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newConditionalAndNoIn(
        BITWISE_OR_EXPRESSION_NO_IN(),
        b.zeroOrMore(f.newTuple20(
          b.token(JavaScriptPunctuator.ANDAND),
          BITWISE_OR_EXPRESSION_NO_IN()
        ))
      ));
  }

  public ExpressionTree BITWISE_OR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.BITWISE_OR)
      .is(f.newBitwiseOr(
        BITWISE_XOR_EXPRESSION(),
        b.zeroOrMore(f.newTuple8(
          b.token(JavaScriptPunctuator.OR),
          BITWISE_XOR_EXPRESSION()
        ))
      ));
  }

  public ExpressionTree BITWISE_OR_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newBitwiseOrNoIn(
        BITWISE_XOR_EXPRESSION_NO_IN(),
        b.zeroOrMore(f.newTuple21(
          b.token(JavaScriptPunctuator.OR),
          BITWISE_XOR_EXPRESSION_NO_IN()
        ))
      ));
  }

  public ExpressionTree BITWISE_XOR_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.BITWISE_XOR)
      .is(f.newBitwiseXor(
        BITWISE_AND_EXPRESSION(),
        b.zeroOrMore(f.newTuple9(
          b.token(JavaScriptPunctuator.XOR),
          BITWISE_AND_EXPRESSION()
        ))
      ));
  }

  public ExpressionTree BITWISE_XOR_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newBitwiseXorNoIn(
        BITWISE_AND_EXPRESSION_NO_IN(),
        b.zeroOrMore(f.newTuple22(
          b.token(JavaScriptPunctuator.XOR),
          BITWISE_AND_EXPRESSION_NO_IN()
        ))
      ));
  }

  public ExpressionTree BITWISE_AND_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.BITWISE_AND)
      .is(f.newBitwiseAnd(
        EQUALITY_EXPRESSION(),
        b.zeroOrMore(f.newTuple10(
          b.token(JavaScriptPunctuator.AND),
          EQUALITY_EXPRESSION()
        ))
      ));
  }

  public ExpressionTree BITWISE_AND_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newBitwiseAndNoIn(
        EQUALITY_EXPRESSION_NO_IN(),
        b.zeroOrMore(f.newTuple23(
          b.token(JavaScriptPunctuator.AND),
          EQUALITY_EXPRESSION_NO_IN()
        ))
      ));
  }

  public ExpressionTree EQUALITY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.EQUALITY_EXPRESSION)
      .is(f.newEquality(
        RELATIONAL_EXPRESSION(),
        b.zeroOrMore(f.newTuple11(
          b.firstOf(
            b.token(JavaScriptPunctuator.EQUAL),
            b.token(JavaScriptPunctuator.NOTEQUAL),
            b.token(JavaScriptPunctuator.EQUAL2),
            b.token(JavaScriptPunctuator.NOTEQUAL2)),
          RELATIONAL_EXPRESSION()
        ))
        )
      );
  }

  public ExpressionTree EQUALITY_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newEqualityNoIn(
        RELATIONAL_EXPRESSION_NO_IN(),
        b.zeroOrMore(f.newTuple24(
          b.firstOf(
            b.token(JavaScriptPunctuator.EQUAL),
            b.token(JavaScriptPunctuator.NOTEQUAL),
            b.token(JavaScriptPunctuator.EQUAL2),
            b.token(JavaScriptPunctuator.NOTEQUAL2)),
          RELATIONAL_EXPRESSION_NO_IN()
        ))
        )
      );
  }

  public ExpressionTree RELATIONAL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.RELATIONAL_EXPRESSION)
      .is(f.newRelational(
        SHIFT_EXPRESSION(),
        b.zeroOrMore(f.newTuple12(
          b.firstOf(
            b.token(JavaScriptPunctuator.LT),
            b.token(JavaScriptPunctuator.GT),
            b.token(JavaScriptPunctuator.LE),
            b.token(JavaScriptPunctuator.GE),
            b.token(JavaScriptKeyword.INSTANCEOF),
            b.token(JavaScriptKeyword.IN)),
          SHIFT_EXPRESSION()
        ))
        )
      );
  }

  public ExpressionTree RELATIONAL_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(f.newRelationalNoIn(
        SHIFT_EXPRESSION(),
        b.zeroOrMore(f.newTuple25(
          b.firstOf(
            b.token(JavaScriptPunctuator.LT),
            b.token(JavaScriptPunctuator.GT),
            b.token(JavaScriptPunctuator.LE),
            b.token(JavaScriptPunctuator.GE),
            b.token(JavaScriptKeyword.INSTANCEOF)),
          SHIFT_EXPRESSION()
        ))
        )
      );
  }

  public ExpressionTree SHIFT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.SHIFT_EXPRESSION)
      .is(f.newShift(
        ADDITIVE_EXPRESSION(),
        b.zeroOrMore(f.newTuple13(
          b.firstOf(
            b.token(JavaScriptPunctuator.SL),
            b.token(JavaScriptPunctuator.SR),
            b.token(JavaScriptPunctuator.SR2)),
          ADDITIVE_EXPRESSION()
        ))
        )
      );
  }

  public ExpressionTree ADDITIVE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.ADDITIVE_EXPRESSION)
      .is(f.newAdditive(
        MULTIPLICATIVE_EXPRESSION(),
        b.zeroOrMore(f.newTuple14(
          b.firstOf(
            b.token(JavaScriptPunctuator.PLUS),
            b.token(JavaScriptPunctuator.MINUS)),
          MULTIPLICATIVE_EXPRESSION()
        ))
        )
      );
  }

  public ExpressionTree MULTIPLICATIVE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.MULTIPLICATIVE_EXPRESSION)
      .is(f.newMultiplicative(
        UNARY_EXPRESSION(),
        b.zeroOrMore(f.newTuple15(
          b.firstOf(
            b.token(JavaScriptPunctuator.STAR),
            b.token(JavaScriptPunctuator.DIV),
            b.token(JavaScriptPunctuator.MOD)),
          UNARY_EXPRESSION()
        ))
        )
      );
  }

  public ExpressionTree UNARY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.UNARY_EXPRESSION)
      .is(b.firstOf(
        POSTFIX_EXPRESSION(),
        f.prefixExpression(
          b.firstOf(
            b.token(JavaScriptKeyword.DELETE),
            b.token(JavaScriptKeyword.VOID),
            b.token(JavaScriptKeyword.TYPEOF),
            b.token(JavaScriptPunctuator.INC),
            b.token(JavaScriptPunctuator.DEC),
            b.token(JavaScriptPunctuator.PLUS),
            b.token(JavaScriptPunctuator.MINUS),
            b.token(JavaScriptPunctuator.TILDA),
            b.token(JavaScriptPunctuator.BANG)),
          UNARY_EXPRESSION()
        )
      ));
  }

  public ExpressionTree POSTFIX_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.POSTFIX_EXPRESSION)
      .is(f.postfixExpression(
        LEFT_HAND_SIDE_EXPRESSION(),
        b.optional(f.newTuple16(
          b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK),
          b.firstOf(
            b.token(JavaScriptPunctuator.INC),
            b.token(JavaScriptPunctuator.DEC))
        ))
      ));
  }

  public ExpressionTree LEFT_HAND_SIDE_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.LEFT_HAND_SIDE_EXPRESSION)
      .is(
        b.firstOf(
          CALL_EXPRESSION(),
          NEW_EXPRESSION()));
  }

  public YieldExpressionTreeImpl YIELD_EXPRESSION() {
    return b.<YieldExpressionTreeImpl>nonterminal(Kind.YIELD_EXPRESSION)
      .is(f.completeYieldExpression(
        b.token(JavaScriptKeyword.YIELD),
        b.optional(f.newYieldExpression(
          b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK),
          b.optional(b.token(JavaScriptPunctuator.STAR)),
          ASSIGNMENT_EXPRESSION())
        )
      ));
  }

  public YieldExpressionTreeImpl YIELD_EXPRESSION_NO_IN() {
    return b.<YieldExpressionTreeImpl>nonterminal()
      .is(f.completeYieldExpressionNoIn(
        b.token(JavaScriptKeyword.YIELD),
        b.optional(f.newYieldExpressionNoIn(
          b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK),
          b.optional(b.token(JavaScriptPunctuator.STAR)),
          ASSIGNMENT_EXPRESSION_NO_IN())
        )
      ));
  }

  public IdentifierTreeImpl IDENTIFIER_REFERENCE() {
    return b.<IdentifierTreeImpl>nonterminal(JavaScriptLegacyGrammar.IDENTIFIER_REFERENCE)
      .is(f.identifierReference(b.firstOf(
        b.token(JavaScriptKeyword.YIELD),
        b.token(JavaScriptTokenType.IDENTIFIER)))
      );
  }

  public IdentifierTreeImpl BINDING_IDENTIFIER() {
    return b.<IdentifierTreeImpl>nonterminal(JavaScriptLegacyGrammar.BINDING_IDENTIFIER)
      .is(f.bindingIdentifier(b.firstOf(
        b.token(JavaScriptKeyword.YIELD),
        b.token(JavaScriptTokenType.IDENTIFIER)))
      );
  }

  public IdentifierTreeImpl IDENTIFIER_NO_LB() {
    return b.<IdentifierTreeImpl>nonterminal(Kind.IDENTIFIER_NO_LB)
      .is(f.identifierNoLb(
        b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK),
        b.token(JavaScriptTokenType.IDENTIFIER)));
  }

  public IdentifierTreeImpl LABEL_IDENTIFIER() {
    return b.<IdentifierTreeImpl>nonterminal(Kind.LABEL_IDENTIFIER)
      .is(f.labelIdentifier(b.token(JavaScriptTokenType.IDENTIFIER)));
  }

  public IdentifierTreeImpl IDENTIFIER_NAME() {
    return b.<IdentifierTreeImpl>nonterminal()
      .is(f.identifierName(b.token(JavaScriptLegacyGrammar.IDENTIFIER_NAME)));
  }

  public ArrowFunctionTreeImpl ARROW_FUNCTION() {
    return b.<ArrowFunctionTreeImpl>nonterminal(Kind.ARROW_FUNCTION)
      .is(f.arrowFunction(
        b.firstOf(
          BINDING_IDENTIFIER(),
          FORMAL_PARAMETER_LIST()),
        b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK),
        b.token(JavaScriptPunctuator.DOUBLEARROW),
        b.firstOf(
          BLOCK(),
          f.assignmentNoCurly(b.token(JavaScriptLegacyGrammar.NEXT_NOT_LCURLY), ASSIGNMENT_EXPRESSION()))
      ));
  }

  public ArrowFunctionTreeImpl ARROW_FUNCTION_NO_IN() {
    return b.<ArrowFunctionTreeImpl>nonterminal()
      .is(f.arrowFunctionNoIn(
        b.firstOf(
          BINDING_IDENTIFIER(),
          FORMAL_PARAMETER_LIST()),
        b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK),
        b.token(JavaScriptPunctuator.DOUBLEARROW),
        b.firstOf(
          BLOCK(),
          f.assignmentNoCurlyNoIn(b.token(JavaScriptLegacyGrammar.NEXT_NOT_LCURLY), ASSIGNMENT_EXPRESSION_NO_IN()))
      ));
  }

  public ExpressionTree MEMBER_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.MEMBER_EXPRESSION)
      .is(f.completeMemberExpression(
        b.firstOf(
          ES6(SUPER_PROPERTY()),
          f.newExpressionWithArgument(b.token(JavaScriptKeyword.NEW), b.firstOf(ES6(SUPER()), MEMBER_EXPRESSION()), ARGUMENTS()),
          PRIMARY_EXPRESSION()),
        b.zeroOrMore(
          b.firstOf(
            BRACKET_EXPRESSION(),
            OBJECT_PROPERTY_ACCESS(),
            ES6(TAGGED_TEMPLATE())))
      ));
  }

  public MemberExpressionTree SUPER_PROPERTY() {
    return b.<MemberExpressionTree>nonterminal()
      .is(f.completeSuperMemberExpression(
        SUPER(),
        b.firstOf(
          OBJECT_PROPERTY_ACCESS(),
          BRACKET_EXPRESSION())
      ));
  }

  public SuperTreeImpl SUPER() {
    return b.<SuperTreeImpl>nonterminal(Kind.SUPER)
      .is(f.superExpression(b.token(JavaScriptKeyword.SUPER)));
  }

  public MemberExpressionTree OBJECT_PROPERTY_ACCESS() {
    return b.<DotMemberExpressionTreeImpl>nonterminal(Kind.DOT_MEMBER_EXPRESSION)
      .is(f.newDotMemberExpression(
        b.token(JavaScriptPunctuator.DOT),
        IDENTIFIER_NAME()));
  }

  public MemberExpressionTree BRACKET_EXPRESSION() {
    return b.<BracketMemberExpressionTreeImpl>nonterminal(Kind.BRACKET_MEMBER_EXPRESSION)
      .is(
        f.newBracketMemberExpression(
          b.token(JavaScriptPunctuator.LBRACKET),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RBRACKET)));
  }

  public ExpressionTree TAGGED_TEMPLATE() {
    return b.<TaggedTemplateTreeImpl>nonterminal(Kind.TAGGED_TEMPLATE)
      .is(f.newTaggedTemplate(TEMPLATE_LITERAL()));

  }

  public ParameterListTreeImpl ARGUMENTS() {
    return b.<ParameterListTreeImpl>nonterminal(Kind.ARGUMENTS)
      .is(f.completeArguments(
        b.token(JavaScriptPunctuator.LPARENTHESIS),
        b.optional(ARGUMENT_LIST()),
        b.token(JavaScriptPunctuator.RPARENTHESIS)

      ));
  }

  public ParameterListTreeImpl ARGUMENT_LIST() {
    return b.<ParameterListTreeImpl>nonterminal(JavaScriptLegacyGrammar.ARGUMENTS_LIST)
      .is(f.newArgumentList(
        ARGUMENT(),
        b.zeroOrMore(f.newTuple17(b.token(JavaScriptPunctuator.COMMA), ARGUMENT())))
      );
  }

  public ExpressionTree ARGUMENT() {
    return b.<ExpressionTree>nonterminal()
      .is(f.argument(
        b.optional(b.token(JavaScriptPunctuator.ELLIPSIS)),
        ASSIGNMENT_EXPRESSION()));
  }

  public ExpressionTree CALL_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(Kind.CALL_EXPRESSION)
      .is(f.callExpression(
        f.simpleCallExpression(b.firstOf(MEMBER_EXPRESSION(), SUPER()), ARGUMENTS()),
        b.zeroOrMore(b.firstOf(
          ARGUMENTS(),
          BRACKET_EXPRESSION(),
          OBJECT_PROPERTY_ACCESS(),
          ES6(TAGGED_TEMPLATE())
        ))));
  }

  public ParenthesisedExpressionTreeImpl PARENTHESISED_EXPRESSION() {
    return b.<ParenthesisedExpressionTreeImpl>nonterminal(Kind.PARENTHESISED_EXPRESSION)
      .is(
        f.parenthesisedExpression(
          b.token(JavaScriptPunctuator.LPARENTHESIS),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RPARENTHESIS)));
  }

  public ClassTreeImpl CLASS_EXPRESSION() {
    return b.<ClassTreeImpl>nonterminal(Kind.CLASS_EXPRESSION)
      .is(
        f.classExpression(
          b.token(JavaScriptKeyword.CLASS),
          b.optional(BINDING_IDENTIFIER()),
          // TODO Factor the duplication with CLASS_DECLARATION() into CLASS_TRAIT() ?
          b.optional(f.newTuple28(b.token(JavaScriptKeyword.EXTENDS), LEFT_HAND_SIDE_EXPRESSION())),
          b.token(JavaScriptPunctuator.LCURLYBRACE),
          b.zeroOrMore(CLASS_ELEMENT()),
          b.token(JavaScriptPunctuator.RCURLYBRACE)));
  }

  public ComputedPropertyNameTreeImpl COMPUTED_PROPERTY_NAME() {
    return b.<ComputedPropertyNameTreeImpl>nonterminal(Kind.COMPUTED_PROPERTY_NAME)
      .is(f.computedPropertyName(
        b.token(JavaScriptPunctuator.LBRACKET),
        ASSIGNMENT_EXPRESSION(),
        b.token(JavaScriptPunctuator.RBRACKET)
      ));
  }

  public ExpressionTree LITERAL_PROPERTY_NAME() {
    return b.<ExpressionTree>nonterminal()
      .is(b.firstOf(
        IDENTIFIER_NAME(),
        STRING_LITERAL(),
        NUMERIC_LITERAL()
      ));
  }

  public ExpressionTree PROPERTY_NAME() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.PROPERTY_NAME)
      .is(b.firstOf(
        LITERAL_PROPERTY_NAME(),
        ES6(COMPUTED_PROPERTY_NAME())
      ));
  }

  public PairPropertyTreeImpl PAIR_PROPERTY() {
    return b.<PairPropertyTreeImpl>nonterminal(Kind.PAIR_PROPERTY)
      .is(f.pairProperty(
        PROPERTY_NAME(),
        b.token(JavaScriptPunctuator.COLON),
        ASSIGNMENT_EXPRESSION()
      ));
  }

  public Tree PROPERTY_DEFINITION() {
    return b.<Tree>nonterminal(JavaScriptLegacyGrammar.PROPERTY_DEFINITION)
      .is(b.firstOf(
        PAIR_PROPERTY(),
        METHOD_DEFINITION(),
        IDENTIFIER_REFERENCE())
      );
  }

  public ObjectLiteralTreeImpl OBJECT_LITERAL() {
    return b.<ObjectLiteralTreeImpl>nonterminal(Kind.OBJECT_LITERAL)
      .is(f.completeObjectLiteral(
        b.token(JavaScriptPunctuator.LCURLYBRACE),
        b.optional(f.newObjectLiteral(
          PROPERTY_DEFINITION(),
          b.zeroOrMore(f.newTuple18(b.token(JavaScriptPunctuator.COMMA), PROPERTY_DEFINITION())),
          b.optional(b.token(JavaScriptPunctuator.COMMA))
        )),
        b.token(JavaScriptPunctuator.RCURLYBRACE)
      ));
  }

  public ExpressionTree NEW_EXPRESSION() {
    return b.<ExpressionTree>nonterminal()
      .is(b.firstOf(
        MEMBER_EXPRESSION(),
        f.newExpression(b.token(JavaScriptKeyword.NEW), b.firstOf(ES6(SUPER()), NEW_EXPRESSION()))
      ));
  }

  public TemplateLiteralTree TEMPLATE_LITERAL() {
    return b.<TemplateLiteralTree>nonterminal(Kind.TEMPLATE_LITERAL)
      .is(b.firstOf(
        f.noSubstitutionTemplate(b.token(JavaScriptLegacyGrammar.BACKTICK), b.optional(TEMPLATE_CHARACTERS()), b.token(JavaScriptLegacyGrammar.BACKTICK)),
        f.substitutionTemplate(
          b.token(JavaScriptLegacyGrammar.BACKTICK),
          b.optional(TEMPLATE_CHARACTERS()),

          b.zeroOrMore(f.newTuple55(
            TEMPLATE_EXPRESSION(),
            b.optional(TEMPLATE_CHARACTERS())
          )),

          b.token(JavaScriptLegacyGrammar.BACKTICK)
        )
      ));
  }

  public TemplateExpressionTree TEMPLATE_EXPRESSION() {
    return b.<TemplateExpressionTree>nonterminal(Kind.TEMPLATE_EXPRESSION)
      .is(
        f.templateExpression(
          b.token(JavaScriptLegacyGrammar.DOLLAR_SIGN),
          b.token(JavaScriptPunctuator.LCURLYBRACE),
          EXPRESSION(),
          b.token(JavaScriptPunctuator.RCURLYBRACE)));
  }

  public TemplateCharactersTree TEMPLATE_CHARACTERS() {
    return b.<TemplateCharactersTree>nonterminal()
      .is(f.templateCharacters(b.oneOrMore(b.token(JavaScriptLegacyGrammar.TEMPLATE_CHARACTER))));
  }

  public IdentifierTree THIS() {
    return b.<IdentifierTree>nonterminal(Kind.THIS)
      .is(f.thisExpression(b.token(JavaScriptKeyword.THIS)));

  }

  public ExpressionTree PRIMARY_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.PRIMARY_EXPRESSION)
      .is(
        b.firstOf(
          THIS(),
          // Not IDENTIFIER_REFERENCE, to avoid conflicts with YIELD_EXPRESSION from ASSIGNMENT_EXPRESSION
          f.identifierReferenceWithoutYield(b.token(JavaScriptTokenType.IDENTIFIER)),
          LITERAL(),
          ARRAY_LITERAL(),
          OBJECT_LITERAL(),
          FUNCTION_EXPRESSION(),
          PARENTHESISED_EXPRESSION(),
          CLASS_EXPRESSION(),
          GENERATOR_EXPRESSION(),
          TEMPLATE_LITERAL()
        ));
  }

  public ExpressionTree ES6_ASSIGNMENT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal()
      .is(b.firstOf(YIELD_EXPRESSION(), ARROW_FUNCTION()));
  }

  public ExpressionTree ES6_ASSIGNMENT_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal()
      .is(b.firstOf(YIELD_EXPRESSION_NO_IN(), ARROW_FUNCTION_NO_IN()));
  }

  public ExpressionTree ASSIGNMENT_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.ASSIGNMENT_EXPRESSION)
      .is(
        b.firstOf(
          f.assignmentExpression(
            LEFT_HAND_SIDE_EXPRESSION(),
            b.firstOf(
              b.token(JavaScriptPunctuator.EQU),
              b.token(JavaScriptPunctuator.STAR_EQU),
              b.token(JavaScriptPunctuator.DIV_EQU),
              b.token(JavaScriptPunctuator.MOD_EQU),
              b.token(JavaScriptPunctuator.PLUS_EQU),
              b.token(JavaScriptPunctuator.MINUS_EQU),
              b.token(JavaScriptPunctuator.SL_EQU),
              b.token(JavaScriptPunctuator.SR_EQU),
              b.token(JavaScriptPunctuator.SR_EQU2),
              b.token(JavaScriptPunctuator.AND_EQU),
              b.token(JavaScriptPunctuator.XOR_EQU),
              b.token(JavaScriptPunctuator.OR_EQU)),
            ASSIGNMENT_EXPRESSION()),
          CONDITIONAL_EXPRESSION_NOT_ES6_ASSIGNMENT_EXPRESSION(),
          ES6_ASSIGNMENT_EXPRESSION()
        ));
  }

  public ExpressionTree ASSIGNMENT_EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.ASSIGNMENT_EXPRESSION_NO_IN)
      .is(
        b.firstOf(
          f.assignmentExpressionNoIn(
            LEFT_HAND_SIDE_EXPRESSION(),
            b.firstOf(
              b.token(JavaScriptPunctuator.EQU),
              b.token(JavaScriptPunctuator.STAR_EQU),
              b.token(JavaScriptPunctuator.DIV_EQU),
              b.token(JavaScriptPunctuator.MOD_EQU),
              b.token(JavaScriptPunctuator.PLUS_EQU),
              b.token(JavaScriptPunctuator.MINUS_EQU),
              b.token(JavaScriptPunctuator.SL_EQU),
              b.token(JavaScriptPunctuator.SR_EQU),
              b.token(JavaScriptPunctuator.SR_EQU2),
              b.token(JavaScriptPunctuator.AND_EQU),
              b.token(JavaScriptPunctuator.XOR_EQU),
              b.token(JavaScriptPunctuator.OR_EQU)),
            ASSIGNMENT_EXPRESSION_NO_IN()),
          ES6_ASSIGNMENT_EXPRESSION_NO_IN(),
          CONDITIONAL_EXPRESSION_NO_IN()
        ));
  }

  public ExpressionTree EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.EXPRESSION)
      .is(f.expression(ASSIGNMENT_EXPRESSION(), b.zeroOrMore(f.newTuple26(b.token(JavaScriptPunctuator.COMMA), ASSIGNMENT_EXPRESSION()))));
  }

  public ExpressionTree EXPRESSION_NO_IN() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.EXPRESSION_NO_IN)
      .is(f.expressionNoIn(ASSIGNMENT_EXPRESSION_NO_IN(), b.zeroOrMore(f.newTuple54(b.token(JavaScriptPunctuator.COMMA), ASSIGNMENT_EXPRESSION_NO_IN()))));
  }

  public ExpressionTree EXPRESSION_NO_LINE_BREAK() {
    return b.<ExpressionTree>nonterminal(JavaScriptLegacyGrammar.EXPRESSION_NO_LB)
      .is(f.expressionNoLineBreak(b.token(JavaScriptLegacyGrammar.SPACING_NO_LINE_BREAK_NOT_FOLLOWED_BY_LINE_BREAK), EXPRESSION()));
  }

  /**
   * A.3 [END] Expressions
   */

  /**
   * A.5 Declarations
   */

  // [START] Module, import & export
  public FromClauseTreeImpl FROM_CLAUSE() {
    return b.<FromClauseTreeImpl>nonterminal(Kind.FROM_CLAUSE)
      .is(f.fromClause(b.token(JavaScriptLegacyGrammar.FROM), STRING_LITERAL()));
  }

  public DefaultExportDeclarationTreeImpl DEFAULT_EXPORT_DECLARATION() {
    return b.<DefaultExportDeclarationTreeImpl>nonterminal(Kind.DEFAULT_EXPORT_DECLARATION)
      .is(f.defaultExportDeclaration(
        b.token(JavaScriptKeyword.EXPORT),
        b.token(JavaScriptKeyword.DEFAULT),
        b.firstOf(
          FUNCTION_AND_GENERATOR_DECLARATION(),
          CLASS_DECLARATION(),
          f.newTuple56(
            f.defaultExportExpression(b.token(JavaScriptLegacyGrammar.NEXT_NOT_FUNCTION_AND_CLASS), ASSIGNMENT_EXPRESSION()), b.token(JavaScriptLegacyGrammar.EOS)))
      ));
  }

  public NamedExportDeclarationTreeImpl NAMED_EXPORT_DECLARATION() {
    return b.<NamedExportDeclarationTreeImpl>nonterminal(Kind.NAMED_EXPORT_DECLARATION)
      .is(
        f.namedExportDeclaration(
          b.token(JavaScriptKeyword.EXPORT),
          b.firstOf(
            f.exportClause(EXPORT_LIST(), b.optional(FROM_CLAUSE()), b.token(JavaScriptLegacyGrammar.EOS)),
            VARIABLE_STATEMENT(),
            CLASS_DECLARATION(),
            FUNCTION_AND_GENERATOR_DECLARATION())));
  }

  public SpecifierListTreeImpl EXPORT_LIST() {
    return b.<SpecifierListTreeImpl>nonterminal(Kind.EXPORT_LIST)
      .is(f.exportList(
        b.token(JavaScriptPunctuator.LCURLYBRACE),
        b.optional(f.newExportSpecifierList(
          EXPORT_SPECIFIER(),
          b.zeroOrMore(f.newTuple50(b.token(JavaScriptPunctuator.COMMA), EXPORT_SPECIFIER())),
          b.optional(b.token(JavaScriptPunctuator.COMMA)))),
        b.token(JavaScriptPunctuator.RCURLYBRACE)
      ));
  }

  public SpecifierTreeImpl EXPORT_SPECIFIER() {
    return b.<SpecifierTreeImpl>nonterminal(Kind.EXPORT_SPECIFIER)
      .is(f.completeExportSpecifier(
        IDENTIFIER_NAME(),
        b.optional(f.newExportSpecifier(b.token(JavaScriptLegacyGrammar.AS), IDENTIFIER_NAME()))
      ));
  }

  public NameSpaceExportDeclarationTree NAMESPACE_EXPORT_DECLARATION() {
    return b.<NameSpaceExportDeclarationTree>nonterminal(Kind.NAMESPACE_EXPORT_DECLARATION)
      .is(f.namespaceExportDeclaration(
        b.token(JavaScriptKeyword.EXPORT),
        b.token(JavaScriptPunctuator.STAR),
        FROM_CLAUSE(),
        b.token(JavaScriptLegacyGrammar.EOS)
      ));
  }

  public ExportDeclarationTree EXPORT_DECLARATION() {
    return b.<ExportDeclarationTree>nonterminal(JavaScriptLegacyGrammar.EXPORT_DECLARATION)
      .is(b.firstOf(
        NAMESPACE_EXPORT_DECLARATION(),
        DEFAULT_EXPORT_DECLARATION(),
        NAMED_EXPORT_DECLARATION()
      ));

  }

  public ImportModuleDeclarationTree IMPORT_MODULE_DECLARATION() {
    return b.<ImportModuleDeclarationTree>nonterminal()
      .is(f.importModuleDeclaration(
        b.token(JavaScriptKeyword.IMPORT), STRING_LITERAL(), b.token(JavaScriptLegacyGrammar.EOS))
      );
  }

  public SpecifierListTree IMPORT_LIST() {
    return b.<SpecifierListTree>nonterminal(Kind.IMPORT_LIST)
      .is(f.importList(
        b.token(JavaScriptPunctuator.LCURLYBRACE),
        b.optional(f.newImportSpecifierList(
          IMPORT_SPECIFIER(),
          b.zeroOrMore(f.newTuple51(b.token(JavaScriptPunctuator.COMMA), IMPORT_SPECIFIER())),
          b.optional(b.token(JavaScriptPunctuator.COMMA)))),
        b.token(JavaScriptPunctuator.RCURLYBRACE)
      ));
  }

  public SpecifierTreeImpl IMPORT_SPECIFIER() {
    return b.<SpecifierTreeImpl>nonterminal(Kind.IMPORT_SPECIFIER)
      .is(f.completeImportSpecifier(
        b.firstOf(
          BINDING_IDENTIFIER(),
          IDENTIFIER_NAME()),
        b.optional(f.newImportSpecifier(b.token(JavaScriptLegacyGrammar.AS), BINDING_IDENTIFIER()))
      ));
  }

  public SpecifierTree NAMESPACE_IMPORT() {
    return b.<SpecifierTree>nonterminal(Kind.NAMESPACE_IMPORT_SPECIFIER)
      .is(f.nameSpaceImport(
        b.token(JavaScriptPunctuator.STAR),
        b.token(JavaScriptLegacyGrammar.AS),
        BINDING_IDENTIFIER()
      ));
  }

  public ImportClauseTreeImpl IMPORT_CLAUSE() {
    return b.<ImportClauseTreeImpl>nonterminal(Kind.IMPORT_CLAUSE)
      .is(f.importClause(
        b.firstOf(
          IMPORT_LIST(),
          NAMESPACE_IMPORT(),
          f.defaultImport(
            BINDING_IDENTIFIER(),
            b.optional(f.newTuple52(b.token(JavaScriptPunctuator.COMMA), b.firstOf(NAMESPACE_IMPORT(), IMPORT_LIST()))))
        )
      ));
  }

  public DeclarationTree IMPORT_DECLARATION() {
    return b.<DeclarationTree>nonterminal(JavaScriptLegacyGrammar.IMPORT_DECLARATION)
      .is(b.firstOf(
        f.importDeclaration(
          b.token(JavaScriptKeyword.IMPORT),
          IMPORT_CLAUSE(),
          FROM_CLAUSE(),
          b.token(JavaScriptLegacyGrammar.EOS)),
        IMPORT_MODULE_DECLARATION()
      ));
  }

  public ModuleTreeImpl MODULE_BODY() {
    return b.<ModuleTreeImpl>nonterminal(JavaScriptLegacyGrammar.MODULE_BODY)
      .is(
        f.module(
          b.oneOrMore(
            b.firstOf(
              IMPORT_DECLARATION(),
              EXPORT_DECLARATION(),
              VARIABLE_STATEMENT(),
              CLASS_DECLARATION(),
              FUNCTION_AND_GENERATOR_DECLARATION(),
              STATEMENT()))));
  }

  // [END] Module, import & export

  // [START] Destructuring pattern

  public BindingElementTree BINDING_PATTERN() {
    return b.<BindingElementTree>nonterminal(JavaScriptLegacyGrammar.BINDING_PATTERN)
      .is(
        b.firstOf(
          OBJECT_BINDING_PATTERN(),
          ARRAY_BINDING_PATTERN()));
  }

  public InitializedBindingElementTreeImpl INITIALISER() {
    return b.<InitializedBindingElementTreeImpl>nonterminal(JavaScriptLegacyGrammar.INITIALISER)
      .is(f.newInitializedBindingElement1(b.token(JavaScriptPunctuator.EQU), ASSIGNMENT_EXPRESSION()));
  }

  public InitializedBindingElementTreeImpl INITIALISER_NO_IN() {
    return b.<InitializedBindingElementTreeImpl>nonterminal()
      .is(f.newInitializedBindingElement2(b.token(JavaScriptPunctuator.EQU), ASSIGNMENT_EXPRESSION_NO_IN()));
  }

  public ObjectBindingPatternTreeImpl OBJECT_BINDING_PATTERN() {
    return b.<ObjectBindingPatternTreeImpl>nonterminal(Kind.OBJECT_BINDING_PATTERN)
      .is(
        f.completeObjectBindingPattern(
          b.token(JavaScriptPunctuator.LCURLYBRACE),
          b.optional(BINDING_PROPERTY_LIST()),
          b.token(JavaScriptPunctuator.RCURLYBRACE)));
  }

  public ObjectBindingPatternTreeImpl BINDING_PROPERTY_LIST() {
    return b.<ObjectBindingPatternTreeImpl>nonterminal()
      .is(
        f.newObjectBindingPattern(
          BINDING_PROPERTY(),
          b.zeroOrMore(f.newTuple53(b.token(JavaScriptPunctuator.COMMA), BINDING_PROPERTY())),
          b.optional(b.token(JavaScriptPunctuator.COMMA))));
  }

  public BindingElementTree BINDING_PROPERTY() {
    return b.<BindingElementTree>nonterminal()
      .is(
        b.firstOf(
          f.bindingProperty(PROPERTY_NAME(), b.token(JavaScriptPunctuator.COLON), BINDING_ELEMENT()),
          BINDING_ELEMENT()));
  }

  public BindingElementTree BINDING_ELEMENT() {
    return b.<BindingElementTree>nonterminal(JavaScriptLegacyGrammar.BINDING_ELEMENT)
      .is(
        f.completeBindingElement1(
          b.firstOf(
            BINDING_IDENTIFIER(),
            BINDING_PATTERN()),
          b.optional(INITIALISER())));
  }

  public BindingElementTree BINDING_ELEMENT_NO_IN() {
    return b.<BindingElementTree>nonterminal()
      .is(
        f.completeBindingElement2(
          b.firstOf(
            BINDING_IDENTIFIER(),
            BINDING_PATTERN()),
          b.optional(INITIALISER_NO_IN())));
  }

  public ArrayBindingPatternTreeImpl ARRAY_BINDING_PATTERN() {
    return b.<ArrayBindingPatternTreeImpl>nonterminal(JavaScriptLegacyGrammar.ARRAY_BINDING_PATTERN)
      .is(
        f.arrayBindingPattern(
          b.token(JavaScriptPunctuator.LBRACKET),
          b.optional(
            b.firstOf(
              BINDING_ELEMENT(),
              BINDING_REST_ELEMENT())),
          b.zeroOrMore(
            f.newTuple29(
              b.token(JavaScriptPunctuator.COMMA),
              b.optional(
                b.firstOf(
                  BINDING_ELEMENT(),
                  BINDING_REST_ELEMENT())))),
          b.token(JavaScriptPunctuator.RBRACKET)));
  }

  // [END] Destructuring pattern

  // [START] Classes, methods, functions & generators

  public ClassTreeImpl CLASS_DECLARATION() {
    return b.<ClassTreeImpl>nonterminal(Kind.CLASS_DECLARATION)
      .is(
        f.classDeclaration(
          b.token(JavaScriptKeyword.CLASS), BINDING_IDENTIFIER(),
          // TODO Factor the duplication with CLASS_EXPRESSION() into CLASS_TRAIT() ?
          b.optional(f.newTuple27(b.token(JavaScriptKeyword.EXTENDS), LEFT_HAND_SIDE_EXPRESSION())),
          b.token(JavaScriptPunctuator.LCURLYBRACE),
          b.zeroOrMore(CLASS_ELEMENT()),
          b.token(JavaScriptPunctuator.RCURLYBRACE)));
  }

  public Tree CLASS_ELEMENT() {
    return b.<Tree>nonterminal(JavaScriptLegacyGrammar.CLASS_ELEMENT)
      .is(
        b.firstOf(
          METHOD_DEFINITION(),
          b.token(JavaScriptPunctuator.SEMI)));
  }

  public MethodDeclarationTree METHOD_DEFINITION() {
    return b.<MethodDeclarationTree>nonterminal(JavaScriptLegacyGrammar.METHOD_DEFINITION)
      .is(
        b.firstOf(
          f.generator(
            b.optional(b.token(JavaScriptLegacyGrammar.STATIC)),
            b.token(JavaScriptPunctuator.STAR),
            PROPERTY_NAME(), FORMAL_PARAMETER_LIST(),
            BLOCK()),
          f.method(
            b.optional(b.token(JavaScriptLegacyGrammar.STATIC)),
            PROPERTY_NAME(), FORMAL_PARAMETER_LIST(),
            BLOCK()),
          f.accessor(
            b.optional(b.token(JavaScriptLegacyGrammar.STATIC)),
            b.firstOf(
              b.token(JavaScriptLegacyGrammar.GET),
              b.token(JavaScriptLegacyGrammar.SET)),
            PROPERTY_NAME(),
            FORMAL_PARAMETER_LIST(),
            BLOCK())));
  }

  public FunctionDeclarationTreeImpl FUNCTION_AND_GENERATOR_DECLARATION() {
    return b.<FunctionDeclarationTreeImpl>nonterminal(JavaScriptLegacyGrammar.FUNCTION_DECLARATION)
      .is(
        f.functionAndGeneratorDeclaration(
          b.token(JavaScriptKeyword.FUNCTION), b.optional(b.token(JavaScriptPunctuator.STAR)), BINDING_IDENTIFIER(), FORMAL_PARAMETER_LIST(),
          BLOCK()));
  }

  // [END] Classes, methods, functions & generators

  /**
   * A.5 [END] Declaration
   */

  public ScriptTreeImpl SCRIPT() {
    return b.<ScriptTreeImpl>nonterminal(JavaScriptLegacyGrammar.SCRIPT)
      .is(
        f.script(
          b.optional(b.token(JavaScriptLegacyGrammar.SHEBANG)),
          b.optional(MODULE_BODY()),
          b.token(JavaScriptLegacyGrammar.SPACING_NOT_SKIPPED),
          b.token(JavaScriptLegacyGrammar.EOF)));
  }

  private static <T> T ES6(T object) {
    return object;
  }

}
