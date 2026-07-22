package expression.builder;

import usace.hec.expressions.ExpressionNode;
import usace.hec.expressions.ExpressionOperator;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to discover all ExpressionNode implementations via reflection.
 */
public class ExpressionNodeRegistry {

    /**
     * Represents a discovered ExpressionNode implementation with its metadata.
     */
    public static class NodeDescriptor {
        private final Class<? extends ExpressionNode<?>> clazz;
        private final String simpleName;
        private final String category;
        private final String opName;
        private final String infixName;
        private final boolean isLeaf;
        private final boolean isBinary;
        private final boolean isUnary;
        private final boolean isConditional;

        public NodeDescriptor(Class<? extends ExpressionNode<?>> clazz, String category,
                              String opName, String infixName, boolean isLeaf,
                              boolean isBinary, boolean isUnary, boolean isConditional) {
            this.clazz = clazz;
            this.simpleName = clazz.getSimpleName();
            this.category = category;
            this.opName = opName != null ? opName : "N/A";
            this.infixName = infixName != null ? infixName : "N/A";
            this.isLeaf = isLeaf;
            this.isBinary = isBinary;
            this.isUnary = isUnary;
            this.isConditional = isConditional;
        }

        //public Class<? extends ExpressionNode<?>> getClazz() { return clazz; }
        public String getSimpleName() { return simpleName; }
        public String getCategory() { return category; }
        public String getOpName() { return opName; }
        public String getInfixName() { return infixName; }
        public boolean isLeaf() { return isLeaf; }
        public boolean isBinary() { return isBinary; }
        public boolean isUnary() { return isUnary; }
        public boolean isConditional() { return isConditional; }

        @Override
        public String toString() {
            return String.format("%s [%s] (infix: %s)", simpleName, category, infixName);
        }
    }

    /**
     * Discover all ExpressionNode implementations in the classpath.
     * Uses reflection on known packages from the expressions library.
     */
    public static List<NodeDescriptor> discoverAllNodes() {
        List<NodeDescriptor> descriptors = new ArrayList<>();

        // Known packages containing ExpressionNode implementations
        String[] packages = {
            "usace.hec.expressions.math",
            "usace.hec.expressions.logical",
            "usace.hec.expressions.comparison",
            "usace.hec.expressions.time",
            "usace.hec.expressions.misc",
            "usace.hec.expressions"
        };

        for (String pkg : packages) {
            descriptors.addAll(discoverInPackage(pkg));
        }

        return descriptors.stream()
                .sorted(Comparator.comparing(NodeDescriptor::getCategory)
                        .thenComparing(NodeDescriptor::getOpName))
                .collect(Collectors.toList());
    }

    private static List<NodeDescriptor> discoverInPackage(String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();

        try {
            // Get all classes in the package using the ClassLoader
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                    .getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    java.io.File dir = new java.io.File(resource.getFile());
                    if (dir.isDirectory()) {
                        for (java.io.File file : dir.listFiles((d, name) -> name.endsWith(".class"))) {
                            String className = packageName + "." +
                                    file.getName().replace(".class", "");
                            try {
                                Class<?> clazz = Class.forName(className);
                                if (ExpressionNode.class.isAssignableFrom(clazz) &&
                                        !clazz.isInterface() &&
                                        !Modifier.isAbstract(clazz.getModifiers())) {
                                    results.addAll(analyzeClass(clazz, packageName));
                                }
                            } catch (ClassNotFoundException e) {
                                // Skip classes that can't be loaded
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning package " + packageName + ": " + e.getMessage());
        }

        return results;
    }

    private static List<NodeDescriptor> analyzeClass(Class<?> clazz, String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();

        String category = deriveCategory(packageName);
        String opName = extractOpName(clazz);
        String infixName = extractInfixName(clazz);

        boolean isLeaf = java.util.Arrays.stream(clazz.getInterfaces())
            .anyMatch(i -> i.getSimpleName().equals("LeafNode"));
        boolean isBinary = clazz.getSuperclass() != null &&
                clazz.getSuperclass().getSimpleName().equals("BinaryExpressionNode");
        boolean isUnary = clazz.getSuperclass() != null &&
                clazz.getSuperclass().getSimpleName().equals("UnaryExpressionNode");
        boolean isConditional = clazz.getSimpleName().equals("IfNode");

        results.add(new NodeDescriptor(
                (Class<? extends ExpressionNode<?>>) clazz,
                category, opName, infixName, isLeaf, isBinary, isUnary, isConditional
        ));

        return results;
    }

    private static String deriveCategory(String packageName) {
        String[] parts = packageName.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1].toUpperCase();
        }
        return "CORE";
    }

    private static String extractOpName(Class<?> clazz) {
        try {
            // Try to get OpName from a no-arg instance if possible
            // For many nodes, we need to check if they have a default constructor
            // Since most don't, we'll use the class name heuristic
            String simpleName = clazz.getSimpleName();
            if (simpleName.endsWith("Node")) {
                String base = simpleName.substring(0, simpleName.length() - 4);
                // Try to find matching ExpressionOperator
                for (ExpressionOperator op : ExpressionOperator.values()) {
                    if (op.name().equalsIgnoreCase(base) ||
                        op.name().replace("_", "").equalsIgnoreCase(base.replace("_", ""))) {
                        return op.name();
                    }
                }
                return base;
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "UNKNOWN";
    }

    private static String extractInfixName(Class<?> clazz) {
        try {
            String simpleName = clazz.getSimpleName();
            if (simpleName.endsWith("Node")) {
                String base = simpleName.substring(0, simpleName.length() - 4);
                for (ExpressionOperator op : ExpressionOperator.values()) {
                    if (op.name().equalsIgnoreCase(base)) {
                        return op.getInfixName() != null ? op.getInfixName() : "N/A";
                    }
                }
            }
        } catch (Exception e) {
            // Fall through
        }
        return "N/A";
    }
}