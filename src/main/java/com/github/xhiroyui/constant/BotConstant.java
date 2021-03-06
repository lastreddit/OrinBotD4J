package com.github.xhiroyui.constant;

import java.time.format.DateTimeFormatter;

public class BotConstant {
	public static final String PREFIX = "!";
	
	public static final String FUNC_FLAG_DURATION = "DURATION";
	public static final String FUNC_FLAG_LIFO = "LIFO";
	public static final String FUNC_FLAG_FIFO = "FIFO";
	public static final String FUNC_FLAG_LOGCHANNEL = "LOGCHANNEL";
	public static final String FUNC_FLAG_ALLOW_DELETE = "ALLOW_DELETE";

	public static final String DB_CHANNEL_MONITOR_TABLE = "channel_monitor";
	public static final String DB_CHANNEL_FLAGS_TABLE = "channel_flags";
	public static final String DB_GUILD_MUTE_TABLE = "guild_mute";
	public static final String DB_MUTED_USERS_TABLE = "muted_users";
	public static final String DB_GUILD_LOG_CHANNEL_TABLE = "guild_log_channels";
	// Error messages
	
	// Utils
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
