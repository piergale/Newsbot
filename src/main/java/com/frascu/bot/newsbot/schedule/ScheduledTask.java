package com.frascu.bot.newsbot.schedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.frascu.bot.newsbot.dao.NewsDao;
import com.frascu.bot.newsbot.dto.NewsDto;
import com.frascu.bot.newsbot.rss.FeedMessage;
import com.frascu.bot.newsbot.rss.RSSFeedParser;
import com.frascu.bot.newsbot.telegram.BotConfig;
import com.frascu.bot.newsbot.telegram.handler.CommandsHandler;
import com.frascu.bot.newsbot.telegram.handler.NewsHandler;

// Create a class extends with TimerTask
public class ScheduledTask extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(ScheduledTask.class);

	private static final int INTERVAL_SECONDS = 60;
	private static final int MIN_OF_N_WORDS_IN_TITLE = 5;
	private static final int LENGHT_OF_WORD_TO_CHECK = 5;

	private static boolean isFirstRun = true;

	// Add your task here
	public void run() {
		try {
			// Read RSS
			RSSFeedParser parser = new RSSFeedParser(BotConfig.getSources());

			LOGGER.info("Reading the rss...");
			List<FeedMessage> feed = parser.readFeed();

			NewsDao newsDao = new NewsDao();
			List<NewsDto> newsDtos = newsDao.saveNewsFromFeeds(feed);

			List<NewsDto> newsToSend = new ArrayList<>();
			List<NewsDto> newsSimilar = new ArrayList<>();

			for (NewsDto newsDto : newsDtos) {
				String title = newsDto.getTitle().replaceAll(",", "");
				String[] words = title.split(" ");
				long numberOfWordsInOtherNewsToday = Arrays.asList(words).stream().filter(word -> {
					String cleanWord = word.trim();
					return cleanWord.length() > LENGHT_OF_WORD_TO_CHECK
							&& newsDao.areOtherNewsWithWordToday(cleanWord, newsDto.getId());
				}).count();
				if (numberOfWordsInOtherNewsToday > MIN_OF_N_WORDS_IN_TITLE) {
					newsSimilar.add(newsDto);
				} else {
					newsToSend.add(newsDto);
				}
			}

			if (!isFirstRun) {
				NewsHandler newsHandler = new NewsHandler();
				// Send the news
				newsHandler.sendNewsToAllUsers(newsToSend);
				newsDao.setNewsAsSent(newsToSend);

				// Send the similar news to the admin
				newsHandler.sendNewsToAdmin(newsSimilar);

			} else {
				ScheduledTask.isFirstRun = false;
			}

		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

	public void init() {
		// The job that answer to the user
		try {
			ApiContextInitializer.init();
			TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
			telegramBotsApi.registerBot(new CommandsHandler());
		} catch (TelegramApiException e) {
			LOGGER.error("Impossible to create the bot.", e);
		}
	}

	public static void main(String[] args) {
		ScheduledTask st = new ScheduledTask();
		st.init();

		Timer time = new Timer();
		time.schedule(st, 0, INTERVAL_SECONDS * 1000);
	}
}