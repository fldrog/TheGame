package com.fb.bot;

import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.fb.bot.game.BotGameLogic;
import com.fb.bot.rooms.RoomsMonitor;
import com.fb.messages.ClientBaseMessage;
import com.fb.messages.client.gameactions.Answer;
import com.fb.messages.client.gameactions.ChallengePlayer;
import com.fb.messages.client.gameactions.ChooseTerritory;
import com.fb.messages.client.room.JoinGame;
import com.fb.messages.server.general.Snapshot.Room;
import com.fb.transport.MessagesTransport;

public class Bot {
    private String clientId;
    private MessagesTransport msgTransport;

    private RoomsMonitor roomsMonitor;
    // control vars
    private boolean joinSent;
    private boolean playing;
    private BotGameLogic crtGameLogic;
    private Room crtRoom;
    private boolean challenged;

    public Bot(String clientId, MessagesTransport msgTransport) {
	this.clientId = clientId;
	this.msgTransport = msgTransport;
	this.roomsMonitor = new RoomsMonitor(this);
    }

    public void sendMessage(ClientBaseMessage msg) {
	msgTransport.sendClientMessage(msg);
    }

    public void addRoom(Room room) {
	roomsMonitor.addRoom(room);
    }

    public void joinRoom(Room room) {
	this.joinSent = true;
	this.crtRoom = room;
	try {
	    // TODO see how we can avoid this S+ thing
	    msgTransport.subscribeToTopic("S" + room.getRoomId());
	} catch (MqttSecurityException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (MqttException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	sendMessage(new JoinGame(getClientId(), room.getRoomId()));
    }

    public boolean isPlaying() {
	return playing;
    }

    public boolean isJoinSent() {
	return joinSent;
    }

    public String getClientId() {
	return clientId;
    }

    public void gameStarted(String gameId, List<String> map) {
	if (isPlaying()) {
	    crtGameLogic = new BotGameLogic(this, crtRoom, map);
	}
    }

    public void userJoinedRoom(String joinedGameId, String joinedUserId) {
	if (isJoinSent()) {
	    playing = true;
	}
	roomsMonitor.userJoinedRoom(joinedGameId, joinedUserId);
    }

    public void userUnjoinedRoom(String unjoinedGameId, String unjoinedUserId) {
	roomsMonitor.userUnjoinedGame(unjoinedGameId, unjoinedUserId);
    }

    public void endGame(String roomId) {
	if (roomId.equals(crtRoom.getRoomId())) {
	    crtGameLogic.endGame();
	    joinSent = false;
	    playing = false;
	    crtRoom = null;
	    crtGameLogic = null;
	    try {
		msgTransport.unsubscribeFromTopic(roomId);
	    } catch (MqttException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	roomsMonitor.endGame(roomId);
    }

    public void addRooms(List<Room> rooms) {
	for (Room room : rooms) {
	    roomsMonitor.addRoom(room);
	}
    }

    public void chooseTerritory() {
	String territoryId = crtGameLogic.nextTerritoryForChoose();
	msgTransport.sendClientMessage(new ChooseTerritory(getClientId(), crtRoom.getRoomId(), territoryId));
    }

    public void challengeUser() {
	String territoryId = crtGameLogic.nextTerritoryChallenge();
	String challengedPlayerId = crtGameLogic.getOwner(territoryId);
	msgTransport.sendClientMessage(new ChallengePlayer(getClientId(), crtRoom.getRoomId(), challengedPlayerId,
		territoryId));
	challenged = true;
    }

    public void assignTerritory(String territoryId, String clientId) {
	crtGameLogic.assignTerritory(territoryId, clientId);
    }

    public void ownTerritoryChallenged(String territoryId, String challengedPlayerId) {
	this.challenged = true;

    }

    public boolean isChallenged() {
	return this.challenged;
    }

    public void answerQuestion(String question) {
	if (isChallenged()) {
	    sendMessage(new Answer(getClientId(), crtRoom.getRoomId(), "mock answer"));
	}
	challenged = false;
    }
}
