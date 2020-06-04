package ru.hse.cs.java2020.task03;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class MainTest {
    class MockBot extends TelegramBot {
        MockBot() {
            super("");
        }

        @Override
        public <T extends BaseRequest, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
            SendMessage message = (SendMessage) request;
            messages.add(message.getParameters().get("text").toString());
            return null;
        }

        public ArrayList<String> messages = new ArrayList<>();
    }

    class MockDb extends UsersDatabase {
        MockDb() {
            super("");
        }

        @Override
        Optional<UserInfo> get(long chatId) {
            if (map.containsKey(chatId)) {
                return Optional.of(map.get(chatId));
            } else {
                return Optional.empty();
            }
        }

        @Override
        void insert(long chatId, UserInfo info) {
            map.put(chatId, info);
        }

        public Map<Long, UserInfo> map = new HashMap<>();
    }

    @Test
    public void testStart() {
        var mockDatabase = new MockDb();
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        bot.processUpdate(0, new String[]{"/start", "token", "org", "login"});
        assertEquals("Your data has been saved", mockBot.messages.get(0));
        assertEquals(mockDatabase.get(0).get(), new UserInfo("token", "org", "login"));
    }

    @Test
    public void testHelp() {
        var mockDatabase = new MockDb();
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        bot.processUpdate(0, new String[]{"/help"});
        assertEquals("This bot has the following methods:\n"
                + "/start token X-Org-Id login\n"
                + "/createTask name description queueId [assignee]\n"
                + "/getTask TaskID\n"
                + "/getQueues\n"
                + "/getMyTasks [number]\nAll parameters should be separated by newlines\n"
                + "to get a new token, visit "
                + "https://oauth.yandex.ru/authorize?response_type=token&client_id=bc837b3407684d88833dfda5a5dfc243",
                mockBot.messages.get(0));
    }

    @Test
    public void testProcessGetTask() {
        var mockDatabase = new MockDb();
        var mockChat = 179;
        UserInfo user = new UserInfo("token", "org", "login");
        mockDatabase.insert(mockChat, user);
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        var task = new TaskInfo();
        task.setAuthor("author");
        task.setDescription("description");
        task.setName("TASK-1");
        task.setAssignedTo("assignee");
        task.addComment(new CommentInfo("author", "text"));
        task.addFollower("follower");
        try {
            Mockito.when(client.getTask(user.getToken(), user.getOrg(), "TASK-1")).thenReturn(task);
        } catch (IOException | InterruptedException | AuthorizationException | TrackerException exc) {
            fail();
        }
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        bot.processUpdate(mockChat, new String[]{"/getTask", "TASK-1"});
        assertEquals("task name: TASK-1", mockBot.messages.get(0));
        assertEquals("description: description", mockBot.messages.get(1));
        assertEquals("author: author", mockBot.messages.get(2));
        assertEquals("assigned to: assignee", mockBot.messages.get(3));
        assertEquals("followers:", mockBot.messages.get(4));
        assertEquals("follower", mockBot.messages.get(5));
        assertEquals("comments:", mockBot.messages.get(6));
        assertEquals("author wrote text", mockBot.messages.get(7));
    }

    @Test
    public void testProcessGetMyTasks() {
        var mockDatabase = new MockDb();
        var mockChat = 179;
        UserInfo user = new UserInfo("token", "org", "login");
        mockDatabase.insert(mockChat, user);
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        var tasks = new ArrayList<String>();
        tasks.add("TASK-1");
        tasks.add("TASK-2");
        tasks.add("TASK-3");
        try {
            Mockito.when(client.getTasksByUser(user.getToken(), user.getOrg(), user.getLogin())).thenReturn(tasks);
        } catch (TrackerException exc) {
            fail();
        }
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        bot.processUpdate(mockChat, new String[]{"/getMyTasks", "2"});
        assertEquals("TASK-1", mockBot.messages.get(0));
        assertEquals("TASK-2", mockBot.messages.get(1));
        assertEquals(2, mockBot.messages.size());
    }

    @Test
    public void testGetQueues() {
        var mockDatabase = new MockDb();
        var mockChat = 179;
        UserInfo user = new UserInfo("token", "org", "login");
        mockDatabase.insert(mockChat, user);
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        var queues = new ArrayList<TrackerQueue>();
        queues.add(new TrackerQueue("Q-1", 1));
        queues.add(new TrackerQueue("Q-2", 2));
        try {
            Mockito.when(client.getAllQueues(user.getToken(), user.getOrg())).thenReturn(queues);
        } catch (IOException | AuthorizationException | InterruptedException exc) {
            fail();
        }
        bot.processUpdate(mockChat, new String[]{"/getQueues"});
        assertEquals("queues:", mockBot.messages.get(0));
        assertEquals("name: Q-1 id: 1", mockBot.messages.get(1));
        assertEquals("name: Q-2 id: 2", mockBot.messages.get(2));
        assertEquals(3, mockBot.messages.size());
    }

    @Test
    public void testCreateTask() {
        var mockDatabase = new MockDb();
        var mockChat = 179;
        UserInfo user = new UserInfo("token", "org", "login");
        mockDatabase.insert(mockChat, user);
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        Mockito.when(client.createTask(user.getToken(), user.getOrg(), "name",
                "desc", Optional.of("user"), "1")).thenReturn(Optional.of("TASK-1"));
        bot.processUpdate(mockChat, new String[]{"/createTask", "name", "desc", "1", "user", "assignee"});
        assertEquals("successfully created problem TASK-1", mockBot.messages.get(0));
    }

    @Test
    public void testMissingUser() {
        var mockDatabase = new MockDb();
        var mockChat = 179;
        UserInfo user = new UserInfo("token", "org", "login");
        mockDatabase.insert(mockChat, user);
        var mockBot = new MockBot();
        var client = Mockito.mock(TrackerClient.class);
        TrackerBot bot = new TrackerBot(mockBot, mockDatabase, client);
        bot.processUpdate(mockChat + 1, new String[]{"/getQueues"});
        assertEquals("You should authorize first", mockBot.messages.get(0));
    }
}

