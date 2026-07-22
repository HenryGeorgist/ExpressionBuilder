package expression.builder;

import usace.hec.expressions.ExpressionNode;
import usace.hec.expressions.ExpressionOperator;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

        public NodeDescriptor(Class<? extends ExpressionNode<?>> clazz, String category, String opName, String infixName, boolean isLeaf, boolean isBinary, boolean isUnary, boolean isConditional) {
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

        public Class<? extends ExpressionNode<?>> getClazz() { return clazz; }
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
     * Discover all ExpressionNode implementations in the classpath
     * by recursively scanning a single base package.
     */
    public static List<NodeDescriptor> discoverAllNodes() {
        List<NodeDescriptor> descriptors = new ArrayList<>();
        // Only specify the root package once; subpackages are scanned automatically
        descriptors.addAll(discoverInPackageRecursively("usace.hec.expressions"));
        return descriptors.stream()
                .sorted(Comparator.comparing(NodeDescriptor::getCategory)
                        .thenComparing(NodeDescriptor::getOpName))
                .collect(Collectors.toList());
    }

    private static List<NodeDescriptor> discoverInPackageRecursively(String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    results.addAll(scanDirectoryRecursively(new File(resource.getFile()), packageName));
                } else if ("jar".equals(protocol)) {
                    results.addAll(scanJarRecursively(resource, path, packageName));
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning package " + packageName + ": " + e.getMessage());
        }
        return results;
    }

    private static List<NodeDescriptor> scanDirectoryRecursively(File dir, String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageName + "." + file.getName().replace(".class", "");
                        results.addAll(analyzeClassQuietly(className));
                    } else if (file.isDirectory()) {
                        // Recurse into subdirectories
                        results.addAll(scanDirectoryRecursively(file, packageName + "." + file.getName()));
                    }
                }
            }
        }
        return results;
    }

    private static List<NodeDescriptor> scanJarRecursively(URL url, String basePackagePath, String basePackageName) throws IOException {
        List<NodeDescriptor> results = new ArrayList<>();
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = conn.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;

                String entryName = entry.getName();
                // Check if it's in the target base package (includes all subpackages)
                if (entryName.startsWith(basePackagePath + "/")) {
                    String className = entryName.replace('/', '.');
                    className = className.substring(0, className.length() - 6); // remove .class
                    results.addAll(analyzeClassQuietly(className));
                }
            }
        }
        return results;
    }

    private static List<NodeDescriptor> analyzeClassQuietly(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (ExpressionNode.class.isAssignableFrom(clazz) && 
                !clazz.isInterface() && 
                !Modifier.isAbstract(clazz.getModifiers())) {
                // Use the actual class's package name for category derivation
                String packageName = clazz.getPackageName();
                return analyzeClass(clazz, packageName);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Skip classes that can't be loaded
        }
        return new ArrayList<>();
    }

    private static List<NodeDescriptor> analyzeClass(Class<?> clazz, String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();
        String category = deriveCategory(packageName);
        String opName = extractOpName(clazz);
        String infixName = extractInfixName(clazz);
        boolean isLeaf = Arrays.stream(clazz.getInterfaces())
                .anyMatch(i -> i.getSimpleName().equals("LeafNode"));
        boolean isBinary = clazz.getSuperclass() != null && clazz.getSuperclass().getSimpleName().equals("BinaryExpressionNode");
        boolean isUnary = clazz.getSuperclass() != null && clazz.getSuperclass().getSimpleName().equals("UnaryExpressionNode");
        boolean isConditional = clazz.getSimpleName().equals("IfNode");
        results.add(new NodeDescriptor(
                (Class<? extends ExpressionNode<?>>) clazz, category, opName, infixName, isLeaf, isBinary, isUnary, isConditional));
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
            String simpleName = clazz.getSimpleName();
            if (simpleName.endsWith("Node")) {
                String base = simpleName.substring(0, simpleName.length() - 4);
                for (ExpressionOperator op : ExpressionOperator.values()) {
                    if (op.name().equalsIgnoreCase(base) || 
                        op.name().replace("_", "").equalsIgnoreCase(base.replace("_", ""))) {
                        return op.name();
                    }
                }
                return base;
            }
        } catch (Exception e) { /* fall through */ }
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
        } catch (Exception e) { /* fall through */ }
        return "N/A";
    }
}