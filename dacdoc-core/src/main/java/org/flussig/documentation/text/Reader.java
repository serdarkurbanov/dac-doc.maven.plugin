package org.flussig.documentation.text;

import org.flussig.documentation.Constants;
import org.flussig.documentation.check.Check;
import org.flussig.documentation.check.CompositeCheck;
import org.flussig.documentation.check.UrlCheck;
import org.flussig.documentation.exception.DacDocException;
import org.flussig.documentation.exception.DacDocParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reader accepts File handlers for given project and extracts all DacDoc anchors (placeholders)
 */
public class Reader {
    private static Pattern anchorPlaceholderPattern = Pattern.compile(String.format(
            "%s%s((.|\\n|\\r)*?)%s",
            Constants.ANCHOR_FRAMING,
            Constants.ANCHOR_KEYWORD,
            Constants.ANCHOR_FRAMING));


    /**
     * Get all markdown files in given directory
     */
    public static Set<File> findMarkdownFiles(Path path) throws DacDocException {
        Set<File> result = new HashSet<>();

        try {
            Files.walk(path)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".md"))
                    .forEach(result::add);
        } catch(Exception e) {
            throw new DacDocException(
                    String.format(
                            "traversing root folder %s throws exception", path), e);
        }

        return result;
    }

    /**
     * Parse files and extract anchors
     */
    public static Map<File, Set<Anchor>> parseFiles(Set<File> files) throws IOException, DacDocParseException {
        Map<File, Set<Anchor>> result = new HashMap<>();

        for(File f: files) {
            Set<Anchor> anchors = new HashSet<>();
            result.put(f, anchors);

            String content = Files.readString(f.toPath());

            // extract all DACDOC placeholders
            Matcher dacdocPlaceholderMatcher = anchorPlaceholderPattern.matcher(content);

            while(dacdocPlaceholderMatcher.find()) {
                String dacdocAnchorFullText = dacdocPlaceholderMatcher.group();

                Anchor anchor = Anchor.from(dacdocAnchorFullText);

                anchors.add(anchor);
            }
        }

        return result;
    }

    /**
     * Map file-anchor tuple to checks
     */
    public static Map<FileAnchorTuple, Check> createCheckMap(Map<File, Set<Anchor>> fileAnchorMap) throws DacDocParseException {
        // convert fileAnchorMap to set of tuples
        Set<FileAnchorTuple> tuples = fileAnchorMap.entrySet().stream()
                .flatMap(kv -> kv.getValue().stream().map(anchor -> new FileAnchorTuple(kv.getKey(), anchor)))
                .collect(Collectors.toSet());

        // assign each tuple a check
        Map<FileAnchorTuple, Check> result = new HashMap<>();

        // first loop through anchors: assign all checks
        fillChecksInitial(tuples, result);

        // second loop through anchors: put values into composite checks
        fillChecksComposite(tuples, result);

        return result;
    }

    /**
     * loops through anchor-check map and replace anchors with results in files
     */
    public static void replaceAnchorsWithResults(Map<FileAnchorTuple, Check> checkMap) {

    }

    // TODO: avoid circular dependencies for composite checks
    private static void fillChecksComposite(Set<FileAnchorTuple> tuples, Map<FileAnchorTuple, Check> result) {
        for(FileAnchorTuple fileAnchorTuple: tuples.stream().filter(t -> t.getAnchor().getAnchorType() == AnchorType.COMPOSITE).collect(Collectors.toSet())) {
            CompositeCheck compositeCheck = (CompositeCheck)result.get(fileAnchorTuple);

            Collection<String> ids = fileAnchorTuple.getAnchor().getIds();

            // find checks for all ids and attach to composite check
            for(String id: ids) {
                Check subCheck;

                Optional<FileAnchorTuple> subTuple = tuples.stream().filter(t -> t.getAnchor().getId().equals(id)).findFirst();

                if(subTuple.isEmpty()) {
                    subCheck = Check.unknownCheck;
                } else {
                    subCheck = result.get(subTuple.get());
                }

                compositeCheck.getChecks().add(subCheck);
            }
        }
    }

    private static void fillChecksInitial(Set<FileAnchorTuple> tuples, Map<FileAnchorTuple, Check> result) throws DacDocParseException {
        Set<Check> checks = new HashSet<>();

        for(FileAnchorTuple fileAnchorTuple: tuples) {
            Check check;

            if(fileAnchorTuple.getAnchor().getAnchorType() == AnchorType.COMPOSITE) {
                // for composite type: put empty composite check
                check = new CompositeCheck(new ArrayList());
            } else {
                // for primitive type: define type of check and add it
                if(fileAnchorTuple.getAnchor().getTestId().equals(Constants.DEFAULT_TEST_ID)) {
                    check = new UrlCheck(
                            fileAnchorTuple.getFile(),
                            UrlCheck.extractMarkdownUri(fileAnchorTuple.getAnchor().getArgument()));
                } else {
                    check = Check.unknownCheck;
                }
            }

            if(!checks.contains(check)) {
                checks.add(check);
            }

            result.put(fileAnchorTuple, check);
        }
    }

    public static class FileAnchorTuple {
        private File file;
        private Anchor anchor;

        public FileAnchorTuple(File file, Anchor anchor) {
            this.anchor = anchor;
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public Anchor getAnchor() {
            return anchor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileAnchorTuple that = (FileAnchorTuple) o;
            return Objects.equals(file, that.file) &&
                    Objects.equals(anchor, that.anchor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, anchor);
        }
    }
}
