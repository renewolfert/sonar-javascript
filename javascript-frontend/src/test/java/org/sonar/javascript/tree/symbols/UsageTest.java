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
package org.sonar.javascript.tree.symbols;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.junit.Test;
import org.sonar.javascript.utils.JavaScriptTreeModelTest;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Symbol.Kind;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;

public class UsageTest extends JavaScriptTreeModelTest {

  private SymbolModelImpl SYMBOL_MODEL = symbolModel(new File("src/test/resources/ast/resolve/usage.js"));

  @Test
  public void global_symbols() throws Exception {
    assertThat(usagesFor("a")).hasSize(3);
    assertThat(usagesFor("f")).hasSize(3);

    Collection<Symbol> symbols = SYMBOL_MODEL.getSymbols("b");
    Symbol b = null;
    for (Symbol symbol : symbols) {
      if (symbol.scope().tree().is(Tree.Kind.SCRIPT)) {
        b = symbol;
      }
    }
    assertThat(b.usages()).hasSize(2);
  }

  @Test
  public void global_build_in_symbols() throws Exception {
    assertThat(usagesFor("eval")).hasSize(2);
  }

  @Test
  public void arguments_build_in_symbol() throws Exception {
    Collection<Symbol> symbols = SYMBOL_MODEL.getSymbols("arguments");
    for (Symbol symbol : symbols) {
      if (symbol.scope().tree().is(Tree.Kind.SCRIPT)) {
        assertThat(symbol.builtIn()).isFalse();
      } else {
        assertThat(symbol.builtIn()).isTrue();
      }
    }
  }

  @Test
  public void function_symbols() throws Exception {
    assertThat(usagesFor("p1")).hasSize(2);
    assertThat(usagesFor("p2")).hasSize(1);
    Collection<Symbol> symbols = SYMBOL_MODEL.getSymbols("b");
    Symbol b = null;
    for (Symbol symbol : symbols) {
      if (symbol.scope().tree().is(Tree.Kind.FUNCTION_DECLARATION)) {
        b = symbol;
      }
    }
    assertThat(b.usages()).hasSize(3);
  }

  @Test
  public void function_expression_symbols() throws Exception {
    assertThat(usagesFor("g")).hasSize(2);
  }

  @Test
  public void catch_block_symbols() throws Exception {
    assertThat(usagesFor("e")).hasSize(2);
  }

  @Test
  public void usage_type() throws Exception {
    Collection<Usage> usages = usagesFor("var1");
    assertThat(usages).hasSize(5);
    Iterator<Usage> iterator = usages.iterator();
    int readCounter = 0;
    int writeCounter = 0;
    int declarationCounter = 0;
    while (iterator.hasNext()) {
      Usage next = iterator.next();
      readCounter += next.kind().equals(Usage.Kind.READ) || next.kind().equals(Usage.Kind.READ_WRITE) ? 1 : 0;
      writeCounter += next.isWrite() ? 1 : 0;
      declarationCounter += next.isDeclaration() ? 1 : 0;
    }
    assertThat(readCounter).isEqualTo(2);
    assertThat(writeCounter).isEqualTo(3);
    assertThat(declarationCounter).isEqualTo(2);
  }

  @Test
  public void block_scope_variables() throws Exception {
    Set<Symbol> symbols = SYMBOL_MODEL.getSymbols("x");
    assertThat(symbols).hasSize(2);

    Symbol globalSymbol = null;
    Symbol blockSymbol = null;

    for (Symbol symbol : symbols) {
      if (symbol.scope().isGlobal()) {
        globalSymbol = symbol;

      } else {
        blockSymbol = symbol;
      }
    }

    assertThat(globalSymbol.is(Kind.LET_VARIABLE)).isTrue();
    assertThat(blockSymbol.is(Kind.CONST_VARIABLE)).isTrue();
    assertThat(blockSymbol.scope().tree().is(Tree.Kind.BLOCK)).isTrue();

    assertThat(globalSymbol.usages()).hasSize(3);
    assertThat(blockSymbol.usages()).hasSize(1);

  }

