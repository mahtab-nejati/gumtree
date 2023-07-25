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

import groovyjarjarantlr4.v4.gui.TestRig;
import groovyjarjarantlr4.v4.runtime.CharStream;
import groovyjarjarantlr4.v4.runtime.CharStreams;
import groovyjarjarantlr4.v4.runtime.CommonTokenStream;
import groovyjarjarantlr4.v4.runtime.RecognitionException;
import groovyjarjarantlr4.v4.runtime.RuleContext;
import groovyjarjarantlr4.v4.runtime.ParserRuleContext;
import groovyjarjarantlr4.v4.runtime.Token;
import groovyjarjarantlr4.v4.runtime.tree.ParseTree;
import groovyjarjarantlr4.v4.gui.TreeViewer;

import org.apache.groovy.parser.antlr4.GroovyLangLexer;
import org.apache.groovy.parser.antlr4.GroovyLangParser;
import groovy.transform.CompileStatic;

import javax.swing.tree.TreeNode;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatter;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "groovy-parser", accept = { "\\.groovy$", "\\.gradle$" }, priority = Registry.Priority.HIGH)
public class GroovyTreeGenerator extends TreeGenerator {
    private Deque<Tree> trees = new ArrayDeque<>();

    @Override
    public TreeContext generate(Reader r) throws IOException {
        try {
            String groovyCode = readReader(r);

            CstInspector gtr = new CstInspector();
            ParseTree parseTree = gtr.inspectParseTree(groovyCode);
            
            TreeContext context = new TreeContext();
            buildTree(context, parseTree);

            return context;
        } catch (Exception e) {
            System.out.println("ERROR");
            throw new SyntaxException(this, r, e);
        }
    }

    public static String readReader(Reader r) throws IOException {
        try (BufferedReader in = new BufferedReader(r)) {
            char[] cbuf = new char[1024];
            StringBuilder sb = new StringBuilder(1024);
            int bytesRead;
            while ((bytesRead = in.read(cbuf, 0, 1024)) != -1) {
                sb.append(cbuf, 0, bytesRead);
            }
            return sb.toString();
        }
    }

    protected String[] getTokenNames() {
        return GroovyLangParser.tokenNames;
    }

    protected String[] getRuleNames() {
        return makeRuleNames();
    }

    protected Type getTokenName(int tokenType) {
        String[] names = getTokenNames();
        if (tokenType < 0 || tokenType >= names.length)
            return Type.NO_TYPE;
        return type(names[tokenType]);
    }

    protected Type getRuleName(int ruleType) {
        String[] names = getRuleNames();
        if (ruleType < 0 || ruleType >= names.length)
            return Type.NO_TYPE;
        return type(names[ruleType]);
    }

    protected void buildTree(TreeContext context, ParseTree pt) {
        Object payload = pt.getPayload(); // makeOrGetType();
        Type type = null;
        if (payload instanceof Token)
            type = getTokenName(((Token) payload).getType());
        else if (payload instanceof RuleContext)
            type = getRuleName(((RuleContext) payload).getRuleIndex());

        String label = pt.getText();
        if (type.name.equals(label)) // FIXME
            label = Tree.NO_LABEL;

        int start = 0;
        int stop = 0;

        if (payload instanceof Token) {
            start = ((Token) payload).getStartIndex();
            stop = ((Token) payload).getStopIndex();
        } else if (payload instanceof RuleContext) {
            start = ((ParserRuleContext) payload).getStart().getStartIndex();
            stop = ((ParserRuleContext) payload).getStop() == null 
                    ? -1 
                    : ((ParserRuleContext) payload).getStop().getStopIndex();
        }
  
        Tree t = null; 

        // add rules to ignore others nodes
        Boolean shouldIncludeInTree = type.toString() != "sep"; // ignore /n node
        
        if (shouldIncludeInTree) {
            t = context.createTree(type, pt.getChildCount() > 0 ? "" : label);
            t.setPos(start);
            t.setLength(stop - start + 1);

            if (trees.isEmpty())
                context.setRoot(t);
            else
                t.setParentAndUpdateChildren(trees.peek());
        }

        if (pt.getChildCount() > 0) {
            if (shouldIncludeInTree) {
                trees.push(t);
            }
            
            for (int i = 0; i < pt.getChildCount(); i++)
                buildTree(context, pt.getChild(i));

            if (shouldIncludeInTree) {
                trees.pop();
            }
        }
    }

