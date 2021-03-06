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

public class SemicolonCheckTest extends TreeCheckTest {

  @Test
  public void test() {
    SemicolonCheck check = new SemicolonCheck();

    CheckMessagesVerifier.verify(getIssues("src/test/resources/checks/semicolon.js", check))
      .next().atLine(2).withMessage("Add a semicolon at the end of this statement.")
      .next().atLine(7)
      .next().atLine(22)
      .next().atLine(24)
      .next().atLine(28)
      .next().atLine(32)
      .next().atLine(34)
      .next().atLine(37)
      .next().atLine(43)
      .next().atLine(44)
      .next().atLine(45)
      .next().atLine(46)
      .next().atLine(47)
      .next().atLine(48)
      .next().atLine(49)
      .next().atLine(50)
      .next().atLine(51)
      .next().atLine(64)
      .next().atLine(65)
      .next().atLine(67)
      .next().atLine(69)
      .noMore();
  }

}
