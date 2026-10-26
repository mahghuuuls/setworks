package com.mahghuuuls.setworks.compat.crafttweaker;

import java.util.ArrayList;
import java.util.List;

/**
 * The text of a loader failure report: the file that failed to parse with its line, and the files
 * CraftTweaker refused after it (it visits every file of the loader in order and stops compiling
 * once one failed to parse). Pure; the bridge supplies what CraftTweaker returned.
 */
final class LoaderScope {

    private LoaderScope() {
    }

    /** Null when the loader ran without a parse error and reported success. */
    static String message(String loaderName, boolean loaded, String failingFile, int line, String explanation, List<String> visited) {
        if (loaded && failingFile == null) {
            return null;
        }
        StringBuilder text = new StringBuilder("loader ").append(loaderName).append(": ");
        if (failingFile == null) {
            return text.append("ended in error (see crafttweaker.log); the definitions of the file that failed are missing").toString();
        }
        text.append(failingFile).append(" line ").append(line).append(" failed to parse");
        if (explanation != null && !explanation.trim().isEmpty()) {
            text.append(": ").append(explanation.trim());
        }
        List<String> skipped = new ArrayList<String>();
        int index = indexOf(visited, failingFile);
        if (index >= 0) {
            skipped.addAll(visited.subList(index + 1, visited.size()));
        }
        if (!skipped.isEmpty()) {
            text.append("; CraftTweaker then skipped the loader's later files ").append(skipped);
        }
        return text.append("; every Setworks definition from ").append(skipped.isEmpty() ? "that file" : "those files")
                .append(" is missing").toString();
    }

    private static int indexOf(List<String> names, String file) {
        String wanted = normalize(file);
        for (int i = 0; i < names.size(); i++) {
            String name = normalize(names.get(i));
            if (name.equals(wanted) || name.endsWith("/" + wanted) || wanted.endsWith("/" + name)) {
                return i;
            }
        }
        return -1;
    }

    /** Separators as CraftTweaker prints them on Windows or not, case ignored. */
    static String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/').toLowerCase();
    }
}
