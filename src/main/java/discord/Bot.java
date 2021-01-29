package discord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.security.auth.login.LoginException;

import managers.ServerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Bot {
	
	private JDA bot;
	private ServerManager SM;
	private boolean isActive;
	
	public Bot(ServerManager SM) {
		startBot();
		this.SM = SM;
	}
	
	public void changeActiveCount(int count) {
		if(bot.getStatus().isInit()) {
			if(count == 1) {
				bot.getPresence().setActivity(Activity.watching(count + " player online"));
			}else {
				bot.getPresence().setActivity(Activity.watching(count + " players online"));
			}
		}
	}
	
	public void sendMessage(String message) {
		bot.getGuilds().forEach(guild -> {
			guild.getTextChannels().forEach(channel -> {
				if(channel.getName().contains("minecraft")) {
					channel.sendMessage(message).complete();
				}
			});
		});
	}
	
	public void closeBot() {
		if(bot.getStatus().isInit()) {
			bot.shutdownNow();
			isActive = false;
		}
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public void startBot() {
		if(bot == null || !bot.getStatus().isInit()) {
			String token = "";
			
			try {
				Scanner botProp = new Scanner(new File("botProp.txt"));
				token = botProp.nextLine();
				botProp.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			JDABuilder bot = JDABuilder.createDefault(token);
			bot.setActivity(Activity.watching("0 players online"));
			bot.addEventListeners(new Listener());
			
			try {
				this.bot = bot.build().awaitReady();
			} catch (LoginException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Listener extends ListenerAdapter {
		
		@Override
		public void onReady(ReadyEvent event) {
			isActive = true;
		}
		
		@Override
		public void onMessageReceived(MessageReceivedEvent event) {
			if(event.getTextChannel().getName().contains("minecraft") && !event.getAuthor().isBot()) {
				SM.processMessageFromDiscord("<" + event.getMember().getEffectiveName() + "> " + event.getMessage().getContentDisplay());
			}
		}
	}
}
