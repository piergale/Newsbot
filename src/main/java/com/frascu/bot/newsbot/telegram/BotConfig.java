package com.frascu.bot.newsbot.telegram;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

public class BotConfig {

	private static final Logger LOGGER = Logger.getLogger(BotConfig.class);

	private static Properties prop;

	static { // load a properties file
		prop = new Properties();
		File file = new File("config.properties");
		try {
			file = new File("config.properties");
			prop.load(new FileInputStream(file));
			LOGGER.info("Properties read from configuration file: " + file.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error("Error!!! Impossible to read properties from the configuration file: " + file.getAbsolutePath(), e);
			System.exit(0);
		}
	}

	public static final String TOKEN = prop.getProperty("token");
	public static final String ADMIN = prop.getProperty("admin");
	public static final String NAME = prop.getProperty("name");

	private BotConfig() {
		super();
	}
}