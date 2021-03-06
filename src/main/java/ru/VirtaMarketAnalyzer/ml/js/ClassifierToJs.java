package ru.VirtaMarketAnalyzer.ml.js;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.main.Utils;
import ru.VirtaMarketAnalyzer.ml.RetailSalePrediction;
import ru.VirtaMarketAnalyzer.publish.GitHubPublisher;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.j48.C45Split;
import weka.classifiers.trees.j48.ClassifierSplitModel;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Instances;

import java.io.*;
import java.lang.reflect.Field;

/**
 * Created by cobr123 on 19.01.2016.
 */
public final class ClassifierToJs {
    private static final Logger logger = LoggerFactory.getLogger(ClassifierToJs.class);
    private static long PRINT_ID = 0;

    public static void main(String[] args) throws Exception {
        final File file = new File(GitHubPublisher.localPath + RetailSalePrediction.predict_retail_sales + File.separator + "prediction_set_script.model");
        final J48 tree = (J48) loadModel(file.getAbsolutePath());
        final String path = file.getAbsolutePath().replace(".model", ".test.js");
        FileUtils.writeStringToFile(new File(path), ClassifierToJs.compress(ClassifierToJs.toSource(tree, "predictCommonBySet")), "UTF-8");
    }

    public static Classifier loadModel(final String path) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (Classifier) ois.readObject();
        }
    }

    public static void saveModel(final Classifier c, final String path) throws Exception {
        Utils.mkdirs(path);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(path))) {
            oos.writeObject(c);
            oos.flush();
        }
    }

    public static String compress(final String script) throws Exception {
        final Reader in = new StringReader(script);
        final JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter() {
            public void warning(String message, String sourceName,
                                int line, String lineSource, int lineOffset) {
                logger.warn("\n[WARNING]");
                if (line < 0) {
                    logger.warn("  " + message);
                } else {
                    logger.warn("  " + line + ':' + lineOffset + ':' + message);
                }
            }

            public void error(String message, String sourceName,
                              int line, String lineSource, int lineOffset) {
                logger.error("[ERROR]");
                if (line < 0) {
                    logger.error("  " + message);
                } else {
                    logger.error("  " + line + ':' + lineOffset + ':' + message);
                }
            }

            public EvaluatorException runtimeError(String message, String sourceName,
                                                   int line, String lineSource, int lineOffset) {
                error(message, sourceName, line, lineSource, lineOffset);
                return new EvaluatorException(message);
            }
        });

        // Close the input stream first, and then open the output stream,
        // in case the output file should override the input file.
        in.close();

        final StringWriter out = new StringWriter();

        final int linebreak = 0;
        final boolean munge = true;
        final boolean verbose = true;
        final boolean disableOptimizations = false;
        final boolean preserveAllSemiColons = false;

        compressor.compress(out, linebreak, munge, verbose, preserveAllSemiColons, disableOptimizations);
        return out.toString();
    }

    public static String toSource(final J48 tree, final String prefix) throws Exception {
        PRINT_ID = 0;
        /** The decision tree */
        final ClassifierTree m_root = (ClassifierTree) getPrivateFieldValue(tree.getClass(), tree, "m_root");
        /** Local model at node. */
//        final ClassifierSplitModel m_localModel = (ClassifierSplitModel) getPrivateFieldValue(m_root.getClass(), m_root, "m_localModel");

        final StringBuilder[] source = toSourceClassifierTree(m_root, prefix);
        final StringBuilder sb = new StringBuilder();
        sb.append("  function getParamFor" + WordUtils.capitalize(prefix) + "(){\n");
        sb.append("    param = [").append(RetailSalePrediction.ATTR.values().length - 1).append("];\n");
        for (RetailSalePrediction.ATTR attr : RetailSalePrediction.ATTR.values()) {
            //для последнего делаем вычисления, поэтому его в параметрах не должно быть
            if (attr.ordinal() != RetailSalePrediction.ATTR.values().length - 1) {
                sb.append("    param[");
                sb.append(attr.ordinal());
                sb.append("] = ");
                sb.append(attr.getFunctionName());
                sb.append("();\n");
            }
        }
        sb.append("    return param;\n");
        sb.append("  }\n");

        sb.append("  function getValueFor" + WordUtils.capitalize(prefix) + "(idx){\n");
        sb.append("    values = [];\n");
        sb.append("    values[0] = 'менее 50';\n");
        sb.append("    values[1] = 'около 50';\n");
        int idx = 2;
        for (final String number : RetailSalePrediction.numbers) {
            for (final String word : RetailSalePrediction.words) {
                sb.append("    values[").append(idx).append("] = '").append(word).append(" ").append(number).append("';\n");
                ++idx;
            }
        }
        sb.append("    return values[idx];\n");
        sb.append("  }\n");

        return sb.toString()
                + "  function " + prefix + "() {\n"
                + "    i = getParamFor" + WordUtils.capitalize(prefix) + "();\n"
                + "    p = " + prefix + "Classify(i);\n"
                + "    return getValueFor" + WordUtils.capitalize(prefix) + "(p);\n"
                + "  }\n"
                + "  function " + prefix + "Classify(i) {\n"
                + "    p = NaN;\n"
                + source[0]  // Assignment code
                + "    return p;\n"
                + "  }\n"
                + source[1]  // Support code
                ;
    }

    /**
     * Returns source code for the tree as an if-then statement. The
     * class is assigned to variable "p", and assumes the tested
     * instance is named "i". The results are returned as two stringbuffers:
     * a section of code for assignment of the class, and a section of
     * code containing support code (eg: other support methods).
     *
     * @return an array containing two stringbuffers, the first string containing
     * assignment code, and the second containing source for support code.
     * @throws Exception if something goes wrong
     */
    public static StringBuilder[] toSourceClassifierTree(final ClassifierTree m_root, final String prefix) throws Exception {
        final StringBuilder[] result = new StringBuilder[2];
        /** True if node is leaf. */
        final boolean m_isLeaf = isLeaf(m_root);
        /** Local model at node. */
        final ClassifierSplitModel m_localModel = (ClassifierSplitModel) getPrivateFieldValue(m_root.getClass(), m_root, "m_localModel");
//        logger.info(m_localModel.getClass().getName());
        /** References to sons. */
        final ClassifierTree[] m_sons = (ClassifierTree[]) getPrivateFieldValue(m_root.getClass(), m_root, "m_sons");
        /** The training instances. */
        final Instances m_train = (Instances) getPrivateFieldValue(m_root.getClass(), m_root, "m_train");

        if (m_isLeaf) {
            result[0] = new StringBuilder("    p = "
                    + m_localModel.distribution().maxClass(0) + ";\n");
            result[1] = new StringBuilder("");
        } else {
            final StringBuilder text = new StringBuilder();
            final StringBuilder atEnd = new StringBuilder();

            //nextID
            PRINT_ID++;
            final long printID = PRINT_ID;

            text.append("  function ").append(prefix).append("N")
                    .append(Integer.toHexString(m_localModel.hashCode()) + printID)
                    .append("(i) {\n")
                    .append("    p = NaN;\n");

            text.append("    if (")
                    .append(sourceExpression(m_localModel, -1, m_train))
                    .append(") {\n");
            text.append("      p = ")
                    .append(m_localModel.distribution().maxClass(0))
                    .append(";\n");
            text.append("    } ");
            for (int i = 0; i < m_sons.length; i++) {
                text.append("else if (").append(sourceExpression(m_localModel, i, m_train)).append(") {\n");
                if (isLeaf(m_sons[i])) {
                    text.append("      p = ").append(m_localModel.distribution().maxClass(i)).append(";\n");
                } else {
                    final StringBuilder[] sub = toSourceClassifierTree(m_sons[i], prefix);
                    text.append(sub[0]);
                    atEnd.append(sub[1]);
                }
                text.append("    } ");
                if (i == m_sons.length - 1) {
                    text.append('\n');
                }
            }

            text.append("    return p;\n  }\n");

            result[0] = new StringBuilder("    p = " + prefix + "N");
            result[0].append(Integer.toHexString(m_localModel.hashCode()) + printID)
                    .append("(i);\n");
            result[1] = text.append(atEnd);
        }
        return result;
    }

    public static String sourceExpression(final ClassifierSplitModel m_localModel, final int index, final Instances data) throws Exception {
        if (m_localModel instanceof C45Split) {
            return sourceExpression((C45Split) m_localModel, index, data);
        } else {
            logger.error(m_localModel.getClass().getName());
            throw new Exception("метод sourceExpression не реализован для класса \"" + m_localModel.getClass().getName() + "\"");
        }
    }

    public static String sourceExpression(final C45Split m_localModel, final int index, final Instances data) throws Exception {
        /** Attribute to split on. */
        final int m_attIndex = (int) getPrivateFieldValue(m_localModel.getClass(), m_localModel, "m_attIndex");
        /** Value of split point. */
        final double m_splitPoint = (double) getPrivateFieldValue(m_localModel.getClass(), m_localModel, "m_splitPoint");

        if (index < 0) {
            return "i[" + m_attIndex + "] == null";
        } else if (data.attribute(m_attIndex).isNominal()) {
            return "i[" + m_attIndex + "]" + " === \"" + data.attribute(m_attIndex).value(index) + "\"";
        } else {
            final StringBuilder expr = new StringBuilder("(i[");
            expr.append(m_attIndex).append("])");
            if (index == 0) {
                expr.append(" <= ").append(m_splitPoint);
            } else {
                expr.append(" > ").append(m_splitPoint);
            }
            return expr.toString();
        }
    }

    public static boolean isLeaf(final ClassifierTree m_root) throws Exception {
        return (boolean) getPrivateFieldValue(m_root.getClass(), m_root, "m_isLeaf");
    }

    //protected static метод нельзя вызвать
//    public static long nextID(final Class clazz, final ClassifierTree ct) throws Exception {
//        try {
//            final Method retrieveItems = clazz.getDeclaredMethod("nextID");
//            return (long) retrieveItems.invoke(clazz);//NoSuchMethodException
//        } catch (final NoSuchMethodException e) {
//            final Class superClass = clazz.getSuperclass();
//            if (superClass == null) {
//                throw e;
//            } else {
//                return nextID(superClass, ct);
//            }
//        }
//    }

    public static Object getPrivateFieldValue(final Class clazz, final Object obj, final String fieldName) throws Exception {
        try {
            final Field f = clazz.getDeclaredField(fieldName); //NoSuchFieldException
            f.setAccessible(true);
            return f.get(obj); //NoSuchFieldException
        } catch (final NoSuchFieldException e) {
            final Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getPrivateFieldValue(superClass, obj, fieldName);
            }
        }
    }
}