  @Test
  public void let_variable_for_loop() throws Exception {
    Set<Symbol> symbols = SYMBOL_MODEL.getSymbols("i");
    assertThat(symbols).hasSize(2);

    Symbol globalSymbol = null;
    Symbol blockSymbol = null;

    for (Symbol symbol : symbols) {
      if (symbol.scope().isGlobal()) {
        globalSymbol = symbol;

      } else {
        blockSymbol = symbol;
      }
    }

    assertThat(globalSymbol.is(Kind.VARIABLE)).isTrue();
    assertThat(blockSymbol.is(Kind.LET_VARIABLE)).isTrue();
    assertThat(blockSymbol.scope().tree().is(Tree.Kind.FOR_STATEMENT)).isTrue();

    assertThat(globalSymbol.usages()).hasSize(2);
    assertThat(blockSymbol.usages()).hasSize(3);

    assertThat(SYMBOL_MODEL.getSymbols("j")).hasSize(1);
    Symbol symbol = symbol("j");
    assertThat(symbol.is(Kind.VARIABLE)).isTrue();
    assertThat(symbol.scope().isGlobal()).isTrue();
    assertThat(symbol.usages()).hasSize(5);
  }

  @Test
  public void let_variable_in_for() throws Exception {
    Symbol symbol = symbol("y");

    assertThat(symbol.is(Kind.LET_VARIABLE)).isTrue();
    assertThat(symbol.scope().isBlock()).isTrue();
    assertThat(symbol.scope().tree().is(Tree.Kind.FOR_OF_STATEMENT)).isTrue();
    assertThat(symbol.usages()).hasSize(2);

  }

  @Test
  public void let_variable_in_for_without_block() throws Exception {
    Symbol symbol = symbol("z");

    assertThat(symbol.is(Kind.LET_VARIABLE)).isTrue();
    assertThat(symbol.scope().isBlock()).isTrue();
    assertThat(symbol.scope().tree().is(Tree.Kind.FOR_IN_STATEMENT)).isTrue();
    assertThat(symbol.usages()).hasSize(2);

  }

  @Test
  public void imported_symbols() throws Exception {
    Symbol defaultMember = symbol("DefaultMember");
    assertThat(defaultMember.scope().isGlobal()).isTrue();
    assertThat(defaultMember.usages()).hasSize(2);
    assertThat(defaultMember.is(Kind.IMPORT)).isTrue();

    Symbol allMembers = symbol("AllMembers");
    assertThat(allMembers.scope().isGlobal()).isTrue();
    assertThat(allMembers.usages()).hasSize(1);
    assertThat(allMembers.is(Kind.IMPORT)).isTrue();

    Symbol member1 = symbol("member1");
    assertThat(member1.scope().isGlobal()).isTrue();
    assertThat(member1.usages()).hasSize(2);
    assertThat(member1.is(Kind.IMPORT)).isTrue();

    Symbol member2 = symbol("member2");
    assertThat(member2.scope().isGlobal()).isTrue();
    assertThat(member2.usages()).hasSize(1);
    assertThat(member2.is(Kind.IMPORT)).isTrue();

    assertThat(SYMBOL_MODEL.getSymbols("member3")).isEmpty();
    Symbol member3 = symbol("member3Alias");
    assertThat(member3.scope().isGlobal()).isTrue();
    assertThat(member3.usages()).hasSize(1);
    assertThat(member3.is(Kind.IMPORT)).isTrue();

    Symbol member4 = symbol("member4");
    assertThat(member4.scope().isGlobal()).isTrue();
    assertThat(member4.usages()).hasSize(1);
    assertThat(member4.is(Kind.IMPORT)).isTrue();

    Symbol defaultMember1 = symbol("DefaultMember1");
    assertThat(defaultMember1.scope().isGlobal()).isTrue();
    assertThat(defaultMember1.usages()).hasSize(1);
    assertThat(defaultMember1.is(Kind.IMPORT)).isTrue();
  }

  public Collection<Usage> usagesFor(String name) {
    return symbol(name).usages();
  }

  public Symbol symbol(String name) {
    return (Symbol) SYMBOL_MODEL.getSymbols(name).toArray()[0];
  }

}
