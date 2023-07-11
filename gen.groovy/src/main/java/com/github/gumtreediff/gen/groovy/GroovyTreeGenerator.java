/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.gen.groovy;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TreeContext;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.ParserPluginFactory;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.StringReaderSource;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.CompilePhase;
import groovyjarjarantlr4.v4.gui.TestRig;
import groovyjarjarantlr4.v4.runtime.CharStream;
import groovyjarjarantlr4.v4.runtime.CharStreams;
import groovyjarjarantlr4.v4.runtime.CommonTokenStream;
import groovyjarjarantlr4.v4.runtime.tree.*;
import org.apache.groovy.parser.antlr4.GroovyLangLexer;
import org.apache.groovy.parser.antlr4.GroovyLangParser;
import groovy.transform.CompileStatic;

import java.util.UUID;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.List;
import java.lang.reflect.InvocationTargetException;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "groovy-parser", accept = {"\\.groovy$", "\\.gradle$"}, priority = Registry.Priority.HIGH)
public class GroovyTreeGenerator extends TreeGenerator {

    @Override
    public TreeContext generate(Reader reader) throws IOException {
        try {
            String groovyCode = readReader(reader);
            // List<ASTNode> astNodes = new AstBuilder().buildFromString(CompilePhase.CONVERSION, groovyCode);
            System.out.println("HERE");

            CstInspector gtr = new CstInspector();
            ParseTree parseTree = gtr.inspectParseTree(groovyCode);

            TreeContext context = new TreeContext();
            return context;
        } catch (Exception e) {
            System.out.println("ERROR");
            throw new SyntaxException(this, reader, e);
        }
    }

    public static String readReader(Reader reader) throws IOException {
        try (BufferedReader in = new BufferedReader(reader)) {
            char[] cbuf = new char[1024];
            StringBuilder sb = new StringBuilder(1024);
            int bytesRead;
            while ((bytesRead = in.read(cbuf, 0, 1024)) != -1) {
                sb.append(cbuf, 0, bytesRead);
            }
            return sb.toString();
        }
    }

    @CompileStatic
    private static class CstInspector extends TestRig {
        CstInspector() throws Exception {
            super(new String[] { "Groovy", "compilationUnit", "-tree" });
        }

        ParseTree inspectParseTree(String text) {
            try {
                CharStream charStream = CharStreams.fromReader(new StringReader(text));
                GroovyLangLexer lexer = new GroovyLangLexer(charStream);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                GroovyLangParser parser = new GroovyLangParser(tokens);
                ParseTree tree = parser.compilationUnit();
                String parseTreeString = tree.toStringTree(parser);
                System.out.println("PARSE TREE");
                System.out.println(parseTreeString);
                return tree;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}