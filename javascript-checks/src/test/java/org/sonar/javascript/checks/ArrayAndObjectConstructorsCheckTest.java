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

import org.junit.Test;
import org.sonar.javascript.checks.tests.TreeCheckTest;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class ArrayAndObjectConstructorsCheckTest extends TreeCheckTest {

  @Test
  public void test() {
    ArrayAndObjectConstructorsCheck check = new ArrayAndObjectConstructorsCheck();

    CheckMessagesVerifier.verify(getIssues("src/test/resources/checks/arrayAndObjectConstructors.js", check))
      .next().atLine(2).withMessage("Use a literal instead of the Array constructor.")
      .next().atLine(3).withMessage("Use a literal instead of the Array constructor.")
      .next().atLine(5).withMessage("Use a literal instead of the Object constructor.")
      .noMore();
  }

}
