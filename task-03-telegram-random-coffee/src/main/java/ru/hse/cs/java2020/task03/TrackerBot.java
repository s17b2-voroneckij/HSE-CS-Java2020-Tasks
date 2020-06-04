package ru.hse.cs.java2020.task03;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendMessage;

import java.io.IOException;
import java.util.Optional;

public class TrackerBot {
    public void processAuthorization(long chatId, String[] request) {
        if (request.length < 4) {
            displayHelpMessage(chatId);
        } else {
            db.insert(chatId, new UserInfo(request[1], request[2], request[3]));
            sendMessage(chatId, "Your data has been saved");
        }
    }

    private void sendMessage(long chatId, String text) {
        bot.execute(new SendMessage(chatId, text));
    }

    public void processGetTask(long chatId, String[] request) {
        var userInfo = db.get(chatId);
        if (!userInfo.isPresent()) {
            authError(chatId);
            return;
        }
        if (request.length < 2) {
            displayHelpMessage(chatId);
        } else {
            try {
                var task = trackerClient.getTask(userInfo.get().getToken(), userInfo.get().getOrg(), request[1]);
                sendMessage(chatId, "task name: " + task.getName());
                sendMessage(chatId, "description: " + task.getDescription());
                sendMessage(chatId, "author: " + task.getAuthor());
                var assignedTo = task.getAssignedTo();
                assignedTo.ifPresent(s -> sendMessage(chatId, "assigned to: " + s));
                var followers = task.getFollowers();
                if (followers.size() > 0) {
                    sendMessage(chatId, "followers:");
                    for (var follower : followers) {
                        sendMessage(chatId, follower);
                    }
                }
                var comments = task.getComments();
                if (comments.size() > 0) {
                    sendMessage(chatId, "comments:");
                    for (var comment : comments) {
                        sendMessage(chatId, comment.getAuthor() + " wrote " + comment.getText());
                    }
                }
            } catch (IOException | InterruptedException exc) {
                sendMessage(chatId, "Something went wrong, try again");
            } catch (AuthorizationException exc) {
                sendMessage(chatId, "Authorization problem occurred, check your token");
                sendMessage(chatId, exc.getMessage());
            } catch (TrackerException exc) {
                sendMessage(chatId, "Task not found");
                sendMessage(chatId, exc.getMessage());
            }
        }
    }

    public void processTaskCreation(long chatId, String[] request) {
        var userInfo = db.get(chatId);
        if (!userInfo.isPresent()) {
            authError(chatId);
            return;
        }
        if (request.length < 4) {
            displayHelpMessage(chatId);
        } else {
            Optional<String> created;
            if (request.length == 4) {
                created = trackerClient.createTask(userInfo.get().getToken(), userInfo.get().getOrg(), request[1],
                        request[2], Optional.empty(), request[3]);
            } else {
                created = trackerClient.createTask(userInfo.get().getToken(), userInfo.get().getOrg(), request[1],
                        request[2], Optional.of(request[4]), request[3]);
            }
            if (created.isPresent()) {
                sendMessage(chatId, "successfully created problem " + created.get());
            } else {
                sendMessage(chatId, "Something went wrong");
            }
        }
    }

    public void processGetMyTasks(long chatId, String[] request) {
        var userInfo = db.get(chatId);
        if (!userInfo.isPresent()) {
            authError(chatId);
            return;
        }
        try {
            var tasks = trackerClient.getTasksByUser(userInfo.get().getToken(), userInfo.get().getOrg(),
                    userInfo.get().getLogin());
            int numTasks = 5;
            if (request.length > 1) {
                numTasks = Integer.parseInt(request[1]);
            }
            for (int i = 0; i < numTasks && i < tasks.size(); i++) {
                sendMessage(chatId, tasks.get(i));
            }
        } catch (TrackerException exc) {
            sendMessage(chatId, "something went wrong");
            System.err.println(exc.getMessage());
            sendMessage(chatId, exc.getMessage());
        }
    }

    void authError(long chatId) {
        sendMessage(chatId, "You should authorize first");
    }

    public void processGetQueues(long chatId) {
        try {
            var userInfo = db.get(chatId);
            if (!userInfo.isPresent()) {
                authError(chatId);
                return;
            }
            var queues = trackerClient.getAllQueues(userInfo.get().getToken(), userInfo.get().getOrg());
            sendMessage(chatId, "queues:");
            for (var queue : queues) {
                sendMessage(chatId, "name: " + queue.getKey() + " id: " + queue.getId());
            }
        } catch (IOException | InterruptedException exc) {
            sendMessage(chatId, "Something went wrong, try again");
        } catch (AuthorizationException exc) {
            sendMessage(chatId, "Authorization problem occurred, check your token");
        }
    }

    public void displayHelpMessage(long chatId) {
        sendMessage(chatId, "This bot has the following methods:\n"
                + "/start token X-Org-Id login\n"
                + "/createTask name description queueId [assignee]\n"
                + "/getTask TaskID\n"
                + "/getQueues\n"
                + "/getMyTasks [number]\nAll parameters should be separated by newlines\n"
                + "to get a new token, visit https://oauth.yandex.ru/authorize?response_type=token&client_id=bc837b3407684d88833dfda5a5dfc243");
    }

    public void processUpdate(long chatId, String[] request) {
        switch (request[0]) {
            case "/start":
                processAuthorization(chatId, request);
                break;
            case "/getTask":
                processGetTask(chatId, request);
                break;
            case "/createTask":
                processTaskCreation(chatId, request);
                break;
            case "/getQueues":
                processGetQueues(chatId);
                break;
            case "/getMyTasks":
                processGetMyTasks(chatId, request);
                break;
            default:
                displayHelpMessage(chatId);
                break;
        }
    }

    public void run() {
        db.start();
        bot.setUpdatesListener(updates -> {
            for (var update : updates) {
                long chatId = update.message().chat().id();
                var body = update.message().text();
                var request = body.split("\n");
                processUpdate(chatId, request);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public TrackerBot(TelegramBot givenBot, UsersDatabase givenDatabase, TrackerClient givenClient) {
        bot = givenBot;
        db = givenDatabase;
        trackerClient = givenClient;
    }

    private final TelegramBot bot;
    private final UsersDatabase db;
    private final TrackerClient trackerClient;
}
