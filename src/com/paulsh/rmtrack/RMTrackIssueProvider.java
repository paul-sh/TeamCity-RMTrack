package com.paulsh.rmtrack;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.issueTracker.AbstractIssueProvider;
import jetbrains.buildServer.issueTracker.IssueFetcher;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RMTrack support
 * User: paul-sh
 * Date: 22.10.11
 */
public class RMTrackIssueProvider extends AbstractIssueProvider {
    private static final Logger LOG = Loggers.SERVER;
//    private String myAccessKey;
//    private String mySecretKey;

    public RMTrackIssueProvider(@NotNull IssueFetcher fetcher) {
        super("rmtrack", fetcher);
    }

    protected RMTrackIssueProvider(@NotNull Pattern pattern, @NotNull IssueFetcher fetcher) {
        super(pattern, fetcher);
    }

    @Nullable
    protected String checkIssueIdBeforeTestConnection(@NotNull String issueId) {
        if (issueId == null)
            throw new IllegalArgumentException("Argument 0 for @NotNull parameter of RMTrackIssueProvider.checkIssueIdBeforeTestConnection must not be null");
        try {
            Integer.parseInt(issueId);
            return null;
        } catch (NumberFormatException e) {
        }
        return "Expected a numeric issue id";
    }

//    @Override
//    public void setProperties(@NotNull final Map<String, String> map) {
//        super.setProperties(map);
//        myHost = map.get("repository");
//        if (myFetcher instanceof RMTrackIssueFetcher) {
//            ((RMTrackIssueFetcher)myFetcher).setKeys(map.get("secure:accesskey"), map.get("secure:secretkey"));
//        }
//    }

    @NotNull
    @Override
    protected Pattern compilePattern(@NotNull final Map<String, String> properties) {
        Pattern result = super.compilePattern(properties);
        if (myFetcher instanceof RMTrackIssueFetcher) {
            ((RMTrackIssueFetcher)myFetcher).setPattern(result);
        }
        return result;
    }

    @NotNull
    @Override
    protected String extractId(@NotNull String match) {
        Matcher matcher = myPattern.matcher(match);
        matcher.find();
        if (matcher.groupCount() >= 1) {
            return matcher.group(1);
        } else {
            return super.extractId(match);
        }
    }
}
