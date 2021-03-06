package com.github.xhiroyui.modules;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.github.xhiroyui.DiscordClient;
import com.github.xhiroyui.TaskLoader;
import com.github.xhiroyui.UserWhitelist;
import com.github.xhiroyui.constant.BotConstant;
import com.github.xhiroyui.constant.FunctionConstant;
import com.github.xhiroyui.util.BotCache;
import com.github.xhiroyui.util.Command;
import com.github.xhiroyui.util.DBConnection;
import com.github.xhiroyui.util.MiscUtils;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;

public class ModerationCommandsHandler extends ModuleHandler {
	public ModerationCommandsHandler() {
		createCommands();
	}

	private void createCommands() {
		Command command;

		command = new Command(FunctionConstant.MOD_SETUP_MUTE_ROLE);
		command.setCommandName("Setup Mute Role");
		command.setCommandDescription("Assigns the muted role for this guild.");
		command.setCommandCallers("setupmute");
		command.setParams(new String[] { "@Role" });
		command.setMaximumArgs(1);
		command.setExample("setupmute @Muted");
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_SETUP_LOG_CHANNEL);
		command.setCommandName("Setup Log Channel");
		command.setCommandDescription("Assigns the log channel for this guild.");
		command.setCommandCallers("setuplog");
		command.setCommandCallers("setuplogchannel");
		command.setParams(new String[] { "#channel" });
		command.setMaximumArgs(1);
		command.setExample("setuplog #channel");
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_MUTE_USER);
		command.setCommandName("Mute User");
		command.setCommandDescription(
				"Gives the user the Muted role. Muted role is obtained from " + FunctionConstant.MOD_SETUP_MUTE_ROLE);
		command.setCommandCallers("mute");
		command.setParams(new String[] { "@User" });
		command.setParams(new String[] { "Duration in minutes" });
		command.setMaximumArgs(2);
		command.setExample("mute @User 30");
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_FLAG_CHANNEL);
		command.setCommandName("Flag Channel");
		command.setCommandDescription("Flags a channel. Type of flags can be found by using the List Flags function.");
		command.setCommandCallers("flag");
		command.setParams(new String[] { "Flag (not case-sensitive)" });
		command.setParams(new String[] { "Additional arguments based on flag. Check with List Flags" });
		command.setMaximumArgs(2);
		command.setExample("flag " + BotConstant.FUNC_FLAG_FIFO + " 1");
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_UNFLAG_CHANNEL);
		command.setCommandName("Unflag Channel");
		command.setCommandDescription(
				"Removes all flags from a channel. Also clears all data generated by flags in this channel.");
		command.setCommandCallers("unflag");
		command.setMaximumArgs(0);
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_CHECK_CHANNEL_FLAGS);
		command.setCommandName("Check Flags");
		command.setCommandDescription("Displays all flags in the current channel.");
		command.setCommandCallers("checkflag");
		command.setCommandCallers("checkflags");
		command.setMaximumArgs(0);
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_LIST_FLAGS);
		command.setCommandName("List Flags");
		command.setCommandDescription("Lists all available flags offered by this bot and its functionalities");
		command.setCommandCallers("listflag");
		command.setCommandCallers("listflags");
		command.setMaximumArgs(0);
		commandList.add(command);

		command = new Command(FunctionConstant.MOD_MESSAGE_PURGE);
		command.setCommandName("Purge Message");
		command.setCommandDescription(
				"Purge Messages less than 14 days old based on parameters. \nMessages older then 14 days are not affected by this command.");
		command.setCommandCallers("purge");
		command.setParams(new String[] { "Denomination (Days / Hours / Minutes)" });
		command.setParams(new String[] { "Amount by denomination" });
		command.setMaximumArgs(2);
		command.setExample("HOURS 2");
		commandList.add(command);

	}

	@EventSubscriber
	public void OnMesageEvent(MessageReceivedEvent event)
			throws RateLimitException, DiscordException, MissingPermissionsException, IOException {
		String[] command = processCommand(event);
		if (command != null) {
			if (!moderatorCheck(event.getAuthor(), event.getGuild())) {
				if (UserWhitelist.getWhitelist().validateUser(event.getAuthor().getStringID())) {
					executeCommand(event, command);
				} else {
					sendMessage(event.getAuthor().getDisplayName(event.getGuild())
							+ " is not a moderator or whitelisted and is unable to use this command!", event);
				}
			} else {
				executeCommand(event, command);
			}
		}
	}

	public void executeCommand(MessageReceivedEvent event, String[] command) {
		String commandCode = validateCommand(event, command);
		if (commandCode != null) {
			switch (commandCode) {
			case FunctionConstant.MOD_SETUP_MUTE_ROLE:
				try {
					setupMuteRole(event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_FLAG_CHANNEL, e, event);
				}
				break;
			case FunctionConstant.MOD_SETUP_LOG_CHANNEL:
				try {
					setupLogChannelForGuild(event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_SETUP_LOG_CHANNEL, e, event);
				}
				break;

			case FunctionConstant.MOD_MUTE_USER:
				try {
					muteUser(command, event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_MUTE_USER, e, event);
				}
				break;
			case FunctionConstant.MOD_FLAG_CHANNEL:
				try {
					flagChannel(command, event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_FLAG_CHANNEL, e, event);
				}
				break;
			case FunctionConstant.MOD_UNFLAG_CHANNEL:
				try {
					removeAllFlags(event.getChannel().getLongID());
					printFlagsForCurrentChannel(event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_UNFLAG_CHANNEL, e, event);
				}
				break;
			case FunctionConstant.MOD_CHECK_CHANNEL_FLAGS:
				try {
					printFlagsForCurrentChannel(event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_CHECK_CHANNEL_FLAGS, e, event);
				}
				break;
			case FunctionConstant.MOD_LIST_FLAGS:
				try {
					printAvailableFlags(event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_LIST_FLAGS, e, event);
				}
				break;
			case FunctionConstant.MOD_MESSAGE_PURGE:
				try {
					purgeMessages(command, event);
				} catch (Exception e) {
					throwError(FunctionConstant.MOD_MESSAGE_PURGE, e, event);
				}
				break;
			}
		}
	}

	// Command functions are placed below here

	private void setupMuteRole(MessageReceivedEvent event) {
		if (event.getMessage().getRoleMentions().size() < 1)
			sendMessage("Please mention a role for mute role setup.", event);
		else {
			if (Integer.parseInt(DBConnection.getDBConnection().selectQuerySingleResult(
					"SELECT COUNT(role_id) FROM " + BotConstant.DB_GUILD_MUTE_TABLE + " WHERE guild_id = ?",
					event.getGuild().getLongID())) == 0)
				DBConnection.getDBConnection().insertQuery(
						"INSERT INTO " + BotConstant.DB_GUILD_MUTE_TABLE + " (guild_id, role_id) VALUES (?,?)",
						event.getGuild().getLongID(), event.getMessage().getRoleMentions().get(0).getLongID());
			else {
				DBConnection.getDBConnection().insertQuery(
						"UPDATE " + BotConstant.DB_GUILD_MUTE_TABLE + " SET role_id = ? WHERE guild_id = ?",
						event.getMessage().getRoleMentions().get(0).getLongID(), event.getGuild().getLongID());
				BotCache.mutedRoleIDCache.refresh(event.getGuild().getLongID());
			}
		}
	}

	private void setupLogChannelForGuild(MessageReceivedEvent event) {
		if (event.getMessage().getChannelMentions().size() < 1)
			sendMessage("Please mention a channel to designate as the log channel for this guild.", event);
		else {
			if (Integer.parseInt(DBConnection.getDBConnection().selectQuerySingleResult(
					"SELECT COUNT(channel_id) FROM " + BotConstant.DB_GUILD_LOG_CHANNEL_TABLE + " WHERE guild_id = ?",
					event.getGuild().getLongID())) == 0)
				DBConnection.getDBConnection().insertQuery(
						"INSERT INTO " + BotConstant.DB_GUILD_LOG_CHANNEL_TABLE
								+ " (guild_id, channel_id) VALUES (?,?)",
						event.getGuild().getLongID(), event.getMessage().getChannelMentions().get(0).getLongID());
			else {
				DBConnection.getDBConnection().insertQuery(
						"UPDATE " + BotConstant.DB_GUILD_LOG_CHANNEL_TABLE + " SET channel_id = ? WHERE guild_id = ?",
						event.getMessage().getChannelMentions().get(0).getLongID(), event.getGuild().getLongID());
				BotCache.guildLogChannelIDCache.refresh(event.getGuild().getLongID());
			}
		}
	}

	private void muteUser(String[] command, MessageReceivedEvent event) throws ExecutionException {
		if (command.length != 3)
			sendMessage(
					"Missing parameters. Please consult the help function for the required parameters for using this command",
					event);
		else if (event.getMessage().getMentions().size() < 1)
			sendMessage("Please mention a user to mute.", event);
		else if (!MiscUtils.isInteger(command[2]))
			sendMessage("Input a valid integer for the time parameter.", event);
		else {
			if (Integer.parseInt(DBConnection.getDBConnection().selectQuerySingleResult(
					"SELECT COUNT(role_id) FROM " + BotConstant.DB_GUILD_MUTE_TABLE + " WHERE guild_id = ?",
					event.getGuild().getLongID())) == 0)
				sendMessage("Mute role not setup for current guild. Please setup the mute role using the "
						+ FunctionConstant.MOD_SETUP_MUTE_ROLE + " command.", event);
			else {
				try {
					event.getMessage().getMentions().get(0).addRole(
							event.getGuild().getRoleByID(BotCache.mutedRoleIDCache.get(event.getGuild().getLongID())));
					DBConnection.getDBConnection().insertQuery(
							"INSERT INTO " + BotConstant.DB_MUTED_USERS_TABLE
									+ " (user_id, guild_id, muted_until) VALUES (?,?,?) ON DUPLICATE KEY UPDATE muted_until = ?",
							event.getMessage().getMentions().get(0).getLongID(), event.getGuild().getLongID(),
							Instant.now().plus(Long.parseLong(command[2]), ChronoUnit.HOURS).toEpochMilli(),
							Instant.now().plus(Long.parseLong(command[2]), ChronoUnit.HOURS).toEpochMilli());
					RequestBuffer.request(() -> DiscordClient.getClient()
							.getOrCreatePMChannel(event.getMessage().getMentions().get(0))
							.sendMessage("You have been muted in " + event.getGuild().getName() + " for " + command[2]
									+ " hour(s)."));
					Long logChannelID = BotCache.guildLogChannelIDCache.get(event.getGuild().getLongID());
					if (logChannelID.compareTo(0L) != 0)
						RequestBuffer
								.request(
										() -> sendLogMessage(
												"User " + event.getMessage().getMentions().get(0).mention()
														+ " has been muted for " + command[2] + " hour(s).",
												logChannelID));
					TaskLoader.getTaskLoader().refreshUnmuter();
				} catch (NullPointerException e) {
					sendMessage("Role not found. Has the role been deleted? Please setup a new role using the **"
							+ FunctionConstant.MOD_SETUP_MUTE_ROLE + "** command.", event);
				}
			}
		}
	}

	private void printAvailableFlags(MessageReceivedEvent event) {
		StringBuilder listFlagsSB = new StringBuilder();
		listFlagsSB.append("__**List of all available Flags**__\n\n");

		listFlagsSB.append("**" + BotConstant.FUNC_FLAG_FIFO + "** : ");
		listFlagsSB.append("Limits the amount of posts per user in the channel. \n");
		listFlagsSB.append("Accepts a single variable {postAmount} in integer. \n");
		listFlagsSB.append("Keeps the newest message in memory. \n");
		listFlagsSB.append("Example : `" + BotConstant.PREFIX + "flag " + BotConstant.FUNC_FLAG_FIFO + " 1` \n");
		listFlagsSB
				.append("*NOTE : Decreasing the {postAmount} on an already flagged channel will delete all stored records\n"
						+ "As such, it is advisable to purge all messages from the channel before doing so.\n" + "*"
						+ BotConstant.FUNC_FLAG_FIFO + " is mutually exclusive with " + BotConstant.FUNC_FLAG_LIFO
						+ "*\n\n");

		listFlagsSB.append("**" + BotConstant.FUNC_FLAG_LIFO + "** : ");
		listFlagsSB.append("Limits the amount of posts per user in the channel. \n");
		listFlagsSB.append("Accepts a single variable {postAmount} in integer. \n");
		listFlagsSB.append("Keeps the oldest message in memory. \n");
		listFlagsSB.append("Example : `" + BotConstant.PREFIX + "flag " + BotConstant.FUNC_FLAG_LIFO + " 1` \n");
		listFlagsSB
				.append("*NOTE : Decreasing the {postAmount} on an already flagged channel will delete all stored records\n"
						+ "As such, it is advisable to purge all messages from the channel before doing so.\n" + "*"
						+ BotConstant.FUNC_FLAG_LIFO + " is mutually exclusive with " + BotConstant.FUNC_FLAG_FIFO
						+ "*\n\n");

		listFlagsSB.append("**" + BotConstant.FUNC_FLAG_DURATION + "** : ");
		listFlagsSB.append("Deletes posts older than the specified amount of time in hours. \n");
		listFlagsSB.append("Accepts a single variable {hours} in integer. \n");
		listFlagsSB.append("Example : `" + BotConstant.PREFIX + "flag " + BotConstant.FUNC_FLAG_DURATION + " 24` \n\n");

		listFlagsSB.append("**" + BotConstant.FUNC_FLAG_LOGCHANNEL + "** : ");
		listFlagsSB.append("Logs bot activity from this channel due to flags in the specified channel. \n");
		listFlagsSB.append("Accepts a single {channelMention}. \n");
		listFlagsSB.append("Example : `" + BotConstant.PREFIX + "flag " + BotConstant.FUNC_FLAG_LOGCHANNEL
				+ " #selectedChannel` \n\n");

		listFlagsSB.append("**" + BotConstant.FUNC_FLAG_ALLOW_DELETE + "** : ");
		listFlagsSB.append("Allows manual deletion of messages in a channel to reflect in the DB. \n");
		listFlagsSB.append("0 = False | 1 = True \n");
		listFlagsSB
				.append("Example : `" + BotConstant.PREFIX + "flag " + BotConstant.FUNC_FLAG_ALLOW_DELETE + " 1` \n\n");

		listFlagsSB.append("All individual flags can be disabled by passing `0` as their arguments or by calling "
				+ "!unflag to remove all flags from the channel");
		sendMessage(listFlagsSB.toString(), event);
	}

	private void flagChannel(String[] command, MessageReceivedEvent event) {
		if (command[1].equalsIgnoreCase("NONE"))
			removeAllFlags(event.getChannel().getLongID());
		else if (command[1].equalsIgnoreCase("KEEP_OLDEST") || command[1].equalsIgnoreCase("KEEPOLDEST")
				|| command[1].equalsIgnoreCase("LIFO"))
			if (command.length == 3)
				setLIFOorFIFOFlag(BotConstant.FUNC_FLAG_LIFO, event, command[2]);
			else
				sendMessage("Missing parameter. The parameters for `" + BotConstant.FUNC_FLAG_LIFO
						+ "` flag is :\n**Post Limit** : {amount}", event);
		else if (command[1].equalsIgnoreCase("KEEP_NEWEST") || command[1].equalsIgnoreCase("KEEPNEWEST")
				|| command[1].equalsIgnoreCase("FIFO"))
			if (command.length == 3)
				setLIFOorFIFOFlag(BotConstant.FUNC_FLAG_FIFO, event, command[2]);
			else
				sendMessage("Missing parameter. The parameters for `" + BotConstant.FUNC_FLAG_FIFO
						+ "` flag is :\n**Post Limit** : {amount}", event);
		else if (command[1].equalsIgnoreCase("DURATION"))
			if (command.length == 3)
				setDurationFlag(BotConstant.FUNC_FLAG_DURATION, event, command[2]);
			else
				sendMessage("Missing parameter. The parameters for `" + BotConstant.FUNC_FLAG_DURATION
						+ "` flag is :\n**Duration** *in hours* : {hours}", event);
		else if (command[1].equalsIgnoreCase("LOGCHANNEL"))
			if (command.length == 3)
				setChannelLogFlag(BotConstant.FUNC_FLAG_LOGCHANNEL, event, command[2]);
			else
				sendMessage("Missing parameter. The parameters for `" + BotConstant.FUNC_FLAG_LOGCHANNEL
						+ "` flag is :\n**#selectedChannel** {channelMention}", event);
		else if (command[1].equalsIgnoreCase("ALLOWDELETE") || command[1].equalsIgnoreCase("ALLOW_DELETE"))
			if (command.length == 3)
				setAllowDeleteFlag(BotConstant.FUNC_FLAG_ALLOW_DELETE, event, command[2]);
			else
				sendMessage("Missing parameter. The parameters for `" + BotConstant.FUNC_FLAG_ALLOW_DELETE
						+ "` flag is : {0} or {1}. 0 = False and 1 = True", event);
		else
			sendMessage("Invalid flag. Please check list of available flags using the List Flags command", event);
	}

	private void removeAllFlags(long channelID) {
		DBConnection.getDBConnection()
				.deleteQuery("DELETE FROM " + BotConstant.DB_CHANNEL_FLAGS_TABLE + " WHERE channel_id = ?", channelID);
		DBConnection.getDBConnection().deleteQuery(
				"DELETE FROM " + BotConstant.DB_CHANNEL_MONITOR_TABLE + " WHERE channel_id = ?", channelID);
		try {
			TaskLoader.getTaskLoader().getMonitor(channelID).shutdown();
		} catch (NullPointerException e) {
			// No monitor found. Doesn't do anything.
		}
	}

	private void setLIFOorFIFOFlag(String flag, MessageReceivedEvent event, String amount) {
		if (MiscUtils.isInteger(amount) && Integer.parseInt(amount) >= 0) {
			ArrayList<String> monitorFlags = TaskLoader.getTaskLoader().getMonitorFlags(event.getChannel().getLongID());
			if ((monitorFlags == null || !(monitorFlags.contains(BotConstant.FUNC_FLAG_FIFO)
					|| monitorFlags.contains(BotConstant.FUNC_FLAG_LIFO))) && Integer.parseInt(amount) > 0) {
				DBConnection.getDBConnection().insertQuery(
						"INSERT INTO " + BotConstant.DB_CHANNEL_FLAGS_TABLE
								+ " (`channel_id`, `flags`, `post_amount`) VALUES (?,?,?)",
						event.getChannel().getLongID(), flag, amount);
			} else if (Integer.parseInt(amount) == 0) {
				DBConnection.getDBConnection().deleteQuery(
						"DELETE FROM " + BotConstant.DB_CHANNEL_FLAGS_TABLE + " WHERE channel_id = ? AND flags = ?",
						event.getChannel().getLongID(), flag);
			} else {
				if (monitorFlags.contains(BotConstant.FUNC_FLAG_FIFO)
						&& flag.equalsIgnoreCase(BotConstant.FUNC_FLAG_FIFO))
					DBConnection.getDBConnection().updateQuery(
							"UPDATE " + BotConstant.DB_CHANNEL_FLAGS_TABLE
									+ " SET post_amount = ? WHERE channel_id = ? AND flags = ?",
							amount, event.getChannel().getLongID(), BotConstant.FUNC_FLAG_FIFO);
				if (monitorFlags.contains(BotConstant.FUNC_FLAG_FIFO)
						&& flag.equalsIgnoreCase(BotConstant.FUNC_FLAG_LIFO)) {
					DBConnection.getDBConnection().updateQuery(
							"UPDATE " + BotConstant.DB_CHANNEL_FLAGS_TABLE
									+ " SET post_amount = ?, flags = ? WHERE channel_id = ? AND flags = ?",
							amount, BotConstant.FUNC_FLAG_LIFO, event.getChannel().getLongID(),
							BotConstant.FUNC_FLAG_FIFO);
					DBConnection.getDBConnection().deleteQuery(
							"DELETE FROM " + BotConstant.DB_CHANNEL_MONITOR_TABLE + " WHERE channel_id = ?",
							event.getChannel().getLongID());
				}
				if (monitorFlags.contains(BotConstant.FUNC_FLAG_LIFO)
						&& flag.equalsIgnoreCase(BotConstant.FUNC_FLAG_LIFO))
					DBConnection.getDBConnection().updateQuery(
							"UPDATE " + BotConstant.DB_CHANNEL_FLAGS_TABLE
									+ " SET post_amount = ? WHERE channel_id = ? AND flags = ?",
							amount, event.getChannel().getLongID(), BotConstant.FUNC_FLAG_LIFO);
				if (monitorFlags.contains(BotConstant.FUNC_FLAG_LIFO)
						&& flag.equalsIgnoreCase(BotConstant.FUNC_FLAG_FIFO)) {
					DBConnection.getDBConnection().updateQuery(
							"UPDATE " + BotConstant.DB_CHANNEL_FLAGS_TABLE
									+ " SET post_amount = ?, flags = ? WHERE channel_id = ? AND flags = ?",
							amount, BotConstant.FUNC_FLAG_FIFO, event.getChannel().getLongID(),
							BotConstant.FUNC_FLAG_LIFO);
					DBConnection.getDBConnection().deleteQuery(
							"DELETE FROM " + BotConstant.DB_CHANNEL_MONITOR_TABLE + " WHERE channel_id = ?",
							event.getChannel().getLongID());
				}
			}
			createUpdateMonitor(event.getChannel().getLongID());
		} else {
			sendMessage("Parameter error. The parameter {postAmount} only accepts integers.", event);
		}
	}

	private void setDurationFlag(String flag, MessageReceivedEvent event, String duration) {
		if (MiscUtils.isInteger(duration) && Integer.parseInt(duration) >= 0) {
			ArrayList<String> monitorFlags = TaskLoader.getTaskLoader().getMonitorFlags(event.getChannel().getLongID());
			if ((monitorFlags == null || !(monitorFlags.contains(BotConstant.FUNC_FLAG_DURATION)))
					&& Integer.parseInt(duration) > 0) {
				DBConnection.getDBConnection().insertQuery(
						"INSERT INTO " + BotConstant.DB_CHANNEL_FLAGS_TABLE
								+ " (`channel_id`, `flags`, `duration`) VALUES (?,?,?)",
						event.getChannel().getLongID(), flag, duration);
			} else if (Integer.parseInt(duration) == 0) {
				DBConnection.getDBConnection().deleteQuery(
						"DELETE FROM " + BotConstant.DB_CHANNEL_FLAGS_TABLE + " WHERE channel_id = ? AND flags = ?",
						event.getChannel().getLongID(), BotConstant.FUNC_FLAG_DURATION);
			} else {
				DBConnection.getDBConnection().updateQuery(
						"UPDATE " + BotConstant.DB_CHANNEL_FLAGS_TABLE
								+ " SET duration = ? WHERE channel_id = ? AND flags = ?",
						duration, event.getChannel().getLongID(), BotConstant.FUNC_FLAG_DURATION);
			}
			createUpdateMonitor(event.getChannel().getLongID());
		} else {
			sendMessage("Parameter error. The parameter {duration} only accepts integers.", event);
		}
	}

	private void setChannelLogFlag(String flag, MessageReceivedEvent event, String command) {
		if (!event.getMessage().getChannelMentions().isEmpty()) {
			ArrayList<String> monitorFlags = TaskLoader.getTaskLoader().getMonitorFlags(event.getChannel().getLongID());
			if (monitorFlags == null || !(monitorFlags.contains(BotConstant.FUNC_FLAG_LOGCHANNEL))) {
				DBConnection.getDBConnection().insertQuery(
						"INSERT INTO " + BotConstant.DB_CHANNEL_FLAGS_TABLE
								+ " (`channel_id`, `flags`, `log_channel_id`) VALUES (?,?,?)",
						event.getChannel().getLongID(), flag,
						event.getMessage().getChannelMentions().get(0).getLongID());
			} else {
				DBConnection.getDBConnection().updateQuery(
						"UPDATE " + BotConstant.DB_CHANNEL_FLAGS_TABLE
								+ " SET log_channel_id = ? WHERE channel_id = ? AND flags = ?",
						event.getMessage().getChannelMentions().get(0).getLongID(), event.getChannel().getLongID(),
						BotConstant.FUNC_FLAG_LOGCHANNEL);
			}
			createUpdateMonitor(event.getChannel().getLongID());
		} else if (command.equalsIgnoreCase("0")) {
			DBConnection.getDBConnection().deleteQuery(
					"DELETE FROM " + BotConstant.DB_CHANNEL_FLAGS_TABLE + " WHERE channel_id = ? AND flags = ?",
					event.getChannel().getLongID(), BotConstant.FUNC_FLAG_LOGCHANNEL);
			createUpdateMonitor(event.getChannel().getLongID());
		} else {
			sendMessage("Parameter error. Please mention a channel as a parameter for this flag.", event);
		}
	}

	private void setAllowDeleteFlag(String flag, MessageReceivedEvent event, String boolValue) {

		if (MiscUtils.isInteger(boolValue) && (Integer.parseInt(boolValue) == 0 || Integer.parseInt(boolValue) == 1)) {
			ArrayList<String> monitorFlags = TaskLoader.getTaskLoader().getMonitorFlags(event.getChannel().getLongID());
			if (monitorFlags == null) {
				sendMessage("The " + BotConstant.FUNC_FLAG_ALLOW_DELETE
						+ " flag requires at least one other flag on the channel.", event);
			} else if (monitorFlags.contains(BotConstant.FUNC_FLAG_ALLOW_DELETE)) {
				if (boolValue.equalsIgnoreCase("0")) {
					DBConnection.getDBConnection().deleteQuery(
							"DELETE FROM " + BotConstant.DB_CHANNEL_FLAGS_TABLE + " WHERE channel_id = ? AND flags = ?",
							event.getChannel().getLongID(), BotConstant.FUNC_FLAG_ALLOW_DELETE);
					createUpdateMonitor(event.getChannel().getLongID());
				}
			} else { // It only comes here if there's flags but no Allow Delete
						// flag.
				if (boolValue.equalsIgnoreCase("1")) {
					DBConnection.getDBConnection().insertQuery(
							"INSERT INTO " + BotConstant.DB_CHANNEL_FLAGS_TABLE
									+ " (`channel_id`, `flags`, `allow_delete`) VALUES (?,?,?)",
							event.getChannel().getLongID(), flag, boolValue);
					createUpdateMonitor(event.getChannel().getLongID());
				}
			}

		} else {
			sendMessage("Parameter error. The parameter only accepts {0} for False or {1} for True", event);
		}
	}

	private void printFlagsForCurrentChannel(MessageReceivedEvent event) {
		ArrayList<String> monitorFlags = TaskLoader.getTaskLoader().getMonitorSettings(event.getChannel().getLongID());
		if (monitorFlags == null || monitorFlags.size() == 0) {
			sendMessage("Channel has no flags.", event);
		} else {
			StringBuilder flagsSB = new StringBuilder();
			flagsSB.append("The channel has the following flags :\n");
			for (int i = 0; i < monitorFlags.size(); i++) {
				if (i == 0 || i % 2 == 0) {
					flagsSB.append("**" + monitorFlags.get(i) + "** : ");
				} else {
					flagsSB.append(monitorFlags.get(i) + "\n");
				}
			}
			sendMessage(flagsSB.toString(), event);
		}

	}

	private void purgeMessages(String[] command, MessageReceivedEvent event) {
		int totalMessagesDeleted = 0;
		if (MiscUtils.isInteger(command[2])) {
			if (command[1].equalsIgnoreCase("min") || command[1].equalsIgnoreCase("minute")
					|| command[1].equalsIgnoreCase("minutes"))
				totalMessagesDeleted = RequestBuffer
						.request(() -> (Integer) event.getChannel()
								.getMessageHistoryTo(
										Instant.now().minus(Integer.parseInt(command[2]), ChronoUnit.MINUTES))
								.bulkDelete().size())
						.get();
			else if (command[1].equalsIgnoreCase("hour") || command[1].equalsIgnoreCase("hours"))
				totalMessagesDeleted = RequestBuffer
						.request(() -> (Integer) event.getChannel()
								.getMessageHistoryTo(
										Instant.now().minus(Integer.parseInt(command[2]), ChronoUnit.HOURS))
								.bulkDelete().size())
						.get();
			else if (command[1].equalsIgnoreCase("day") || command[1].equalsIgnoreCase("days"))
				totalMessagesDeleted = RequestBuffer
						.request(
								() -> (Integer) event.getChannel()
										.getMessageHistoryTo(
												Instant.now().minus(Integer.parseInt(command[2]), ChronoUnit.DAYS))
										.bulkDelete().size())
						.get();
			else {
				sendMessage("Invalid denomination. Please use either `minutes`, `hours` or `days`.", event);
				return;
			}
		}
		sendMessage("Deleted a total of " + totalMessagesDeleted + " message(s) ~!", event);
	}

	private void createUpdateMonitor(long channelID) {
		if (TaskLoader.getTaskLoader().checkMonitors(channelID))
			TaskLoader.getTaskLoader().refreshMonitorSettings(channelID);
		else
			TaskLoader.getTaskLoader().addMonitor(channelID);
	}
}