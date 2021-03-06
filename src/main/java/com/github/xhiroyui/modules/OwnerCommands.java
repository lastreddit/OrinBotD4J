package com.github.xhiroyui.modules;

import java.util.ArrayList;

import com.github.xhiroyui.DiscordClient;
import com.github.xhiroyui.util.Command;

import sx.blah.discord.api.IDiscordClient;

public class OwnerCommands implements IModuleExtended{

    private String moduleName = "OwnerCommands";
    private String moduleVersion = "1.0";
    private String moduleMinimumVersion = "2.3.0";
    private String author = "Xhiro Yui / Rhestia";
    OwnerCommandsHandler ownerCommandsHandler = new OwnerCommandsHandler();
    
    public void disable() {
    	DiscordClient.getClient().getDispatcher().unregisterListener(ownerCommandsHandler);
    }

	@Override
	public boolean enable(IDiscordClient client) {
		client.getDispatcher().registerListener(ownerCommandsHandler);
		return true;
	}
	
    public String getAuthor() {
        return author;
    }

    public String getMinimumDiscord4JVersion() {
        return moduleMinimumVersion;
    }

    public String getName() {
        return moduleName;
    }

    public String getVersion() {
        return moduleVersion;
    }
    
    public ArrayList<Command> getModuleCommands() {
    	return ownerCommandsHandler.getModuleCommands();
    }
}