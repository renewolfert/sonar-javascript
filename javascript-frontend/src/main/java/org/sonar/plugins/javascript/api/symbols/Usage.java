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
package org.sonar.plugins.javascript.api.symbols;

import com.google.common.annotations.Beta;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;

@Beta
public class Usage {

  public enum Kind {
    DECLARATION,
    DECLARATION_WRITE,
    // parameters in function signature
    LEXICAL_DECLARATION,
    WRITE,
    READ,
    READ_WRITE;
  }

  private Kind kind;
  private IdentifierTree identifierTree;

  /**
   * @param identifierTree - this tree contains only symbol name identifier (we need it for symbol highlighting)
   * @param kind           - kind of usage
   */
  private Usage(IdentifierTree identifierTree, Kind kind) {
    this.kind = kind;
    this.identifierTree = identifierTree;
  }

  public Kind kind() {
    return kind;
  }

  public IdentifierTree identifierTree() {
    return identifierTree;
  }

  public static Usage create(IdentifierTree symbolTree, Kind kind) {
    return new Usage(symbolTree, kind);
  }

  public boolean isDeclaration() {
    return kind == Kind.DECLARATION_WRITE || kind == Kind.DECLARATION;
  }

  public boolean isWrite() {
    return kind == Kind.DECLARATION_WRITE || kind == Kind.WRITE || kind == Kind.READ_WRITE;
  }
}
