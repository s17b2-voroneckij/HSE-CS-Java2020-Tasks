package ru.hse.cs.java2020.task03;

import com.pengrad.telegrambot.TelegramBot;

public class Main {
    public static void main(String[] args) {
        var db = new UsersDatabase("/home/dima/test.db");
        var trackerClient = TrackerClient.getTrackerClient();
        var bot = new TelegramBot("854909600:AAHB77kIuoSnks7C57UVkpeU8VJIhNEeKmg");
        var trackerBot = new TrackerBot(bot, db, trackerClient);
        trackerBot.run();
    }
}