    private static String[] makeRuleNames() {
        return new String[] {
                "compilationUnit", "scriptStatements", "scriptStatement", "packageDeclaration",
                "importDeclaration", "typeDeclaration", "modifier", "modifiersOpt", "modifiers",
                "classOrInterfaceModifiersOpt", "classOrInterfaceModifiers", "classOrInterfaceModifier",
                "variableModifier", "variableModifiersOpt", "variableModifiers", "typeParameters",
                "typeParameter", "typeBound", "typeList", "classDeclaration", "classBody",
                "enumConstants", "enumConstant", "classBodyDeclaration", "memberDeclaration",
                "methodDeclaration", "compactConstructorDeclaration", "methodName", "returnType",
                "fieldDeclaration", "variableDeclarators", "variableDeclarator", "variableDeclaratorId",
                "variableInitializer", "variableInitializers", "emptyDims", "emptyDimsOpt",
                "standardType", "type", "classOrInterfaceType", "generalClassOrInterfaceType",
                "standardClassOrInterfaceType", "primitiveType", "typeArguments", "typeArgument",
                "annotatedQualifiedClassName", "qualifiedClassNameList", "formalParameters",
                "formalParameterList", "thisFormalParameter", "formalParameter", "methodBody",
                "qualifiedName", "qualifiedNameElement", "qualifiedNameElements", "qualifiedClassName",
                "qualifiedStandardClassName", "literal", "gstring", "gstringValue", "gstringPath",
                "lambdaExpression", "standardLambdaExpression", "lambdaParameters", "standardLambdaParameters",
                "lambdaBody", "closure", "closureOrLambdaExpression", "blockStatementsOpt",
                "blockStatements", "annotationsOpt", "annotation", "elementValues", "annotationName",
                "elementValuePairs", "elementValuePair", "elementValuePairName", "elementValue",
                "elementValueArrayInitializer", "block", "blockStatement", "localVariableDeclaration",
                "variableDeclaration", "typeNamePairs", "typeNamePair", "variableNames",
                "conditionalStatement", "ifElseStatement", "switchStatement", "loopStatement",
                "continueStatement", "breakStatement", "yieldStatement", "tryCatchStatement",
                "assertStatement", "statement", "catchClause", "catchType", "finallyBlock",
                "resources", "resourceList", "resource", "switchBlockStatementGroup",
                "switchLabel", "forControl", "enhancedForControl", "classicalForControl",
                "forInit", "forUpdate", "castParExpression", "parExpression", "expressionInPar",
                "expressionList", "expressionListElement", "enhancedStatementExpression",
                "statementExpression", "postfixExpression", "switchExpression", "switchBlockStatementExpressionGroup",
                "switchExpressionLabel", "expression", "castOperandExpression", "commandExpression",
                "commandArgument", "pathExpression", "pathElement", "namePart", "dynamicMemberName",
                "indexPropertyArgs", "namedPropertyArgs", "primary", "namedPropertyArgPrimary",
                "namedArgPrimary", "commandPrimary", "list", "map", "mapEntryList", "namedPropertyArgList",
                "mapEntry", "namedPropertyArg", "namedArg", "mapEntryLabel", "namedPropertyArgLabel",
                "namedArgLabel", "creator", "dim", "arrayInitializer", "anonymousInnerClassDeclaration",
                "createdName", "nonWildcardTypeArguments", "typeArgumentsOrDiamond",
                "arguments", "argumentList", "enhancedArgumentListInPar", "firstArgumentListElement",
                "argumentListElement", "enhancedArgumentListElement", "stringLiteral",
                "className", "identifier", "builtInType", "keywords", "rparen", "nls",
                "sep"
        };
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

                return tree;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}