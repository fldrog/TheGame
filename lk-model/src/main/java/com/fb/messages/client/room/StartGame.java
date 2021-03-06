package com.fb.messages.client.room;

import com.fb.messages.ClientBaseMessage;
import com.fb.topics.Topic;

public class StartGame extends ClientBaseMessage {
    private String gameId;

    public StartGame(String userId, String gameId) {
	super(userId, Topic.ROOMS_ENGINE);
	this.gameId = gameId;
    }

    public String getGameId() {
	return gameId;
    }
}
