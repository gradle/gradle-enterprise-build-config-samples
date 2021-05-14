package com.gradle.enterprise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExportApiClient {
    private static final String BUILD_EVENT = "BuildEvent";

    private static class EventTypes {
        private static final String PROJECT_STRUCTURE = "ProjectStructure";
        private static final String BUILD_REQUESTED_TASKS = "BuildRequestedTasks";
        private static final String USER_NAMED_VALUE = "UserNamedValue";
        private static final String BUILD_FINISHED = "BuildFinished";
        private static final String ALL = PROJECT_STRUCTURE + "," + BUILD_REQUESTED_TASKS + "," + USER_NAMED_VALUE + "," + BUILD_FINISHED;
    }

    private static class StatusCodes {
        private static final int UNAUTHORIZED = 401;
        private static final int NOT_FOUND = 404;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

    private final OkHttpClient httpClient;
    private final URL baseUrl;
    private final EventSource.Factory eventSourceFactory;
    private final CustomValueKeys customValueKeys;

    public ExportApiClient(URL baseUrl, Authenticator authenticator, CustomValueKeys customValueKeys) {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ZERO)
            .readTimeout(Duration.ZERO)
            .retryOnConnectionFailure(true)
            .authenticator(authenticator)
            .build();
        this.eventSourceFactory = EventSources.createFactory(httpClient);
        this.baseUrl = baseUrl;
        this.customValueKeys = customValueKeys;
    }

    public BuildValidationData fetchBuildValidationData(String buildScanId) {
        var request = new Request.Builder()
            .url(endpointFor(buildScanId))
            .build();

        var eventListener = new BuildValidationDataEventListener(baseUrl, buildScanId, customValueKeys);
        eventSourceFactory.newEventSource(request, eventListener);
        return eventListener.getBuildValidationData();
    }

    private URL endpointFor(String buildScanId) {
        try {
            return new URL(baseUrl, "/build-export/v1/build/" + buildScanId + "/events?eventTypes=" + EventTypes.ALL);
        } catch (MalformedURLException e) {
            // It is highly unlikely this exception will ever be thrown. If it is thrown, then it is likely due to a
            // programming mistake (._.)
            throw new UnexpectedExceptionWhileFetchingBuildScan(buildScanId, baseUrl, e);
        }
    }

    private static class BuildValidationDataEventListener extends EventSourceListener {
        private final URL gradleEnterpriseServerUrl;
        private final String buildScanId;
        private final CustomValueKeys customValueKeys;

        private final CompletableFuture<String> rootProjectName = new CompletableFuture<>();
        private final CompletableFuture<String> gitUrl = new CompletableFuture<>();
        private final CompletableFuture<String> gitBranch = new CompletableFuture<>();
        private final CompletableFuture<String> gitCommitId = new CompletableFuture<>();
        private final CompletableFuture<List<String>> requestedTasks = new CompletableFuture<>();
        private final CompletableFuture<Boolean> buildSuccessful = new CompletableFuture<>();

        private final List<CompletableFuture<?>> completables = List.of(
            rootProjectName, gitUrl, gitBranch, gitCommitId, requestedTasks, buildSuccessful);

        private BuildValidationDataEventListener(URL gradleEnterpriseServerUrl, String buildScanId, CustomValueKeys customValueKeys) {
            this.gradleEnterpriseServerUrl = gradleEnterpriseServerUrl;
            this.buildScanId = buildScanId;
            this.customValueKeys = customValueKeys;
        }

        public BuildValidationData getBuildValidationData() {
            try {
                return new BuildValidationData(
                    rootProjectName.get(),
                    buildScanId,
                    gradleEnterpriseServerUrl,
                    gitUrl.get(),
                    gitBranch.get(),
                    gitCommitId.get(),
                    requestedTasks.get(),
                    buildSuccessful.get()
                );
            } catch (ExecutionException e) {
                if (e.getCause() == null) {
                    throw new UnexpectedExceptionWhileFetchingBuildScan(buildScanId, gradleEnterpriseServerUrl, e);
                } else {
                    Throwables.throwIfUnchecked(e.getCause());
                    throw new UnexpectedExceptionWhileFetchingBuildScan(buildScanId, gradleEnterpriseServerUrl, e.getCause());
                }
            } catch (InterruptedException e) {
                throw new InterruptedWhileFetchingBuildScan(buildScanId, gradleEnterpriseServerUrl, e);
            }
        }

        @Override
        public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
            if (BUILD_EVENT.equals(type)) {
                var event = toJsonNode(data);
                var eventType = event.get("type").get("eventType").asText();
                switch(eventType) {
                    case EventTypes.PROJECT_STRUCTURE:
                        onProjectStructure(event.get("data"));
                        break;
                    case EventTypes.BUILD_REQUESTED_TASKS:
                        onBuildRequestedTasks(event.get("data"));
                        break;
                    case EventTypes.USER_NAMED_VALUE:
                        onUserNamedValue(event.get("data"));
                        break;
                    case EventTypes.BUILD_FINISHED:
                        onBuildFinished(event.get("data"));
                        break;
                }
            }
        }

        private void onProjectStructure(JsonNode eventData) {
            rootProjectName.complete(eventData.get("rootProjectName").asText());
        }

        private void onBuildRequestedTasks(JsonNode eventData) {
            var requestedTasksNode = eventData.get("requested");
            requestedTasks.complete(MAPPER.convertValue(requestedTasksNode, new TypeReference<List<String>>() {
            }));
        }

        private void onUserNamedValue(JsonNode eventData) {
            var key = eventData.get("key").asText();
            var value = eventData.get("value").asText();

            if (customValueKeys.getGitRepositoryKey().equals(key)) {
                this.gitUrl.complete(value);
            }
            if (customValueKeys.getGitBranchKey().equals(key)) {
                this.gitBranch.complete(value);
            }
            if (customValueKeys.getGitCommitIdKey().equals(key)) {
                this.gitCommitId.complete(value);
            }
        }

        private void onBuildFinished(JsonNode eventData) {
            buildSuccessful.complete(
                !eventData.hasNonNull("failure")
            );
        }

        @Override
        public void onClosed(@NotNull EventSource eventSource) {
            // If the event stream is closed before we have completed all of the completable futures, then we can
            // assume that the build scan doesn't have the data
            // CompletableFuture.complete() sets the value only if the CompletableFuture hasn't already been completed.
            rootProjectName.complete("");
            gitUrl.complete("");
            gitBranch.complete("");
            gitCommitId.complete("");
            requestedTasks.complete(Collections.emptyList());
            buildSuccessful.complete(false);
        }

        @Override
        public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
            var error = t;
            if (error == null) {
                switch(response.code()) {
                    case StatusCodes.UNAUTHORIZED:
                        error = new AuthenticationFailed(buildScanId, gradleEnterpriseServerUrl);
                        break;
                    case StatusCodes.NOT_FOUND:
                        error = new BuildScanNotFound(buildScanId, gradleEnterpriseServerUrl);
                        break;
                    default:
                        error = new UnexpectedResponse(buildScanId, gradleEnterpriseServerUrl, response);
                }
            }

            for(var completable: completables) completable.completeExceptionally(error);
            eventSource.cancel();
        }

        private JsonNode toJsonNode(String data) {
            try {
                return MAPPER.readTree(data);
            } catch (JsonProcessingException e) {
                throw new UnparsableBuildScanEvent(buildScanId, gradleEnterpriseServerUrl, data, e);
            }
        }
    }
}