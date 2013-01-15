package com.paulsh.rmtrack;

import jetbrains.buildServer.issueTracker.AbstractIssueProviderFactory;
import jetbrains.buildServer.issueTracker.IssueFetcher;
import jetbrains.buildServer.issueTracker.IssueProvider;
import org.jetbrains.annotations.NotNull;

/**
 * RMTrack support
 * User: paul-sh
 * Date: 22.10.11
 */
public class RMTrackIssueProviderFactory extends AbstractIssueProviderFactory {

    protected RMTrackIssueProviderFactory(@NotNull IssueFetcher fetcher) {
        super(fetcher, "RMTrack");
    }

    @NotNull
    public IssueProvider createProvider() {
        return new RMTrackIssueProvider(myFetcher);
    }
}
