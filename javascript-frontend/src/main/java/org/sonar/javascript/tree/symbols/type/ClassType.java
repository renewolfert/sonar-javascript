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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.plugins.javascript.api.tree.expression.ClassTree;

public class ClassType extends ObjectType {

  private ClassTree classTree;

  private Map<String, FunctionType> methods = new HashMap<>();

  protected ClassType() {
    super(Callability.NON_CALLABLE);
  }

  @Override
  public Kind kind() {
    return Kind.CLASS;
  }

  public static ClassType create(ClassTree classTree) {
    ClassType type = new ClassType();
    type.classTree = classTree;
    return type;
  }

  public ClassTree classTree() {
    return classTree;
  }

  public ObjectType createObject() {
    ObjectType objectType = ObjectType.create(Callability.NON_CALLABLE);
    objectType.classType(this);
    return objectType;
  }

  public void addMethod(String name, FunctionType functionType) {
    methods.put(name, functionType);
  }

  @Nullable
  public FunctionType findMethod(String name) {
    return methods.get(name);
  }
}
