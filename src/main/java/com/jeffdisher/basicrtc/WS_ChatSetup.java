package com.jeffdisher.basicrtc;

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
	public static final String HEADER_XFF = "X-Forwarded-For";

	private final boolean _isVerbose;
	private final PairConnector<OnePeer> _activePeers = new PairConnector<>((OnePeer first, OnePeer second) -> first.attachOtherPeer(second));

	public WS_ChatSetup(boolean isVerbose)
	{
		_isVerbose = isVerbose;
	}

	@Override
	public WebSocketListener create(JettyServerUpgradeRequest arg0, Object[] arg1)
	{
		// This is typically used behind a proxy (for SSL termination), so check if there is a proxy header.
		String peerDescription = arg0.getHeader(HEADER_XFF);
		if (null == peerDescription)
		{
			// If not, just fall-back to the remote address.
			peerDescription = arg0.getRemoteSocketAddress().toString();
		}
		String roomName = (String)arg1[1];
		return new OnePeer(_isVerbose, peerDescription, roomName, this);
	}

	@Override
	public void disconnectPeer(String roomName, OnePeer peer)
	{
		boolean didRemove = _activePeers.removePartialIfMatched(roomName, peer);
		if (_isVerbose && didRemove)
		{
			System.out.println("Deregistered room " + roomName);
		}
	}

	@Override
	public OnePeer connectPeer(String roomName, OnePeer peer)
	{
		OnePeer matchedAndRemoved = _activePeers.attachOrRegisterPartial(roomName, peer);
		if (_isVerbose)
		{
			if (null != matchedAndRemoved)
			{
				System.out.println("Paired in room " + roomName);
			}
			else
			{
				System.out.println("Registered room " + roomName);
			}
		}
		return matchedAndRemoved;
	}
}
