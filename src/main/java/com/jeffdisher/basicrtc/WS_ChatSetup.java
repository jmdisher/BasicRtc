package com.jeffdisher.basicrtc;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;

import com.jeffdisher.breakwater.IWebSocketFactory;


/**
 * The chat setup socket is used for pairs of users connecting to the server so they can pass WebRTC setup messages
 * between them.  This connection is very short-lived as they close the connection once they are up and running.
 * Note that it can handle an arbitrary number of such pairs, created concurrently.
 */
public class WS_ChatSetup implements IWebSocketFactory, OnePeer.IPeerRegistry
{
	private final Map<String, OnePeer> _activePeers = new HashMap<>();

	@Override
	public WebSocketListener create(JettyServerUpgradeRequest arg0, Object[] arg1)
	{
		String roomName = (String)arg1[1];
		return new OnePeer(roomName, this);
	}

	@Override
	public synchronized void disconnectPeer(String roomName)
	{
		// Just remove this from the map, if it is there.
		_activePeers.remove(roomName);
	}

	@Override
	public synchronized OnePeer connectPeer(String roomName, OnePeer peer)
	{
		// See if there is already a peer registered.  If so, remove it from the map, connect it to this new peer, and return it.
		OnePeer firstPeer = _activePeers.remove(roomName);
		if (null != firstPeer)
		{
			firstPeer.attachOtherPeer(peer);
		}
		else
		{
			// We are the first peer, so just install ourselves.
			_activePeers.put(roomName, peer);
		}
		return firstPeer;
	}
}
