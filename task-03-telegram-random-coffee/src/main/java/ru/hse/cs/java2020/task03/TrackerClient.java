package ru.hse.cs.java2020.task03;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

public class TrackerClient {
    ArrayList<TrackerQueue> getAllQueues(String oauthToken, String orgID)
    throws java.io.IOException, java.lang.InterruptedException, AuthorizationException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/queues?"))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken,
                        "X-Org-Id", orgID)
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var body = response.body();
        if (response.statusCode() / HUNDRED == 4) {
            throw new AuthorizationException("");
        }
        ArrayList<TrackerQueue> result = new ArrayList<>();
        var queues = new JSONArray(body);
        for (int i = 0; i < queues.length(); i++) {
            var elem = queues.getJSONObject(i);
            result.add(new TrackerQueue(elem.getString("key"), elem.getInt("id")));
        }
        return result;
    }

    Optional<String> createTask(String oauthToken, String orgID, String name, String description,
                                Optional<String> user, String queueID) {
        JSONObject requestJSON = new JSONObject();
        requestJSON.put("summary", name);
        requestJSON.put("description", description);
        requestJSON.put("queue", new JSONObject().put("id", queueID));
        if (user.isPresent()) {
            requestJSON.put("assignee", user.get());
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/issues?"))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken,
                        "X-Org-Id", orgID)
                .POST(HttpRequest.BodyPublishers.ofString(requestJSON.toString()))
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == CREATED_CODE) {
                JSONObject obj = new JSONObject(response.body());
                return Optional.of(obj.getString("key"));
            } else {
                return Optional.empty();
            }
        } catch (IOException | InterruptedException exc) {
            return Optional.empty();
        }
    }

    ArrayList<String> getTasksByUser(String oauthToken, String orgID, String user)
                                             throws TrackerException {
        JSONObject request = new JSONObject();
        request.put("filter", new JSONObject().put("assignee", user));
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/issues/_search?order=+updatedAt"))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken,
                        "X-Org-Id", orgID)
                .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();
        try {
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != SUCCESS_CODE) {
                System.err.println(response.body());
                throw new TrackerException(response.body());
            }
            JSONArray responseJSON = new JSONArray(response.body());
            var result = new ArrayList<String>();
            for (int i = 0; i < responseJSON.length(); i++) {
                result.add(responseJSON.getJSONObject(i).getString("key"));
            }
            return result;
        } catch (IOException | InterruptedException exc) {
            System.err.println(exc.getMessage());
            throw new TrackerException(exc.getMessage());
        }
    }

    TaskInfo getTask(String oauthToken, String orgID, String task)
            throws java.io.IOException, java.lang.InterruptedException, AuthorizationException, TrackerException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/issues/" + task))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken,
                        "X-Org-Id", orgID)
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == UNAUTHORIZED_ERROR || response.statusCode() == FORBIDDEN_ERROR) {
            throw new AuthorizationException(response.body());
        }
        if (response.statusCode() == NOT_FOUND_ERROR) {
            throw new TrackerException(response.body());
        }
        var body = response.body();
        var res = new TaskInfo();
        JSONObject obj = new JSONObject(body);
        res.setName(obj.getString("key"));
        res.setDescription(obj.getString("summary"));
        if (obj.has("assignee")) {
            res.setAssignedTo(obj.getJSONObject("assignee").getString("display"));
        } else {
            res.setAssignedTo(null);
        }
        res.setAuthor(obj.getJSONObject("createdBy").getString("display"));
        if (obj.has("followers")) {
            var followers = obj.getJSONArray("followers");
            for (int i = 0; i < followers.length(); i++) {
                res.addFollower(followers.getJSONObject(i).getString("display"));
            }
        }
        HttpRequest commentRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tracker.yandex.net/v2/issues/" + task + "/comments"))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT))
                .headers("Authorization", "OAuth " + oauthToken,
                        "X-Org-Id", orgID)
                .GET()
                .build();
        response = client.send(commentRequest, HttpResponse.BodyHandlers.ofString());
        body = response.body();
        var comments = new JSONArray(body);
        for (int i = 0; i < comments.length(); i++) {
            var comment = comments.getJSONObject(i);
            res.addComment(new CommentInfo(comment.getJSONObject("createdBy").getString("display"),
                                           comment.getString("text")));
        }
        return res;
    }

    private TrackerClient() {
        client = HttpClient.newHttpClient();
    }

    public static TrackerClient getTrackerClient() {
        if (instance == null) {
            instance = new TrackerClient();
        }
        return instance;
    }

    private static final int REQUEST_TIMEOUT = 10;
    private static final int HUNDRED = 100;
    private static final int NOT_FOUND_ERROR = 404;
    private static final int FORBIDDEN_ERROR = 403;
    private static final int UNAUTHORIZED_ERROR = 401;
    private static final int CREATED_CODE = 201;
    private static final int SUCCESS_CODE = 200;
    private static TrackerClient instance = null;
    private final HttpClient client;
}
