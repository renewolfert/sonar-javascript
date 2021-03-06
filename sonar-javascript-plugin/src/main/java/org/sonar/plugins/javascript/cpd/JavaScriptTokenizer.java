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
package org.sonar.plugins.javascript.cpd;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.sonar.javascript.lexer.JavaScriptLexer;
import org.sonar.plugins.javascript.JavaScriptSquidSensor;

public class JavaScriptTokenizer implements Tokenizer {

  private final Charset charset;
  private final boolean excludeMinified;

  public JavaScriptTokenizer(Charset charset, boolean excludeMinified) {
    this.charset = charset;
    this.excludeMinified = excludeMinified;
  }

  @Override
  public final void tokenize(SourceCode source, Tokens cpdTokens) {
    Lexer lexer = JavaScriptLexer.create(charset);

    String fileName = source.getFileName();
    if (excludeMinified && JavaScriptSquidSensor.isMinifiedFile(fileName)) {
      return;
    }

    List<Token> tokens = lexer.lex(new File(fileName));
    for (Token token : tokens) {
      TokenEntry cpdToken = new TokenEntry(getTokenImage(token), fileName, token.getLine());
      cpdTokens.add(cpdToken);
    }
    cpdTokens.add(TokenEntry.getEOF());
  }

  private static String getTokenImage(Token token) {
    if (token.getType() == GenericTokenType.LITERAL) {
      return GenericTokenType.LITERAL.getValue();
    }
    return token.getValue();
  }

}
