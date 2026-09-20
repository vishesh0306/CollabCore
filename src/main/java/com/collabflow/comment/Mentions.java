package com.collabflow.comment;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.collabflow.identity.User;

/**
 * Finds the people named with an @ in a comment.
 *
 * <p>"@Priya" matches the teammate called Priya, and "@priya" also matches
 * priya@example.com. Only people in the task's team can be mentioned, so a comment can't
 * be used to reach someone outside it. Names with spaces can't be matched this way; a
 * separate username field would be the fix if that ever becomes a problem.
 */
final class Mentions {

    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9._-]+)");

    private Mentions() {
    }

    static Set<UUID> findIn(String body, List<User> teamMembers) {
        Set<String> names = MENTION.matcher(body).results()
                .map(match -> match.group(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (names.isEmpty()) {
            return Set.of();
        }
        return teamMembers.stream()
                .filter(member -> names.contains(member.getName().toLowerCase(Locale.ROOT))
                        || names.contains(emailStart(member)))
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    private static String emailStart(User user) {
        return user.getEmail().substring(0, user.getEmail().indexOf('@'));
    }
}
