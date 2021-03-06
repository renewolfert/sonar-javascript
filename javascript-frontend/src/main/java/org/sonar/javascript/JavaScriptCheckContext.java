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
package org.sonar.javascript;

import java.io.File;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitorContext;

public class JavaScriptCheckContext implements TreeVisitorContext {

  private final ScriptTree tree;
  private final File file;
  private final SymbolModel symbolModel;

  public JavaScriptCheckContext(ScriptTree tree, File file, SymbolModel symbolModel) {
    this.tree = tree;
    this.file = file;
    this.symbolModel = symbolModel;
  }

  @Override
  public ScriptTree getTopTree() {
    return tree;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public SymbolModel getSymbolModel() {
    return symbolModel;
  }
}
