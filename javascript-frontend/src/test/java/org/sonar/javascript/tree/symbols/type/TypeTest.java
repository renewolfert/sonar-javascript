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
package org.sonar.javascript.tree.symbols.type;

import java.io.File;
import org.sonar.javascript.tree.symbols.SymbolModelImpl;
import org.sonar.javascript.utils.JavaScriptTreeModelTest;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.tree.ScriptTree;


public abstract class TypeTest extends JavaScriptTreeModelTest {
  protected ScriptTree ROOT_NODE;
  protected SymbolModelImpl SYMBOL_MODEL;

  protected Symbol getSymbol(String name) {
    return SYMBOL_MODEL.getSymbols(name).iterator().next();
  }

  protected void setUp(String filename) throws Exception {
    File file = new File("src/test/resources/ast/resolve/type/", filename);
    ROOT_NODE = (ScriptTree) p.parse(file);
    SYMBOL_MODEL = SymbolModelImpl.create(ROOT_NODE, file, null);
  }
}
