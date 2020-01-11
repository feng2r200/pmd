/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.internal;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.pmd.lang.AbstractPmdLanguageVersionHandler;
import net.sourceforge.pmd.lang.DataFlowHandler;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.VisitorStarter;
import net.sourceforge.pmd.lang.XPathHandler;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.DefaultASTXPathHandler;
import net.sourceforge.pmd.lang.dfa.DFAGraphRule;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.MethodLikeNode;
import net.sourceforge.pmd.lang.java.ast.internal.LanguageLevelChecker;
import net.sourceforge.pmd.lang.java.ast.internal.ReportingStrategy;
import net.sourceforge.pmd.lang.java.dfa.DataFlowFacade;
import net.sourceforge.pmd.lang.java.dfa.JavaDFAGraphRule;
import net.sourceforge.pmd.lang.java.metrics.JavaMetricsComputer;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey;
import net.sourceforge.pmd.lang.java.multifile.MultifileVisitorFacade;
import net.sourceforge.pmd.lang.java.qname.QualifiedNameResolver;
import net.sourceforge.pmd.lang.java.rule.internal.JavaRuleViolationFactory;
import net.sourceforge.pmd.lang.java.symboltable.SymbolFacade;
import net.sourceforge.pmd.lang.java.typeresolution.TypeResolutionFacade;
import net.sourceforge.pmd.lang.java.xpath.GetCommentOnFunction;
import net.sourceforge.pmd.lang.java.xpath.JavaFunctions;
import net.sourceforge.pmd.lang.java.xpath.MetricFunction;
import net.sourceforge.pmd.lang.java.xpath.TypeIsExactlyFunction;
import net.sourceforge.pmd.lang.java.xpath.TypeIsFunction;
import net.sourceforge.pmd.lang.java.xpath.TypeOfFunction;
import net.sourceforge.pmd.lang.metrics.LanguageMetricsProvider;
import net.sourceforge.pmd.lang.metrics.MetricKey;
import net.sourceforge.pmd.lang.metrics.internal.AbstractLanguageMetricsProvider;
import net.sourceforge.pmd.lang.rule.RuleViolationFactory;

import net.sf.saxon.sxpath.IndependentContext;

public class JavaLanguageHandler extends AbstractPmdLanguageVersionHandler {

    private final LanguageLevelChecker<?> levelChecker;
    private final LanguageMetricsProvider<ASTAnyTypeDeclaration, MethodLikeNode> myMetricsProvider = new JavaMetricsProvider();

    public JavaLanguageHandler(int jdkVersion) {
        this(jdkVersion, false);
    }

    public JavaLanguageHandler(int jdkVersion, boolean preview) {
        super(JavaProcessingStage.class);
        this.levelChecker = new LanguageLevelChecker<>(jdkVersion, preview, ReportingStrategy.reporterThatThrows());
    }


    @Override
    public Parser getParser(ParserOptions parserOptions) {
        return new JavaLanguageParser(levelChecker, parserOptions);
    }

    public int getJdkVersion() {
        return levelChecker.getJdkVersion();
    }

    @Override
    public DataFlowHandler getDataFlowHandler() {
        return new JavaDataFlowHandler();
    }

    @Override
    public XPathHandler getXPathHandler() {
        return new DefaultASTXPathHandler() {
            @Override
            public void initialize() {
                TypeOfFunction.registerSelfInSimpleContext();
                GetCommentOnFunction.registerSelfInSimpleContext();
                MetricFunction.registerSelfInSimpleContext();
                TypeIsFunction.registerSelfInSimpleContext();
                TypeIsExactlyFunction.registerSelfInSimpleContext();
            }

            @Override
            public void initialize(IndependentContext context) {
                super.initialize(context, LanguageRegistry.getLanguage(JavaLanguageModule.NAME), JavaFunctions.class);
            }
        };
    }

    @Override
    public RuleViolationFactory getRuleViolationFactory() {
        return JavaRuleViolationFactory.INSTANCE;
    }

    @Override
    public VisitorStarter getDataFlowFacade() {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new DataFlowFacade().initializeWith(getDataFlowHandler(), (ASTCompilationUnit) rootNode);
            }
        };
    }

    @Override
    public VisitorStarter getSymbolFacade() {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new SymbolFacade().initializeWith(null, (ASTCompilationUnit) rootNode);
            }
        };
    }

    @Override
    public VisitorStarter getSymbolFacade(final ClassLoader classLoader) {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new SymbolFacade().initializeWith(classLoader, (ASTCompilationUnit) rootNode);
            }
        };
    }

    @Override
    public VisitorStarter getTypeResolutionFacade(final ClassLoader classLoader) {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new TypeResolutionFacade().initializeWith(classLoader, (ASTCompilationUnit) rootNode);
            }
        };
    }

    @Deprecated
    @Override
    public VisitorStarter getMultifileFacade() {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new MultifileVisitorFacade().initializeWith((ASTCompilationUnit) rootNode);
            }
        };
    }


    @Override
    public VisitorStarter getQualifiedNameResolutionFacade(final ClassLoader classLoader) {
        return new VisitorStarter() {
            @Override
            public void start(Node rootNode) {
                new QualifiedNameResolver().initializeWith(classLoader, (ASTCompilationUnit) rootNode);
            }
        };
    }


    @Override
    public DFAGraphRule getDFAGraphRule() {
        return new JavaDFAGraphRule();
    }


    @Override
    public LanguageMetricsProvider<ASTAnyTypeDeclaration, MethodLikeNode> getLanguageMetricsProvider() {
        return myMetricsProvider;
    }


    private static class JavaMetricsProvider extends AbstractLanguageMetricsProvider<ASTAnyTypeDeclaration, MethodLikeNode> {


        JavaMetricsProvider() {
            super(ASTAnyTypeDeclaration.class, MethodLikeNode.class, JavaMetricsComputer.getInstance());
        }


        @Override
        public List<? extends MetricKey<ASTAnyTypeDeclaration>> getAvailableTypeMetrics() {
            return Arrays.asList(JavaClassMetricKey.values());
        }


        @Override
        public List<? extends MetricKey<MethodLikeNode>> getAvailableOperationMetrics() {
            return Arrays.asList(JavaOperationMetricKey.values());
        }
    }
}