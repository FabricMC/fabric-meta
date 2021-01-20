package net.fabricmc.meta.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftSemverConverter {
    // From Fabric Loader
    private static final Pattern RELEASE_PATTERN = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?");
    private static final Pattern PRE_RELEASE_PATTERN = Pattern.compile(".+(?:-pre| Pre-[Rr]elease )(\\d+)");
    private static final Pattern RELEASE_CANDIDATE_PATTERN = Pattern.compile(".+(?:-rc| [Rr]elease Candidate )(\\d+)");
    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(?:Snapshot )?(\\d+)w0?(0|[1-9]\\d*)([a-z])");

    private static final Pattern RELEASE_PATTERN_START = Pattern.compile("\\d+\\.\\d+\\.(\\d+)");


    private MinecraftSemverConverter() {}

    // Adapted from Fabric Loader
    public static String convert(String majorVersion, String name) {
        // This does not match loader's output as the needed data is not available
        if (name.contains("combat")) {
            return normalizeVersion(name);
        }

        Matcher matcher;

        matcher = RELEASE_PATTERN.matcher(name);
        if (matcher.matches()) {return name;}

        if (name.startsWith(majorVersion)) {
            Matcher m = RELEASE_PATTERN_START.matcher(name);
            matcher = RELEASE_CANDIDATE_PATTERN.matcher(name);
            if (matcher.matches()) {
                String rcBuild = matcher.group(1);

                // This is a hack to fake 1.16's new release candidates to follow on from the 8 pre releases.
                if (name.split("-")[0].equals("1.16")) {
                    int build = Integer.parseInt(rcBuild);
                    rcBuild = Integer.toString(8 + build);
                }

                name = String.format("rc.%s", rcBuild);
            } else {
                matcher = PRE_RELEASE_PATTERN.matcher(name);

                if (matcher.matches()) {
                    boolean legacyVersion;

                    // Handle lack of semver parser
                    int minor = 0;
                    if (m.find()) {
                        minor = Integer.parseInt(m.group(1));
                    }

                    legacyVersion = Integer.parseInt(majorVersion.split("\\.")[1]) < 16 ||
                            (Integer.parseInt(majorVersion.split("\\.")[1]) == 16 && minor < 1);

                    // Mark pre-releases as 'beta' versions, except for version 1.16 and before, where they are 'rc'
                    if (legacyVersion) {
                        name = String.format("rc.%s", matcher.group(1));
                    } else {
                        name = String.format("beta.%s", matcher.group(1));
                    }
                }
            }
            if (m.reset().find()) {
                return String.format("%s-%s", m.group(), name);
            }
        } else if ((matcher = SNAPSHOT_PATTERN.matcher(name)).matches()) {
            name = String.format("alpha.%s.%s.%s", matcher.group(1), matcher.group(2), matcher.group(3));
        } else {
            name = normalizeVersion(name);
        }

        return String.format("%s-%s", majorVersion.replace("-af", ""), name);
    }

    // Adapted from Fabric loader
    private static String normalizeVersion(String version) {
        StringBuilder ret = new StringBuilder(version.length() + 5);
        boolean lastIsDigit = false;
        boolean lastIsLeadingZero = false;
        boolean lastIsSeparator = false;

        for (int i = 0, max = version.length(); i < max; i++) {
            char c = version.charAt(i);

            if (c >= '0' && c <= '9') {
                if (i > 0 && !lastIsDigit && !lastIsSeparator) { // no separator between non-number and number, add one
                    ret.append('.');
                } else if (lastIsDigit && lastIsLeadingZero) { // leading zero in output -> strip
                    ret.setLength(ret.length() - 1);
                }

                lastIsLeadingZero = c == '0' && (!lastIsDigit || lastIsLeadingZero); // leading or continued leading zero(es)
                lastIsSeparator = false;
                lastIsDigit = true;
            } else if (c == '.' || c == '-') { // keep . and - separators
                if (lastIsSeparator) continue;

                lastIsSeparator = true;
                lastIsDigit = false;
            } else if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) { // replace remaining non-alphanumeric with .
                if (lastIsSeparator) continue;

                c = '.';
                lastIsSeparator = true;
                lastIsDigit = false;
            } else { // keep other characters (alpha)
                if (lastIsDigit) ret.append('.'); // no separator between number and non-number, add one

                lastIsSeparator = false;
                lastIsDigit = false;
            }

            ret.append(c);
        }

        // strip leading and trailing .

        int start = 0;
        while (start < ret.length() && ret.charAt(start) == '.') start++;

        int end = ret.length();
        while (end > start && ret.charAt(end - 1) == '.') end--;

        return ret.substring(start, end);
    }

}
