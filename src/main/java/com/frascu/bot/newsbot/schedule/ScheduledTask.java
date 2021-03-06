package com.frascu.bot.newsbot.schedule;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.frascu.bot.newsbot.dao.DaoBase;
import com.frascu.bot.newsbot.dao.NewsDao;
import com.frascu.bot.newsbot.dto.NewsDto;
import com.frascu.bot.newsbot.rss.FeedMessage;
import com.frascu.bot.newsbot.rss.RSSFeedParser;
import com.frascu.bot.newsbot.telegram.BotConfig;
import com.frascu.bot.newsbot.telegram.handler.CommandsHandler;
import com.frascu.bot.newsbot.telegram.handler.NewsHandler;

/**
 * This class contains the task that checks the news and sends them to the
 * users.
 * 
 * @author franc
 *
 */
public class ScheduledTask extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(ScheduledTask.class);

	private static final int INTERVAL_SECONDS = 60;

	private NewsDao newsDao = new NewsDao();

	// Add your task here
	public void run() {
		try {
			List<NewsDto> newsDtos = readAndSaveFeed();
			new NewsHandler().sendNewsToAllUsers(newsDtos);
		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

	private List<NewsDto> readAndSaveFeed() {
		// Read RSS
		RSSFeedParser parser = new RSSFeedParser(BotConfig.getSources());

		LOGGER.info("Reading the rss...");
		List<FeedMessage> feed = parser.readFeed();

		return newsDao.saveNewsFromFeeds(feed);
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
		readAndSaveFeed();
	}

	public static void main(String[] args) {
		DaoBase.init();

		ScheduledTask st = new ScheduledTask();
		st.init();

		Timer time = new Timer();
		time.schedule(st, 0, INTERVAL_SECONDS * 1000);
	}
}
