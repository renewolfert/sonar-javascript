/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.checks;

import com.sonar.sslr.squid.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.javascript.JavaScriptAstScanner;
import org.sonar.squid.api.SourceFile;

import java.io.File;

public class TooManyLinesInFileCheckTest {

  private TooManyLinesInFileCheck check = new TooManyLinesInFileCheck();

  @Test
  public void testDefault() {
    SourceFile file = JavaScriptAstScanner.scanSingleFile(new File("src/test/resources/checks/tooManyLinesInFile.js"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();
  }

  @Test
  public void testCustom() {
    check.maximum = 1;
    SourceFile file = JavaScriptAstScanner.scanSingleFile(new File("src/test/resources/checks/tooManyLinesInFile.js"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().withMessage("File \"tooManyLinesInFile.js\" has 7 lines, which is greater than 1 authorized. Split it into smaller files.")
      .noMore();
  }

}